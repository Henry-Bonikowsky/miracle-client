mod check_mod_compatibility;
mod dependency_resolver;

pub mod curseforge;
pub mod mod_updates;
pub mod modpack;
pub mod modrinth;

use crate::auth::{AuthManager, DeviceCodeResponse, MinecraftProfile};
use crate::minecraft::{GameVersion, MinecraftManager};
use crate::profiles::{
    sanitize_profile_name, Profile, ProfileExport, ProfileManager, PERFORMANCE_MODS,
};
use crate::supabase::{Friend, FriendRequestResult, ModUpdateInfo, SupabaseClient, User};
use crate::updater::{UpdateInfo, UpdateManager};
use check_mod_compatibility::{check_mods_compatibility, ModCompatibility};
use dependency_resolver::{resolve_and_install_dependencies, resolve_dependencies};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::sync::Mutex;
use tauri::{AppHandle, Emitter, Manager};

/// Maps Minecraft versions to the appropriate Miracle Client mod version
fn get_mod_version_for_minecraft(mc_version: &str) -> &'static str {
    // API changed between versions:
    // - DrawContext.drawText return type changed in 1.21.5
    // - KeyBinding.Category API changed in 1.21.9, Camera.update signature changed in 1.21.11
    match mc_version {
        "1.21" | "1.21.1" | "1.21.2" | "1.21.3" | "1.21.4" => "1.21.4",
        "1.21.5" | "1.21.6" | "1.21.7" => "1.21.5",
        "1.21.8" => "1.21.8",
        "1.21.9" | "1.21.10" => "1.21.11",
        _ => "1.21.11", // 1.21.11+
    }
}

// Global state
pub struct AppState {
    pub auth_manager: AuthManager,
    pub minecraft_manager: MinecraftManager,
    pub update_manager: UpdateManager,
    pub supabase: SupabaseClient,
    pub profile_manager: Mutex<ProfileManager>,
    pub game_process: Mutex<Option<std::process::Child>>,
    pub current_player: Mutex<Option<(String, String)>>, // (uuid, username) of current player
}

impl Default for AppState {
    fn default() -> Self {
        Self {
            auth_manager: AuthManager::new(),
            minecraft_manager: MinecraftManager::new(),
            update_manager: UpdateManager::new("1.0.0"),
            supabase: SupabaseClient::new(),
            profile_manager: Mutex::new(ProfileManager::new()),
            game_process: Mutex::new(None),
            current_player: Mutex::new(None),
        }
    }
}

// ==================== Auth Commands ====================

/// Start the device code flow - returns code info for user to enter
#[tauri::command]
pub async fn auth_start_device_flow(app: AppHandle) -> Result<DeviceCodeResponse, String> {
    let state = app.state::<AppState>();

    state
        .auth_manager
        .start_device_code_flow()
        .await
        .map_err(|e| e.to_string())
}

/// Poll for authentication completion and return profile
#[tauri::command]
pub async fn auth_poll_device_flow(
    app: AppHandle,
    device_code: String,
    interval: u64,
) -> Result<MinecraftProfile, String> {
    let state = app.state::<AppState>();

    // Poll for the token and complete authentication
    state
        .auth_manager
        .poll_and_authenticate(&device_code, interval)
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn auth_logout() -> Result<(), String> {
    // Clear stored credentials
    Ok(())
}

/// Write accounts list for mod (called before game launch)
#[tauri::command]
pub async fn write_accounts_for_game(accounts: Vec<AccountForMod>) -> Result<(), String> {
    write_accounts_for_mod(&accounts).await
}

#[tauri::command]
pub async fn auth_refresh(
    app: AppHandle,
    refresh_token: String,
) -> Result<MinecraftProfile, String> {
    let state = app.state::<AppState>();

    state
        .auth_manager
        .refresh(&refresh_token)
        .await
        .map_err(|e| e.to_string())
}

// ==================== Game Commands ====================

#[tauri::command]
pub async fn get_minecraft_versions(app: AppHandle) -> Result<Vec<GameVersion>, String> {
    let state = app.state::<AppState>();

    state
        .minecraft_manager
        .get_versions()
        .await
        .map_err(|e| e.to_string())
}

#[tauri::command]
#[allow(non_snake_case)]
pub async fn launch_game(
    app: AppHandle,
    version: String,
    accessToken: String,
    username: String,
    uuid: String,
    ram: u32,
    javaPath: Option<String>,
    showGameLogs: Option<bool>,
    profileId: Option<String>,
    theme: Option<String>,
) -> Result<(), String> {
    let state = app.state::<AppState>();

    // Get active profile ID and name (use provided or fetch from manager)
    let (active_profile_id, active_profile_name) = match profileId {
        Some(id) => {
            let manager = state.profile_manager.lock().unwrap();
            let name = manager
                .get_profile(&id)
                .map(|p| p.name.clone())
                .unwrap_or_else(|| id.clone());
            (id, name)
        }
        None => {
            let mut manager = state.profile_manager.lock().unwrap();
            manager
                .get_active_profile(&version)
                .map(|p| (p.id.clone(), p.name.clone()))
                .unwrap_or_else(|_| ("default".to_string(), "Default".to_string()))
        }
    };
    let profile_dir_name = sanitize_profile_name(&active_profile_name);
    tracing::info!(
        "Launching with profile: {} (dir: {})",
        active_profile_name,
        profile_dir_name
    );

    // Migrate old UUID-based folder to name-based folder if needed
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");
    let version_mods_dir = game_dir.join("mods").join(&version);
    let old_uuid_dir = version_mods_dir.join(&active_profile_id);
    let new_name_dir = version_mods_dir.join(&profile_dir_name);

    if old_uuid_dir.exists() && !new_name_dir.exists() && active_profile_id != profile_dir_name {
        tracing::info!(
            "Migrating mods folder: {} -> {}",
            active_profile_id,
            profile_dir_name
        );
        if let Err(e) = std::fs::rename(&old_uuid_dir, &new_name_dir) {
            tracing::warn!("Failed to migrate mods folder, will create new: {}", e);
        }
    }

    // Sync theme from launcher to mod config
    if let Some(theme_name) = theme {
        if let Err(e) = sync_theme_to_mod_config(&theme_name).await {
            tracing::warn!("Failed to sync theme to mod config: {}", e);
        } else {
            tracing::info!("Synced theme '{}' to mod config", theme_name);
        }
    }

    // Emit progress events to frontend
    let app_clone = app.clone();
    let progress_callback = move |current: u64, total: u64, file: &str| {
        let _ = app_clone.emit(
            "download_progress",
            serde_json::json!({
                "current": current,
                "total": total,
                "file": file,
            }),
        );
    };

    // Check if Minecraft is downloaded
    app.emit("launch_state", "checking").ok();

    // Download Minecraft if needed
    app.emit("launch_state", "downloading_minecraft").ok();
    state
        .minecraft_manager
        .download_minecraft(&version, progress_callback)
        .await
        .map_err(|e| e.to_string())?;

    // Download Fabric
    app.emit("launch_state", "downloading_fabric").ok();
    let fabric_version = state
        .minecraft_manager
        .get_fabric_loader(&version)
        .await
        .map_err(|e| e.to_string())?;

    let app_clone = app.clone();
    let progress_callback = move |current: u64, total: u64, file: &str| {
        let _ = app_clone.emit(
            "download_progress",
            serde_json::json!({
                "current": current,
                "total": total,
                "file": file,
            }),
        );
    };

    state
        .minecraft_manager
        .download_fabric(&version, &fabric_version, progress_callback)
        .await
        .map_err(|e| e.to_string())?;

    // Check for Miracle Client updates and install mod
    app.emit("launch_state", "downloading_mods").ok();

    // Map Minecraft version to mod version (1.21.4 or 1.21.8)
    // This is important because Supabase stores releases by mod version, not raw MC version
    let mod_version = get_mod_version_for_minecraft(&version);
    tracing::info!(
        "Checking for updates: MC {} -> Mod version {}",
        version,
        mod_version
    );

    // Check Supabase for mod updates
    if state.supabase.is_configured() {
        match state
            .supabase
            .check_miracle_client_update(MIRACLE_CLIENT_VERSION, mod_version)
            .await
        {
            Ok(update_info) => {
                if update_info.has_update {
                    tracing::info!(
                        "Miracle Client update available: {} -> {}",
                        update_info.current_version,
                        update_info.latest_version
                    );

                    // Get profile-specific mods directory
                    let game_dir = dirs::data_dir()
                        .unwrap_or_else(|| PathBuf::from("."))
                        .join("MiracleClient");
                    let version_mods_dir = game_dir.join("mods").join(&version);
                    let mods_dir = version_mods_dir.join(&profile_dir_name);

                    // Remove old Miracle Client jars from both version root and profile dir
                    for dir in [&version_mods_dir, &mods_dir] {
                        if dir.exists() {
                            if let Ok(entries) = std::fs::read_dir(dir) {
                                for entry in entries.flatten() {
                                    let filename = entry.file_name().to_string_lossy().to_string();
                                    if filename.starts_with("miracle-client")
                                        && filename.ends_with(".jar")
                                    {
                                        tracing::info!("Removing old mod: {}", filename);
                                        let _ = std::fs::remove_file(entry.path());
                                    }
                                }
                            }
                        }
                    }

                    // Download the update
                    app.emit("launch_state", "updating_mod").ok();
                    match state
                        .supabase
                        .download_mod_update(&update_info, &mods_dir)
                        .await
                    {
                        Ok(_) => {
                            tracing::info!("Miracle Client updated successfully");
                            app.emit(
                                "mod_updated",
                                serde_json::json!({
                                    "from": update_info.current_version,
                                    "to": update_info.latest_version,
                                    "changelog": update_info.changelog
                                }),
                            )
                            .ok();
                        }
                        Err(e) => {
                            tracing::error!("Failed to download mod update: {}", e);
                            // Fall back to bundled mod
                            install_bundled_mod(&app, &version, &profile_dir_name)
                                .await
                                .map_err(|e| e.to_string())?;
                        }
                    }
                } else {
                    // No update available, install bundled mod
                    install_bundled_mod(&app, &version, &profile_dir_name)
                        .await
                        .map_err(|e| e.to_string())?;
                }
            }
            Err(e) => {
                tracing::warn!("Failed to check for updates: {}, using bundled mod", e);
                install_bundled_mod(&app, &version, &profile_dir_name)
                    .await
                    .map_err(|e| e.to_string())?;
            }
        }
    } else {
        // Supabase not configured, use bundled mod
        install_bundled_mod(&app, &version, &profile_dir_name)
            .await
            .map_err(|e| e.to_string())?;
    }

    // Resolve and install missing dependencies (including Fabric API)
    tracing::info!("Resolving mod dependencies...");
    // Use profile-specific mods directory
    let mods_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("mods")
        .join(&version)
        .join(&profile_dir_name);

    if let Ok(installed_deps) = resolve_and_install_dependencies(&mods_dir, &version).await {
        if !installed_deps.is_empty() {
            tracing::info!(
                "Auto-installed {} dependencies: {:?}",
                installed_deps.len(),
                installed_deps
            );
        }
    }

    // Launch the game with profile-specific mods folder
    app.emit("launch_state", "launching").ok();

    let child_process = state
        .minecraft_manager
        .launch(
            &version,
            &fabric_version,
            &accessToken,
            &username,
            &uuid,
            ram,
            showGameLogs.unwrap_or(false),
            Some(&profile_dir_name),
        )
        .await
        .map_err(|e| e.to_string())?;

    // Get the process ID before storing
    let pid = child_process.id();

    // Store the child process and current player info
    *state.game_process.lock().unwrap() = Some(child_process);
    *state.current_player.lock().unwrap() = Some((uuid.clone(), username.clone()));

    app.emit("launch_state", "running").ok();

    // Update online status to "online"
    let _ = state.supabase.update_user_status(&uuid, true, None).await;
    tracing::info!("Set user {} online", username);

    // Hide the launcher window
    if let Some(window) = app.get_webview_window("main") {
        window.hide().ok();
    }

    // Monitor the process in a background task
    let app_clone = app.clone();
    let uuid_clone = uuid.clone();
    let username_clone = username.clone();
    tokio::spawn(async move {
        // Poll to check if process is still running and handle auth requests
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(1)).await;

            // Check for auth requests from mod
            check_and_handle_auth_request(&app_clone).await;

            // Check if process still exists
            #[cfg(target_os = "windows")]
            let is_running = std::process::Command::new("tasklist")
                .arg("/FI")
                .arg(format!("PID eq {}", pid))
                .output()
                .map(|output| {
                    let stdout = String::from_utf8_lossy(&output.stdout);
                    stdout.contains(&pid.to_string())
                })
                .unwrap_or(false);

            #[cfg(not(target_os = "windows"))]
            let is_running = std::process::Command::new("ps")
                .arg("-p")
                .arg(pid.to_string())
                .output()
                .map(|output| output.status.success())
                .unwrap_or(false);

            if !is_running {
                tracing::info!("Game process {} has exited", pid);
                // Get state to clear the process and update offline status
                if let Some(state) = app_clone.try_state::<AppState>() {
                    *state.game_process.lock().unwrap() = None;
                    *state.current_player.lock().unwrap() = None;
                    // Set user offline
                    let _ = state
                        .supabase
                        .update_user_status(&uuid_clone, false, None)
                        .await;
                    tracing::info!("Set user {} offline", username_clone);
                }

                // Check if mod requested an account switch
                if let Some(switch_account_id) = check_switch_request().await {
                    tracing::info!("Account switch requested to: {}", switch_account_id);
                    app_clone
                        .emit(
                            "account_switch_requested",
                            serde_json::json!({
                                "account_id": switch_account_id
                            }),
                        )
                        .ok();
                }

                app_clone.emit("launch_state", "idle").ok();

                // Show the launcher window again
                if let Some(window) = app_clone.get_webview_window("main") {
                    window.show().ok();
                    window.set_focus().ok();
                }
                break;
            }
        }
    });

    Ok(())
}

/// Sync the launcher's theme to the mod's config file
async fn sync_theme_to_mod_config(theme_name: &str) -> Result<(), String> {
    // Write to MiracleClient/config/miracle/ (same path Fabric mod reads from)
    // The game runs from MiracleClient/, not .minecraft/
    let config_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("config")
        .join("miracle");

    // Create config directory if it doesn't exist
    tokio::fs::create_dir_all(&config_dir)
        .await
        .map_err(|e| format!("Failed to create config directory: {}", e))?;

    let config_file = config_dir.join("config.json");

    // Read existing config or create new
    let mut config: serde_json::Value = if config_file.exists() {
        let content = tokio::fs::read_to_string(&config_file)
            .await
            .unwrap_or_else(|_| "{}".to_string());
        serde_json::from_str(&content).unwrap_or(serde_json::json!({}))
    } else {
        serde_json::json!({})
    };

    // Convert theme name to uppercase for mod compatibility (e.g., "midnight" -> "MIDNIGHT")
    let theme_upper = theme_name.to_uppercase().replace("-", "_");

    // Update theme in config
    config["theme"] = serde_json::Value::String(theme_upper);

    // Write config back
    let content = serde_json::to_string_pretty(&config)
        .map_err(|e| format!("Failed to serialize config: {}", e))?;

    tokio::fs::write(&config_file, content)
        .await
        .map_err(|e| format!("Failed to write config: {}", e))?;

    Ok(())
}

/// Write accounts list for mod to read (for in-game account switching)
async fn write_accounts_for_mod(accounts: &[AccountForMod]) -> Result<(), String> {
    let config_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("config")
        .join("miracle");

    tokio::fs::create_dir_all(&config_dir)
        .await
        .map_err(|e| format!("Failed to create config directory: {}", e))?;

    let accounts_file = config_dir.join("accounts.json");
    let content = serde_json::to_string_pretty(accounts)
        .map_err(|e| format!("Failed to serialize accounts: {}", e))?;

    tokio::fs::write(&accounts_file, content)
        .await
        .map_err(|e| format!("Failed to write accounts file: {}", e))?;

    tracing::info!("Wrote {} accounts for mod to read", accounts.len());
    Ok(())
}

/// Check if mod requested an account switch, returns account ID if so
async fn check_switch_request() -> Option<String> {
    let switch_file = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("config")
        .join("miracle")
        .join("switch_account.json");

    if !switch_file.exists() {
        return None;
    }

    // Read and delete the file
    let content = std::fs::read_to_string(&switch_file).ok()?;
    let _ = std::fs::remove_file(&switch_file);

    let json: serde_json::Value = serde_json::from_str(&content).ok()?;
    json.get("account_id")
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
}

/// Get the miracle config directory path
fn get_miracle_config_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("config")
        .join("miracle")
}

/// Check for auth request from mod and process it
async fn check_and_handle_auth_request(app: &AppHandle) -> bool {
    let config_dir = get_miracle_config_dir();
    let request_file = config_dir.join("auth_request.json");

    if !request_file.exists() {
        return false;
    }

    // Read and delete the request
    let content = match std::fs::read_to_string(&request_file) {
        Ok(c) => c,
        Err(_) => return false,
    };
    let _ = std::fs::remove_file(&request_file);

    let json: serde_json::Value = match serde_json::from_str(&content) {
        Ok(j) => j,
        Err(_) => return false,
    };

    let action = match json.get("action").and_then(|v| v.as_str()) {
        Some(a) => a,
        None => return false,
    };

    if action == "start_device_flow" {
        tracing::info!("[Auth] Mod requested device flow");

        let state = match app.try_state::<AppState>() {
            Some(s) => s,
            None => return false,
        };

        // Start device code flow
        match state.auth_manager.start_device_code_flow().await {
            Ok(device_code) => {
                // Write response with device code info
                let response = serde_json::json!({
                    "status": "pending",
                    "user_code": device_code.user_code,
                    "verification_uri": device_code.verification_uri,
                    "device_code": device_code.device_code,
                    "interval": device_code.interval,
                    "expires_in": device_code.expires_in
                });

                let response_file = config_dir.join("auth_response.json");
                if let Err(e) = std::fs::write(
                    &response_file,
                    serde_json::to_string_pretty(&response).unwrap(),
                ) {
                    tracing::error!("[Auth] Failed to write response: {}", e);
                    return false;
                }

                tracing::info!(
                    "[Auth] Device code: {}, URL: {}",
                    device_code.user_code,
                    device_code.verification_uri
                );

                // Start polling in background
                let app_clone = app.clone();
                let device_code_str = device_code.device_code.clone();
                let interval = device_code.interval;

                tokio::spawn(async move {
                    poll_device_flow_for_mod(app_clone, device_code_str, interval).await;
                });

                return true;
            }
            Err(e) => {
                tracing::error!("[Auth] Failed to start device flow: {}", e);
                let response = serde_json::json!({
                    "status": "error",
                    "error": e.to_string()
                });
                let response_file = config_dir.join("auth_response.json");
                let _ = std::fs::write(
                    &response_file,
                    serde_json::to_string_pretty(&response).unwrap(),
                );
                return false;
            }
        }
    }

    false
}

/// Poll device flow and write result for mod
async fn poll_device_flow_for_mod(app: AppHandle, device_code: String, interval: u64) {
    let config_dir = get_miracle_config_dir();

    let state = match app.try_state::<AppState>() {
        Some(s) => s,
        None => return,
    };

    match state
        .auth_manager
        .poll_and_authenticate(&device_code, interval)
        .await
    {
        Ok(profile) => {
            tracing::info!("[Auth] Device flow succeeded for: {}", profile.name);

            // Write success response
            let response = serde_json::json!({
                "status": "success",
                "account": {
                    "id": profile.id,
                    "name": profile.name
                }
            });

            let response_file = config_dir.join("auth_response.json");
            if let Err(e) = std::fs::write(
                &response_file,
                serde_json::to_string_pretty(&response).unwrap(),
            ) {
                tracing::error!("[Auth] Failed to write success response: {}", e);
            }

            // Also update accounts.json so mod can see the new account immediately
            let accounts_file = config_dir.join("accounts.json");
            if let Ok(content) = std::fs::read_to_string(&accounts_file) {
                if let Ok(mut accounts) = serde_json::from_str::<Vec<AccountForMod>>(&content) {
                    // Add new account if not already present
                    if !accounts.iter().any(|a| a.id == profile.id) {
                        accounts.push(AccountForMod {
                            id: profile.id.clone(),
                            name: profile.name.clone(),
                            is_active: false,
                        });
                        if let Ok(new_content) = serde_json::to_string_pretty(&accounts) {
                            let _ = std::fs::write(&accounts_file, new_content);
                            tracing::info!("[Auth] Updated accounts.json with new account");
                        }
                    }
                }
            }

            // Emit event to frontend to update accounts list
            app.emit(
                "account_added",
                serde_json::json!({
                    "id": profile.id,
                    "name": profile.name,
                    "accessToken": profile.access_token,
                    "refreshToken": profile.refresh_token,
                    "expiresAt": profile.expires_at
                }),
            )
            .ok();
        }
        Err(e) => {
            tracing::error!("[Auth] Device flow failed: {}", e);
            let response = serde_json::json!({
                "status": "error",
                "error": e.to_string()
            });
            let response_file = config_dir.join("auth_response.json");
            let _ = std::fs::write(
                &response_file,
                serde_json::to_string_pretty(&response).unwrap(),
            );
        }
    }
}

/// Account info for mod (minimal, no tokens)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AccountForMod {
    pub id: String,      // UUID
    pub name: String,    // Username
    pub is_active: bool, // Is this the currently playing account
}

/// Read the theme from the mod's config file (for syncing mod→launcher)
#[tauri::command]
pub async fn get_mod_theme() -> Result<Option<String>, String> {
    let config_file = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("config")
        .join("miracle")
        .join("config.json");

    if !config_file.exists() {
        return Ok(None);
    }

    let content = tokio::fs::read_to_string(&config_file)
        .await
        .map_err(|e| format!("Failed to read config: {}", e))?;

    let config: serde_json::Value =
        serde_json::from_str(&content).map_err(|e| format!("Failed to parse config: {}", e))?;

    // Get theme and convert from uppercase to lowercase (e.g., "MIDNIGHT" -> "midnight")
    if let Some(theme) = config.get("theme").and_then(|v| v.as_str()) {
        Ok(Some(theme.to_lowercase().replace("_", "-")))
    } else {
        Ok(None)
    }
}
/// Install the bundled Miracle Client mod
async fn install_bundled_mod(
    app: &AppHandle,
    minecraft_version: &str,
    profile_dir_name: &str,
) -> Result<(), String> {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    // Use profile-specific mods directory (profile_dir_name is sanitized profile name)
    let mods_dir = game_dir
        .join("mods")
        .join(minecraft_version)
        .join(profile_dir_name);
    tokio::fs::create_dir_all(&mods_dir)
        .await
        .map_err(|e| e.to_string())?;

    // Determine which mod version to use based on Minecraft version
    let mod_version = get_mod_version_for_minecraft(minecraft_version);
    let mod_filename = format!("miracle-client-{}.jar", mod_version);

    // Try production path first (bundled resources)
    let resource_path = app
        .path()
        .resource_dir()
        .map_err(|e| e.to_string())?
        .join("resources")
        .join(&mod_filename);

    // Fallback to dev mode path (src-tauri/resources/)
    let dev_resource_path = std::env::current_dir()
        .unwrap_or_else(|_| PathBuf::from("."))
        .join("src-tauri")
        .join("resources")
        .join(&mod_filename);

    let source_path = if resource_path.exists() {
        tracing::info!("Using production resource path: {:?}", resource_path);
        resource_path
    } else if dev_resource_path.exists() {
        tracing::info!("Using dev resource path: {:?}", dev_resource_path);
        dev_resource_path
    } else {
        return Err(format!(
            "Miracle Client mod for Minecraft {} not found. Tried:\n  - {:?}\n  - {:?}",
            minecraft_version, resource_path, dev_resource_path
        ));
    };

    let dest_path = mods_dir.join(&mod_filename);

    // Clean up any old miracle-client jars in this folder (different versions or old 1.0.0 naming)
    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
                if name.starts_with("miracle-client-")
                    && name.ends_with(".jar")
                    && name != mod_filename
                {
                    if let Err(e) = std::fs::remove_file(&path) {
                        tracing::warn!("Failed to remove old mod {:?}: {}", path, e);
                    } else {
                        tracing::info!("Removed old mod: {:?}", path);
                    }
                }
            }
        }
    }

    tokio::fs::copy(&source_path, &dest_path)
        .await
        .map_err(|e| format!("Failed to install Miracle Client mod: {}", e))?;
    tracing::info!(
        "Installed Miracle Client mod version {} to {:?}",
        mod_version,
        dest_path
    );

    Ok(())
}

/// Check which mods are compatible with a specific Minecraft version
#[tauri::command]
pub async fn check_mod_compatibility(
    mod_slugs: Vec<String>,
    minecraft_version: String,
) -> Result<Vec<ModCompatibility>, String> {
    check_mods_compatibility(mod_slugs, &minecraft_version).await
}

/// Delete all mod folders (owner only - for debugging)
#[tauri::command]
pub async fn delete_all_mod_folders() -> Result<String, String> {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    let mods_dir = game_dir.join("mods");

    if !mods_dir.exists() {
        return Ok("No mods directory found".to_string());
    }

    // Read all subdirectories in mods/
    let mut deleted_count = 0;
    let entries = std::fs::read_dir(&mods_dir).map_err(|e| e.to_string())?;

    for entry in entries {
        if let Ok(entry) = entry {
            let path = entry.path();
            if path.is_dir() {
                tracing::info!("Deleting mod folder: {:?}", path);
                std::fs::remove_dir_all(&path).map_err(|e| e.to_string())?;
                deleted_count += 1;
            }
        }
    }

    Ok(format!("Deleted {} version mod folders", deleted_count))
}

#[tauri::command]
pub async fn stop_game(app: AppHandle) -> Result<(), String> {
    let state = app.state::<AppState>();

    // Get current player info before clearing
    let player_info = state.current_player.lock().unwrap().take();

    if let Some(mut process) = state.game_process.lock().unwrap().take() {
        process.kill().map_err(|e| e.to_string())?;
    }

    // Set user offline if we have their info
    if let Some((uuid, username)) = player_info {
        let _ = state.supabase.update_user_status(&uuid, false, None).await;
        tracing::info!("Set user {} offline (manual stop)", username);
    }

    app.emit("launch_state", "idle").ok();

    // Show the launcher window again
    if let Some(window) = app.get_webview_window("main") {
        window.show().ok();
        window.set_focus().ok();
    }

    Ok(())
}

/// Resolve and install missing dependencies for all mods
#[tauri::command]
pub async fn resolve_dependencies_for_version(
    minecraft_version: String,
) -> Result<Vec<String>, String> {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    let mods_dir = game_dir.join("mods").join(&minecraft_version);

    if !mods_dir.exists() {
        return Ok(Vec::new());
    }

    tracing::info!(
        "Resolving dependencies for Minecraft {}...",
        minecraft_version
    );

    let installed = resolve_and_install_dependencies(&mods_dir, &minecraft_version).await?;

    if installed.is_empty() {
        tracing::info!("All dependencies satisfied!");
    } else {
        tracing::info!(
            "Installed {} dependencies: {:?}",
            installed.len(),
            installed
        );
    }

    Ok(installed)
}

// ==================== Update Commands ====================

#[tauri::command]
pub async fn check_updates(app: AppHandle) -> Result<UpdateInfo, String> {
    let state = app.state::<AppState>();

    state
        .update_manager
        .check_launcher_update()
        .await
        .map_err(|e| e.to_string())
}

// ==================== Mod Commands ====================

#[derive(Debug, Serialize, Deserialize)]
pub struct ModInfo {
    id: String,
    name: String,
    version: String,
    enabled: bool,
    filename: String,
}

/// Helper function to get the mods directory for a version and optional profile.
/// The profile_dir param should be the sanitized profile name (use sanitize_profile_name).
fn get_mods_directory(minecraft_version: Option<&str>, profile_dir: Option<&str>) -> PathBuf {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    match (minecraft_version, profile_dir) {
        (Some(version), Some(profile)) => game_dir.join("mods").join(version).join(profile),
        (Some(version), None) => game_dir.join("mods").join(version),
        _ => game_dir.join("mods"),
    }
}

fn get_resourcepacks_directory(
    minecraft_version: Option<&str>,
    profile_dir: Option<&str>,
) -> PathBuf {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    match (minecraft_version, profile_dir) {
        (Some(version), Some(profile)) => {
            game_dir.join("resourcepacks").join(version).join(profile)
        }
        (Some(version), None) => game_dir.join("resourcepacks").join(version),
        _ => game_dir.join("resourcepacks"),
    }
}

fn get_shaderpacks_directory(
    minecraft_version: Option<&str>,
    profile_dir: Option<&str>,
) -> PathBuf {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    match (minecraft_version, profile_dir) {
        (Some(version), Some(profile)) => game_dir.join("shaderpacks").join(version).join(profile),
        (Some(version), None) => game_dir.join("shaderpacks").join(version),
        _ => game_dir.join("shaderpacks"),
    }
}

fn get_datapacks_directory(minecraft_version: Option<&str>, profile_dir: Option<&str>) -> PathBuf {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");

    match (minecraft_version, profile_dir) {
        (Some(version), Some(profile)) => game_dir.join("datapacks").join(version).join(profile),
        (Some(version), None) => game_dir.join("datapacks").join(version),
        _ => game_dir.join("datapacks"),
    }
}

/// Look up profile name from ID and return sanitized directory name
pub(super) fn get_profile_dir_name(state: &AppState, profile_id: &str) -> String {
    let manager = state.profile_manager.lock().unwrap();
    if let Some(profile) = manager.get_profile(profile_id) {
        sanitize_profile_name(&profile.name)
    } else {
        sanitize_profile_name(profile_id)
    }
}

/// Install performance mods (sodium, lithium, etc.) to a profile's mods directory
pub async fn install_performance_mods(
    mods_dir: &PathBuf,
    minecraft_version: &str,
) -> Result<Vec<String>, String> {
    let mut installed = Vec::new();

    // Ensure mods directory exists
    tokio::fs::create_dir_all(mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    // Check which performance mods are already installed
    let existing_files: Vec<String> = if mods_dir.exists() {
        std::fs::read_dir(mods_dir)
            .map(|entries| {
                entries
                    .filter_map(|e| e.ok())
                    .map(|e| e.file_name().to_string_lossy().to_lowercase())
                    .collect()
            })
            .unwrap_or_default()
    } else {
        Vec::new()
    };

    for mod_slug in PERFORMANCE_MODS {
        // Check if this mod is already installed (by checking if filename contains the slug)
        let already_installed = existing_files
            .iter()
            .any(|f| f.contains(mod_slug) && (f.ends_with(".jar") || f.ends_with(".jar.disabled")));

        if already_installed {
            tracing::debug!("Performance mod {} already installed", mod_slug);
            continue;
        }

        // Download from Modrinth directly to the mods directory
        tracing::info!("Installing performance mod: {}", mod_slug);
        match modrinth::download_mod_to_dir(mod_slug, minecraft_version, mods_dir).await {
            Ok(filename) => {
                tracing::info!("Installed performance mod: {} -> {}", mod_slug, filename);
                installed.push(mod_slug.to_string());
            }
            Err(e) => {
                tracing::warn!("Failed to install performance mod {}: {}", mod_slug, e);
                // Continue with other mods, don't fail completely
            }
        }
    }

    Ok(installed)
}

/// Install performance mods to a specific profile directory
#[tauri::command]
pub async fn ensure_performance_mods(
    app: AppHandle,
    minecraft_version: String,
    profile_id: String,
) -> Result<Vec<String>, String> {
    let state = app.state::<AppState>();
    let profile_dir = get_profile_dir_name(&state, &profile_id);
    let mods_dir = get_mods_directory(Some(&minecraft_version), Some(&profile_dir));
    install_performance_mods(&mods_dir, &minecraft_version).await
}

#[tauri::command]
pub async fn get_installed_mods(
    app: AppHandle,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<Vec<ModInfo>, String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let mods_dir = get_mods_directory(minecraft_version.as_deref(), profile_dir.as_deref());

    tracing::info!(
        "get_installed_mods: checking directory {:?} (version={:?}, profile={:?})",
        mods_dir,
        minecraft_version,
        profile_id
    );

    if !mods_dir.exists() {
        tracing::info!("get_installed_mods: directory does not exist");
        return Ok(Vec::new());
    }

    let mut mods = Vec::new();

    // Read all .jar and .jar.disabled files
    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = entry.file_name().to_string_lossy().to_string();

            let (is_enabled, jar_path) = if filename.ends_with(".jar.disabled") {
                (false, path.clone())
            } else if filename.ends_with(".jar") {
                (true, path.clone())
            } else {
                continue;
            };

            // Try to parse fabric.mod.json from the JAR
            if let Ok(file) = std::fs::File::open(&jar_path) {
                if let Ok(mut archive) = zip::ZipArchive::new(file) {
                    if let Ok(mut mod_json_file) = archive.by_name("fabric.mod.json") {
                        let mut contents = String::new();
                        if std::io::Read::read_to_string(&mut mod_json_file, &mut contents).is_ok()
                        {
                            if let Ok(json) = serde_json::from_str::<serde_json::Value>(&contents) {
                                let id = json["id"].as_str().unwrap_or(&filename).to_string();
                                let name = json["name"].as_str().unwrap_or(&id).to_string();
                                let version =
                                    json["version"].as_str().unwrap_or("Unknown").to_string();

                                mods.push(ModInfo {
                                    id: id.clone(),
                                    name,
                                    version,
                                    enabled: is_enabled,
                                    filename: filename.clone(),
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    tracing::info!("get_installed_mods: found {} mods", mods.len());
    mods.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(mods)
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ContentInfo {
    name: String,
    filename: String,
    enabled: bool,
}

#[tauri::command]
pub async fn get_installed_resourcepacks(
    app: AppHandle,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<Vec<ContentInfo>, String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let dir = get_resourcepacks_directory(minecraft_version.as_deref(), profile_dir.as_deref());
    tracing::info!(
        "get_installed_resourcepacks: Looking in {:?} (version={:?}, profile={:?})",
        dir,
        minecraft_version,
        profile_dir
    );
    tracing::info!("Directory exists: {}", dir.exists());


    if !dir.exists() {
        return Ok(Vec::new());
    }

    let mut packs = Vec::new();
    if let Ok(entries) = std::fs::read_dir(&dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = entry.file_name().to_string_lossy().to_string();

            let (is_enabled, name) = if filename.ends_with(".zip.disabled") {
                (false, filename.trim_end_matches(".disabled").to_string())
            } else if filename.ends_with(".zip") {
                (true, filename.clone())
            } else if path.is_dir() {
                (true, filename.clone())
            } else {
                continue;
            };

            packs.push(ContentInfo {
                name: name.trim_end_matches(".zip").to_string(),
                filename,
                enabled: is_enabled,
            });
        }
    }

    tracing::info!("Found {} resource packs in {:?}", packs.len(), dir);
    packs.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(packs)
}

#[tauri::command]
pub async fn get_installed_shaders(
    app: AppHandle,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<Vec<ContentInfo>, String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let dir = get_shaderpacks_directory(minecraft_version.as_deref(), profile_dir.as_deref());
    tracing::info!(
        "get_installed_shaders: Looking in {:?} (version={:?}, profile={:?})",
        dir,
        minecraft_version,
        profile_dir
    );
    tracing::info!("Directory exists: {}", dir.exists());

    if !dir.exists() {
        return Ok(Vec::new());
    }

    let mut packs = Vec::new();
    if let Ok(entries) = std::fs::read_dir(&dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = entry.file_name().to_string_lossy().to_string();

            let (is_enabled, name) = if filename.ends_with(".zip.disabled") {
                (false, filename.trim_end_matches(".disabled").to_string())
            } else if filename.ends_with(".zip") {
                (true, filename.clone())
            } else if path.is_dir() {
                (true, filename.clone())
            } else {
                continue;
            };

            packs.push(ContentInfo {
                name: name.trim_end_matches(".zip").to_string(),
                filename,
                enabled: is_enabled,
            });
        }
    }

    tracing::info!("Found {} shader packs in {:?}", packs.len(), dir);
    packs.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(packs)
}

#[tauri::command]
pub async fn get_installed_datapacks(
    app: AppHandle,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<Vec<ContentInfo>, String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let dir = get_datapacks_directory(minecraft_version.as_deref(), profile_dir.as_deref());
    tracing::info!(
        "get_installed_datapacks: Looking in {:?} (version={:?}, profile={:?})",
        dir,
        minecraft_version,
        profile_dir
    );
    tracing::info!("Directory exists: {}", dir.exists());


    if !dir.exists() {
        return Ok(Vec::new());
    }

    let mut packs = Vec::new();
    if let Ok(entries) = std::fs::read_dir(&dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = entry.file_name().to_string_lossy().to_string();

            let (is_enabled, name) = if filename.ends_with(".zip.disabled") {
                (false, filename.trim_end_matches(".disabled").to_string())
            } else if filename.ends_with(".zip") {
                (true, filename.clone())
            } else if path.is_dir() {
                (true, filename.clone())
            } else {
                continue;
            };

            packs.push(ContentInfo {
                name: name.trim_end_matches(".zip").to_string(),
                filename,
                enabled: is_enabled,
            });
        }
    }

    tracing::info!("Found {} datapacks in {:?}", packs.len(), dir);
    packs.sort_by(|a, b| a.name.cmp(&b.name));
    Ok(packs)
}

#[tauri::command]
pub async fn toggle_mod(
    app: AppHandle,
    mod_id: String,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<(), String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let mods_dir = get_mods_directory(minecraft_version.as_deref(), profile_dir.as_deref());

    // Find the mod file
    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = entry.file_name().to_string_lossy().to_string();

            // Check if this is the mod we're looking for
            let should_toggle = {
                if let Ok(file) = std::fs::File::open(&path) {
                    if let Ok(mut archive) = zip::ZipArchive::new(file) {
                        if let Ok(mut mod_json_file) = archive.by_name("fabric.mod.json") {
                            let mut contents = String::new();
                            if std::io::Read::read_to_string(&mut mod_json_file, &mut contents)
                                .is_ok()
                            {
                                if let Ok(json) =
                                    serde_json::from_str::<serde_json::Value>(&contents)
                                {
                                    json["id"].as_str() == Some(&mod_id)
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            };

            if should_toggle {
                // Toggle the mod
                if filename.ends_with(".jar.disabled") {
                    // Enable it
                    let new_path = mods_dir.join(filename.trim_end_matches(".disabled"));
                    std::fs::rename(&path, &new_path).map_err(|e| e.to_string())?;
                } else if filename.ends_with(".jar") {
                    // Disable it
                    let new_path = mods_dir.join(format!("{}.disabled", filename));
                    std::fs::rename(&path, &new_path).map_err(|e| e.to_string())?;
                }

                return Ok(());
            }
        }
    }

    Err(format!("Mod with id '{}' not found", mod_id))
}

#[tauri::command]
pub async fn uninstall_mod(
    app: AppHandle,
    mod_id: String,
    minecraft_version: Option<String>,
    profile_id: Option<String>,
) -> Result<(), String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let mods_dir = get_mods_directory(minecraft_version.as_deref(), profile_dir.as_deref());

    // Find and delete the mod file
    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();

            // Check if this is the mod we're looking for
            let should_delete = {
                if let Ok(file) = std::fs::File::open(&path) {
                    if let Ok(mut archive) = zip::ZipArchive::new(file) {
                        if let Ok(mut mod_json_file) = archive.by_name("fabric.mod.json") {
                            let mut contents = String::new();
                            if std::io::Read::read_to_string(&mut mod_json_file, &mut contents)
                                .is_ok()
                            {
                                if let Ok(json) =
                                    serde_json::from_str::<serde_json::Value>(&contents)
                                {
                                    json["id"].as_str() == Some(&mod_id)
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            };

            if should_delete {
                // Delete the mod file
                std::fs::remove_file(&path).map_err(|e| e.to_string())?;
                return Ok(());
            }
        }
    }

    Err(format!("Mod with id '{}' not found", mod_id))
}

#[tauri::command]
pub async fn download_mod(
    app: AppHandle,
    mod_slug: String,
    minecraft_version: String,
    curseforge_id: Option<i32>,
    profile_id: Option<String>,
) -> Result<String, String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let mods_dir = get_mods_directory(Some(&minecraft_version), profile_dir.as_deref());

    // Create version-specific mods directory if it doesn't exist
    tokio::fs::create_dir_all(&mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    // If curseforge_id is provided, use CurseForge API
    if let Some(project_id) = curseforge_id {
        return curseforge::download_curseforge_mod(project_id, &minecraft_version, &mods_dir)
            .await;
    }

    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(120))
        .user_agent("MiracleClient/1.0")
        .build()
        .map_err(|e| e.to_string())?;

    // Get mod versions from Modrinth
    tracing::info!("Fetching versions for mod: {}", mod_slug);
    let versions_url = format!(
        "https://api.modrinth.com/v2/project/{}/version?game_versions=[\"{}\"]&loaders=[\"fabric\"]",
        mod_slug, minecraft_version
    );

    let versions_response = client
        .get(&versions_url)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch mod versions: {}", e))?;

    if !versions_response.status().is_success() {
        return Err(format!(
            "Failed to fetch mod versions: HTTP {}",
            versions_response.status()
        ));
    }

    let versions: Vec<serde_json::Value> = versions_response
        .json()
        .await
        .map_err(|e| format!("Failed to parse versions response: {}", e))?;

    if let Some(latest_version) = versions.first() {
        let files = latest_version["files"]
            .as_array()
            .ok_or("No files found in version")?;

        if let Some(file) = files.first() {
            let download_url = file["url"].as_str().ok_or("No download URL found")?;
            let filename = file["filename"].as_str().ok_or("No filename found")?;
            let mod_path = mods_dir.join(filename);

            // Check if already downloaded
            if mod_path.exists() {
                return Ok(format!("Mod already installed: {}", filename));
            }

            tracing::info!("Downloading mod: {}", filename);
            app.emit("download_progress", format!("Downloading {}", filename))
                .ok();

            let response = client
                .get(download_url)
                .send()
                .await
                .map_err(|e| format!("Failed to download mod: {}", e))?;

            if !response.status().is_success() {
                return Err(format!(
                    "Failed to download mod: HTTP {}",
                    response.status()
                ));
            }

            let bytes = response
                .bytes()
                .await
                .map_err(|e| format!("Failed to read mod bytes: {}", e))?;

            if bytes.len() < 1000 {
                return Err("Downloaded mod file is too small".to_string());
            }

            tokio::fs::write(&mod_path, bytes)
                .await
                .map_err(|e| format!("Failed to write mod file: {}", e))?;

            tracing::info!("Mod downloaded successfully: {}", filename);
            Ok(format!("Successfully installed: {}", filename))
        } else {
            Err("No download file found in version".to_string())
        }
    } else {
        Err(format!(
            "No compatible version found for Minecraft {} (Fabric)",
            minecraft_version
        ))
    }
}

#[tauri::command]
pub async fn open_mods_folder(
    app: AppHandle,
    minecraft_version: String,
    profile_id: Option<String>,
) -> Result<(), String> {
    let profile_dir = profile_id.as_ref().map(|pid| {
        let state = app.state::<AppState>();
        get_profile_dir_name(&state, pid)
    });
    let mods_dir = get_mods_directory(Some(&minecraft_version), profile_dir.as_deref());

    // Create the directory if it doesn't exist
    if !mods_dir.exists() {
        tokio::fs::create_dir_all(&mods_dir)
            .await
            .map_err(|e| format!("Failed to create mods directory: {}", e))?;
    }

    // Open the folder with the system's default file explorer
    #[cfg(target_os = "windows")]
    {
        std::process::Command::new("explorer")
            .arg(mods_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(mods_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    #[cfg(target_os = "linux")]
    {
        std::process::Command::new("xdg-open")
            .arg(mods_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    Ok(())
}

// ==================== Miracle Client Update Commands ====================

/// Current version of the bundled Miracle Client mod
const MIRACLE_CLIENT_VERSION: &str = "1.0.0";

/// Check for Miracle Client mod updates
#[tauri::command]
pub async fn check_miracle_update(
    app: AppHandle,
    minecraft_version: String,
) -> Result<ModUpdateInfo, String> {
    let state = app.state::<AppState>();

    // Map Minecraft version to mod version for Supabase lookup
    let mod_version = get_mod_version_for_minecraft(&minecraft_version);

    tracing::info!(
        "Checking for Miracle Client updates (current: {}, MC: {} -> Mod: {})",
        MIRACLE_CLIENT_VERSION,
        minecraft_version,
        mod_version
    );

    state
        .supabase
        .check_miracle_client_update(MIRACLE_CLIENT_VERSION, mod_version)
        .await
}

/// Download and install a Miracle Client mod update
#[tauri::command]
pub async fn download_miracle_update(
    app: AppHandle,
    minecraft_version: String,
) -> Result<String, String> {
    let state = app.state::<AppState>();

    // Map Minecraft version to mod version for Supabase lookup
    let mod_version = get_mod_version_for_minecraft(&minecraft_version);

    // First check if there's an update available
    let update_info = state
        .supabase
        .check_miracle_client_update(MIRACLE_CLIENT_VERSION, mod_version)
        .await?;

    if !update_info.has_update {
        return Ok("Already up to date".to_string());
    }

    // Get the mods directory for this Minecraft version
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient");
    let mods_dir = game_dir.join("mods").join(&minecraft_version);

    // Remove old Miracle Client jars from version dir and all profile subdirs
    if mods_dir.exists() {
        // Clean version root
        if let Ok(entries) = std::fs::read_dir(&mods_dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                let filename = entry.file_name().to_string_lossy().to_string();

                // Remove miracle jars from version root
                if filename.starts_with("miracle-client") && filename.ends_with(".jar") {
                    tracing::info!("Removing old mod: {}", filename);
                    let _ = std::fs::remove_file(&path);
                }

                // Also clean profile subdirectories
                if path.is_dir() {
                    if let Ok(sub_entries) = std::fs::read_dir(&path) {
                        for sub_entry in sub_entries.flatten() {
                            let sub_filename = sub_entry.file_name().to_string_lossy().to_string();
                            if sub_filename.starts_with("miracle-client")
                                && sub_filename.ends_with(".jar")
                            {
                                tracing::info!("Removing old mod from profile: {}", sub_filename);
                                let _ = std::fs::remove_file(sub_entry.path());
                            }
                        }
                    }
                }
            }
        }
    }

    // Download the new version
    app.emit("launch_state", "updating_mod").ok();

    let downloaded_path = state
        .supabase
        .download_mod_update(&update_info, &mods_dir)
        .await?;

    tracing::info!(
        "Miracle Client updated from {} to {}",
        update_info.current_version,
        update_info.latest_version
    );

    Ok(format!(
        "Updated Miracle Client to version {}",
        update_info.latest_version
    ))
}

/// Check if Supabase is configured for updates
#[tauri::command]
pub async fn is_update_service_configured(app: AppHandle) -> Result<bool, String> {
    let state = app.state::<AppState>();
    Ok(state.supabase.is_configured())
}

// ==================== Friends Commands ====================

/// Register/update user in the friends system
#[tauri::command]
pub async fn friends_register_user(
    app: AppHandle,
    minecraft_uuid: String,
    username: String,
) -> Result<User, String> {
    let state = app.state::<AppState>();
    state
        .supabase
        .get_or_create_user(&minecraft_uuid, &username)
        .await
}

/// Search for users by username
#[tauri::command]
pub async fn friends_search_users(
    app: AppHandle,
    query: String,
    exclude_uuid: String,
) -> Result<Vec<User>, String> {
    let state = app.state::<AppState>();
    state.supabase.search_users(&query, &exclude_uuid).await
}

/// Get all friends and pending requests
#[tauri::command]
pub async fn friends_get_list(
    app: AppHandle,
    minecraft_uuid: String,
) -> Result<Vec<Friend>, String> {
    let state = app.state::<AppState>();
    state.supabase.get_friends(&minecraft_uuid).await
}

/// Send a friend request
#[tauri::command]
pub async fn friends_send_request(
    app: AppHandle,
    from_uuid: String,
    from_username: String,
    to_user_id: String,
) -> Result<FriendRequestResult, String> {
    let state = app.state::<AppState>();
    state
        .supabase
        .send_friend_request(&from_uuid, &from_username, &to_user_id)
        .await
}

/// Accept a friend request
#[tauri::command]
pub async fn friends_accept_request(
    app: AppHandle,
    friendship_id: String,
) -> Result<FriendRequestResult, String> {
    let state = app.state::<AppState>();
    state.supabase.accept_friend_request(&friendship_id).await
}

/// Remove a friendship or decline a request
#[tauri::command]
pub async fn friends_remove(
    app: AppHandle,
    friendship_id: String,
) -> Result<FriendRequestResult, String> {
    let state = app.state::<AppState>();
    state.supabase.remove_friendship(&friendship_id).await
}

/// Update user's online status
#[tauri::command]
pub async fn friends_update_status(
    app: AppHandle,
    minecraft_uuid: String,
    is_online: bool,
    current_server: Option<String>,
) -> Result<(), String> {
    let state = app.state::<AppState>();
    state
        .supabase
        .update_user_status(&minecraft_uuid, is_online, current_server.as_deref())
        .await
}

// ==================== Profile Commands ====================

/// Get all profiles for a Minecraft version
#[tauri::command]
pub async fn get_profiles(
    app: AppHandle,
    minecraft_version: String,
) -> Result<Vec<Profile>, String> {
    let state = app.state::<AppState>();
    let manager = state.profile_manager.lock().unwrap();
    let mut profiles = manager.get_profiles(&minecraft_version);

    // For each profile, scan the mods folder to get actual mod count
    for profile in &mut profiles {
        let mods_dir = manager.get_mods_dir(&minecraft_version, &profile.id);
        if mods_dir.exists() {
            if let Ok(entries) = std::fs::read_dir(&mods_dir) {
                let mod_names: Vec<String> = entries
                    .flatten()
                    .filter_map(|entry| {
                        let filename = entry.file_name().to_string_lossy().to_string();
                        if filename.ends_with(".jar") || filename.ends_with(".jar.disabled") {
                            // Extract mod name from filename (remove .jar or .jar.disabled)
                            let name = filename
                                .trim_end_matches(".disabled")
                                .trim_end_matches(".jar")
                                .to_string();
                            Some(name)
                        } else {
                            None
                        }
                    })
                    .collect();
                profile.mods = mod_names;
            }
        }
    }

    Ok(profiles)
}

/// Get the active profile for a version (creates default if none exists)
#[tauri::command]
pub async fn get_active_profile(
    app: AppHandle,
    minecraft_version: String,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.get_active_profile(&minecraft_version)
}

/// Set the active profile for a version
#[tauri::command]
pub async fn set_active_profile(
    app: AppHandle,
    minecraft_version: String,
    profile_id: String,
) -> Result<(), String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.set_active_profile(&minecraft_version, &profile_id)
}

/// Create a new custom profile
#[tauri::command]
pub async fn create_profile(
    app: AppHandle,
    name: String,
    minecraft_version: String,
    base_profile_id: Option<String>,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.create_profile(&name, &minecraft_version, base_profile_id.as_deref())
}

/// Create a preset profile (skyblock, pvp)
#[tauri::command]
pub async fn create_preset_profile(
    app: AppHandle,
    minecraft_version: String,
    preset_type: String,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.create_preset(&minecraft_version, &preset_type)
}

/// Delete a profile
#[tauri::command]
pub async fn delete_profile(app: AppHandle, profile_id: String) -> Result<(), String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.delete_profile(&profile_id)
}

/// Duplicate a profile
#[tauri::command]
pub async fn duplicate_profile(
    app: AppHandle,
    profile_id: String,
    new_name: String,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    manager.duplicate_profile(&profile_id, &new_name)
}

/// Export a profile to shareable format
#[tauri::command]
pub async fn export_profile(app: AppHandle, profile_id: String) -> Result<ProfileExport, String> {
    let state = app.state::<AppState>();
    let manager = state.profile_manager.lock().unwrap();
    manager.export_profile(&profile_id)
}

/// Import a profile from exported format
#[tauri::command]
pub async fn import_profile(
    app: AppHandle,
    name: String,
    minecraft_version: String,
    mods: Vec<String>,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();
    let mut manager = state.profile_manager.lock().unwrap();
    let export = ProfileExport {
        name,
        version: minecraft_version.clone(),
        mods,
    };
    manager.import_profile(export, &minecraft_version)
}

/// Get the mods directory for a profile
#[tauri::command]
pub async fn get_profile_mods_dir(
    app: AppHandle,
    minecraft_version: String,
    profile_id: String,
) -> Result<String, String> {
    let state = app.state::<AppState>();
    let manager = state.profile_manager.lock().unwrap();
    let path = manager.get_mods_dir(&minecraft_version, &profile_id);
    Ok(path.to_string_lossy().to_string())
}

/// Get the list of performance mods that are auto-included
#[tauri::command]
pub async fn get_performance_mods() -> Result<Vec<String>, String> {
    Ok(PERFORMANCE_MODS.iter().map(|s| s.to_string()).collect())
}

// ==================== Profile Sharing Commands ====================

use crate::supabase::{ShareProfileResult, SharedProfile};

/// Share a profile to Supabase, get a short code
#[tauri::command]
pub async fn share_profile_online(
    app: AppHandle,
    profile_id: String,
    creator_uuid: Option<String>,
    creator_username: Option<String>,
) -> Result<ShareProfileResult, String> {
    let state = app.state::<AppState>();

    // Get the profile from local storage (extract data before releasing lock)
    let (name, version, mods) = {
        let manager = state.profile_manager.lock().unwrap();
        let export = manager.export_profile(&profile_id)?;
        (export.name, export.version, export.mods)
    };

    // Share to Supabase
    state
        .supabase
        .share_profile(
            &name,
            &version,
            &mods,
            creator_uuid.as_deref(),
            creator_username.as_deref(),
        )
        .await
}

/// Get a shared profile by short code
#[tauri::command]
pub async fn get_shared_profile(
    app: AppHandle,
    short_code: String,
) -> Result<Option<SharedProfile>, String> {
    let state = app.state::<AppState>();
    state.supabase.get_shared_profile(&short_code).await
}

/// Import a shared profile from Supabase by short code
#[tauri::command]
pub async fn import_shared_profile(
    app: AppHandle,
    short_code: String,
    target_version: String,
) -> Result<Profile, String> {
    let state = app.state::<AppState>();

    // Get the shared profile from Supabase
    let shared = state
        .supabase
        .get_shared_profile(&short_code)
        .await?
        .ok_or_else(|| format!("Profile with code '{}' not found", short_code))?;

    // Import into local profiles
    let mut manager = state.profile_manager.lock().unwrap();
    let export = crate::profiles::ProfileExport {
        name: format!("{} (Shared)", shared.name),
        version: target_version.clone(),
        mods: shared.mods,
    };

    manager.import_profile(export, &target_version)
}
