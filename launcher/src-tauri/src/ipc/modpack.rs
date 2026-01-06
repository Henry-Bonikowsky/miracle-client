use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::io::Read;
use std::path::PathBuf;
use tokio::io::AsyncWriteExt;
use zip::ZipArchive;

use crate::profiles::ProfileManager;

const MODRINTH_API_BASE: &str = "https://api.modrinth.com/v2";

/// Modrinth .mrpack index structure
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MrpackIndex {
    #[serde(rename = "formatVersion")]
    pub format_version: i32,
    pub game: String,
    #[serde(rename = "versionId")]
    pub version_id: String,
    pub name: String,
    pub summary: Option<String>,
    pub files: Vec<MrpackFile>,
    pub dependencies: HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MrpackFile {
    pub path: String,
    pub hashes: MrpackHashes,
    pub downloads: Vec<String>,
    #[serde(rename = "fileSize")]
    pub file_size: i64,
    pub env: Option<MrpackEnv>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MrpackHashes {
    pub sha1: String,
    pub sha512: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MrpackEnv {
    pub client: String,
    pub server: String,
}

/// CurseForge modpack manifest structure
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeManifest {
    pub minecraft: CurseForgeMinecraft,
    #[serde(rename = "manifestType")]
    pub manifest_type: String,
    #[serde(rename = "manifestVersion")]
    pub manifest_version: i32,
    pub name: String,
    pub version: String,
    pub author: String,
    pub files: Vec<CurseForgeFileRef>,
    pub overrides: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeMinecraft {
    pub version: String,
    #[serde(rename = "modLoaders")]
    pub mod_loaders: Vec<CurseForgeModLoader>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeModLoader {
    pub id: String,
    pub primary: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeFileRef {
    #[serde(rename = "projectID")]
    pub project_id: i64,
    #[serde(rename = "fileID")]
    pub file_id: i64,
    pub required: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModpackInstallProgress {
    pub stage: String,
    pub current: u32,
    pub total: u32,
    pub message: String,
}

fn create_client() -> Result<reqwest::Client, String> {
    reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(120))
        .user_agent("MiracleClient/1.0 (https://github.com/miracle-client)")
        .build()
        .map_err(|e| e.to_string())
}

/// Get version info for a Modrinth modpack
async fn get_modrinth_modpack_version(
    project_slug: &str,
    game_version: &str,
) -> Result<(String, String), String> {
    let client = create_client()?;

    let url = format!(
        "{}/project/{}/version?game_versions=[\"{}\"]&loaders=[\"fabric\"]",
        MODRINTH_API_BASE, project_slug, game_version
    );

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to get modpack versions: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Failed to get modpack versions: HTTP {}",
            response.status()
        ));
    }

    #[derive(Deserialize)]
    struct VersionInfo {
        id: String,
        files: Vec<FileInfo>,
    }

    #[derive(Deserialize)]
    struct FileInfo {
        url: String,
        filename: String,
        primary: bool,
    }

    let versions: Vec<VersionInfo> = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse versions: {}", e))?;

    let version = versions
        .first()
        .ok_or_else(|| format!("No compatible version found for {}", game_version))?;

    let file = version
        .files
        .iter()
        .find(|f| f.primary)
        .or_else(|| version.files.first())
        .ok_or("No download file found")?;

    Ok((version.id.clone(), file.url.clone()))
}

/// Download and extract a Modrinth .mrpack file
async fn download_mrpack(url: &str) -> Result<(MrpackIndex, Vec<u8>), String> {
    let client = create_client()?;

    tracing::info!("Downloading modpack from: {}", url);

    let response = client
        .get(url)
        .send()
        .await
        .map_err(|e| format!("Failed to download modpack: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Failed to download modpack: HTTP {}",
            response.status()
        ));
    }

    let bytes = response
        .bytes()
        .await
        .map_err(|e| format!("Failed to read modpack: {}", e))?;

    // Parse the mrpack (it's a zip file)
    let cursor = std::io::Cursor::new(&bytes);
    let mut archive =
        ZipArchive::new(cursor).map_err(|e| format!("Failed to open modpack archive: {}", e))?;

    // Read modrinth.index.json
    let mut index_file = archive
        .by_name("modrinth.index.json")
        .map_err(|e| format!("Failed to find modrinth.index.json: {}", e))?;

    let mut index_json = String::new();
    index_file
        .read_to_string(&mut index_json)
        .map_err(|e| format!("Failed to read index: {}", e))?;

    let index: MrpackIndex =
        serde_json::from_str(&index_json).map_err(|e| format!("Failed to parse index: {}", e))?;

    Ok((index, bytes.to_vec()))
}

/// Install a modpack from Modrinth or CurseForge
#[tauri::command]
pub async fn install_modpack(
    project_slug: String,
    source: String,
    game_version: String,
) -> Result<String, String> {
    match source.as_str() {
        "modrinth" => install_modrinth_modpack(&project_slug, &game_version).await,
        "curseforge" => {
            // project_slug is actually the project ID for CurseForge
            let project_id: i64 = project_slug
                .parse()
                .map_err(|_| "Invalid CurseForge project ID".to_string())?;
            install_curseforge_modpack_online(project_id, &game_version).await
        }
        _ => Err(format!("Unknown source: {}", source)),
    }
}

/// Install a CurseForge modpack from online (by project ID)
async fn install_curseforge_modpack_online(
    project_id: i64,
    game_version: &str,
) -> Result<String, String> {
    use super::curseforge;

    tracing::info!(
        "Installing CurseForge modpack: {} for MC {}",
        project_id,
        game_version
    );

    // Get the modpack files
    let files = curseforge::get_mod_files(project_id as i32, game_version).await?;

    if files.is_empty() {
        return Err(format!(
            "No compatible version found for MC {}",
            game_version
        ));
    }

    let latest = files.first().ok_or("No files found")?;
    let download_url = latest
        .download_url
        .as_ref()
        .ok_or("No download URL - this modpack may require manual download")?;

    // Download the modpack zip
    let zip_bytes = curseforge::download_file_bytes(download_url).await?;

    // Install from the zip bytes
    install_curseforge_modpack_from_bytes(&zip_bytes).await
}

/// Install a CurseForge modpack from raw zip bytes (manifest.json format)
async fn install_curseforge_modpack_from_bytes(zip_bytes: &[u8]) -> Result<String, String> {
    use super::curseforge;

    // Parse manifest synchronously to avoid holding ZipArchive across await
    let manifest: CurseForgeManifest = {
        let cursor = std::io::Cursor::new(zip_bytes);
        let mut archive = ZipArchive::new(cursor)
            .map_err(|e| format!("Failed to open modpack archive: {}", e))?;

        let mut manifest_file = archive
            .by_name("manifest.json")
            .map_err(|e| format!("Failed to find manifest.json: {}", e))?;

        let mut manifest_json = String::new();
        manifest_file
            .read_to_string(&mut manifest_json)
            .map_err(|e| format!("Failed to read manifest: {}", e))?;

        serde_json::from_str(&manifest_json)
            .map_err(|e| format!("Failed to parse manifest: {}", e))?
    };

    tracing::info!(
        "Installing CurseForge modpack: {} v{}",
        manifest.name,
        manifest.version
    );

    // Check if this is a Fabric modpack
    let is_fabric = manifest
        .minecraft
        .mod_loaders
        .iter()
        .any(|loader| loader.id.to_lowercase().contains("fabric"));

    if !is_fabric {
        tracing::warn!("This modpack uses Forge/other loader. Only Fabric mods will be installed.");
    }

    let game_version = &manifest.minecraft.version;

    // Create a new profile for this modpack
    let mut profile_manager = ProfileManager::new();
    let profile =
        profile_manager.create_modpack_profile(&manifest.name, game_version, "curseforge")?;

    let profile_id = profile.id.clone();
    let mods_dir = profile_manager.get_mods_dir(game_version, &profile_id);

    // Create mods directory
    tokio::fs::create_dir_all(&mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    // Download all mods
    let total_files = manifest.files.len();
    let mut downloaded = 0;
    let mut skipped = 0;
    let mut failed: Vec<String> = Vec::new();

    for (i, file_ref) in manifest.files.iter().enumerate() {
        tracing::info!(
            "Processing mod {}/{}: project={} file={}",
            i + 1,
            total_files,
            file_ref.project_id,
            file_ref.file_id
        );

        // Get file info from CurseForge API
        match curseforge::get_file_by_id(file_ref.project_id, file_ref.file_id).await {
            Ok(file_info) => {
                // Check if it's a Fabric mod (mod_loader type 4)
                if let Some(loader) = file_info.mod_loader {
                    if loader != 4 {
                        tracing::info!("Skipping non-Fabric mod: {}", file_info.file_name);
                        skipped += 1;
                        continue;
                    }
                }

                let dest_path = mods_dir.join(&file_info.file_name);
                if dest_path.exists() {
                    tracing::info!("Mod already exists: {}", file_info.file_name);
                    downloaded += 1;
                    continue;
                }

                // Download the mod
                if let Some(url) = &file_info.download_url {
                    match curseforge::download_file_bytes(url).await {
                        Ok(bytes) => {
                            if bytes.len() > 1000 {
                                tokio::fs::write(&dest_path, &bytes).await.ok();
                                downloaded += 1;
                                tracing::info!("Downloaded: {}", file_info.file_name);
                            } else {
                                failed.push(file_info.file_name.clone());
                            }
                        }
                        Err(e) => {
                            tracing::warn!("Failed to download {}: {}", file_info.file_name, e);
                            failed.push(file_info.file_name.clone());
                        }
                    }
                } else {
                    tracing::warn!(
                        "No download URL for: {} (manual download required)",
                        file_info.file_name
                    );
                    failed.push(format!("{} (no API download)", file_info.file_name));
                }
            }
            Err(e) => {
                tracing::warn!(
                    "Failed to get file info for project={} file={}: {}",
                    file_ref.project_id,
                    file_ref.file_id,
                    e
                );
                failed.push(format!(
                    "project:{}/file:{}",
                    file_ref.project_id, file_ref.file_id
                ));
            }
        }

        // Small delay to avoid rate limiting
        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
    }

    // Extract overrides from the zip
    let overrides_folder = &manifest.overrides;
    let overrides: Vec<(PathBuf, Vec<u8>)> = {
        let cursor = std::io::Cursor::new(zip_bytes);
        let mut archive =
            ZipArchive::new(cursor).map_err(|e| format!("Failed to reopen archive: {}", e))?;

        let game_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient");

        let mut files_to_write = Vec::new();
        let prefix = format!("{}/", overrides_folder);

        for i in 0..archive.len() {
            let mut file = match archive.by_index(i) {
                Ok(f) => f,
                Err(_) => continue,
            };

            let name = file.name().to_string();

            if name.starts_with(&prefix) {
                let relative_path = name.strip_prefix(&prefix).unwrap_or(&name);
                if relative_path.is_empty() || name.ends_with('/') {
                    continue;
                }

                // Put mods in the profile-specific directory
                let dest_path = if relative_path.starts_with("mods/") {
                    mods_dir.join(relative_path.strip_prefix("mods/").unwrap_or(relative_path))
                } else {
                    game_dir.join(relative_path)
                };

                let mut contents = Vec::new();
                file.read_to_end(&mut contents).ok();

                if !contents.is_empty() {
                    files_to_write.push((dest_path, contents));
                }
            }
        }

        files_to_write
    };

    // Write override files
    for (dest_path, contents) in overrides {
        if let Some(parent) = dest_path.parent() {
            tokio::fs::create_dir_all(parent).await.ok();
        }
        tokio::fs::write(&dest_path, &contents).await.ok();
    }

    // Set this profile as active
    profile_manager.set_active_profile(game_version, &profile_id)?;

    let result_msg = format!(
        "Modpack '{}' installed: {} downloaded, {} skipped (non-Fabric), {} failed",
        manifest.name,
        downloaded,
        skipped,
        failed.len()
    );

    if !failed.is_empty() {
        tracing::warn!("Failed mods: {:?}", failed);
    }

    tracing::info!("{}", result_msg);
    Ok(profile_id)
}

async fn install_modrinth_modpack(
    project_slug: &str,
    game_version: &str,
) -> Result<String, String> {
    tracing::info!(
        "Installing Modrinth modpack: {} for MC {}",
        project_slug,
        game_version
    );

    // Get the modpack version and download URL
    let (_version_id, download_url) =
        get_modrinth_modpack_version(project_slug, game_version).await?;

    // Download and parse the mrpack
    let (index, mrpack_bytes) = download_mrpack(&download_url).await?;

    // Create a new profile for this modpack
    let mut profile_manager = ProfileManager::new();
    let profile = profile_manager.create_modpack_profile(&index.name, game_version, "modrinth")?;

    let profile_id = profile.id.clone();
    let mods_dir = profile_manager.get_mods_dir(game_version, &profile_id);

    // Create mods directory
    tokio::fs::create_dir_all(&mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    let client = create_client()?;

    // Download all mod files
    let total_files = index.files.len();
    for (i, file) in index.files.iter().enumerate() {
        // Check if this is a client-side file
        if let Some(env) = &file.env {
            if env.client == "unsupported" {
                continue;
            }
        }

        // Only process mods folder files for now
        if !file.path.starts_with("mods/") {
            continue;
        }

        let filename = file.path.strip_prefix("mods/").unwrap_or(&file.path);

        let dest_path = mods_dir.join(filename);

        // Skip if already exists
        if dest_path.exists() {
            continue;
        }

        tracing::info!("Downloading mod {}/{}: {}", i + 1, total_files, filename);

        // Try each download URL
        let mut downloaded = false;
        for url in &file.downloads {
            match client.get(url).send().await {
                Ok(response) if response.status().is_success() => {
                    if let Ok(bytes) = response.bytes().await {
                        if tokio::fs::write(&dest_path, &bytes).await.is_ok() {
                            downloaded = true;
                            break;
                        }
                    }
                }
                _ => continue,
            }
        }

        if !downloaded {
            tracing::warn!("Failed to download: {}", filename);
        }
    }

    // Extract overrides from the mrpack (config files, etc.)
    // Read all override files synchronously first (ZipFile is not Send)
    let overrides: Vec<(PathBuf, Vec<u8>)> = {
        let cursor = std::io::Cursor::new(&mrpack_bytes);
        let mut archive =
            ZipArchive::new(cursor).map_err(|e| format!("Failed to reopen modpack: {}", e))?;

        let game_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient");

        let mut files_to_write = Vec::new();

        for i in 0..archive.len() {
            let mut file = archive
                .by_index(i)
                .map_err(|e| format!("Failed to read archive entry: {}", e))?;

            let name = file.name().to_string();

            // Handle overrides folder
            if name.starts_with("overrides/") {
                let relative_path = name.strip_prefix("overrides/").unwrap_or(&name);
                if relative_path.is_empty() || name.ends_with('/') {
                    continue;
                }

                let dest_path = game_dir.join(relative_path);

                let mut contents = Vec::new();
                file.read_to_end(&mut contents).ok();

                if !contents.is_empty() {
                    files_to_write.push((dest_path, contents));
                }
            }
        }

        files_to_write
    };

    // Now write files asynchronously
    for (dest_path, contents) in overrides {
        if let Some(parent) = dest_path.parent() {
            tokio::fs::create_dir_all(parent).await.ok();
        }

        let mut dest_file = tokio::fs::File::create(&dest_path)
            .await
            .map_err(|e| format!("Failed to create override file: {}", e))?;
        dest_file
            .write_all(&contents)
            .await
            .map_err(|e| format!("Failed to write override file: {}", e))?;
    }

    // Set this profile as active
    profile_manager.set_active_profile(game_version, &profile_id)?;

    tracing::info!(
        "Modpack '{}' installed successfully with profile ID: {}",
        index.name,
        profile_id
    );

    Ok(profile_id)
}

/// Get information about a modpack before installing
#[tauri::command]
pub async fn get_modpack_info(
    project_slug: String,
    source: String,
    game_version: String,
) -> Result<ModpackInfo, String> {
    match source.as_str() {
        "modrinth" => {
            let client = create_client()?;

            // Get project info
            let url = format!("{}/project/{}", MODRINTH_API_BASE, project_slug);
            let response = client
                .get(&url)
                .send()
                .await
                .map_err(|e| format!("Failed to get project info: {}", e))?;

            if !response.status().is_success() {
                return Err(format!(
                    "Failed to get project info: HTTP {}",
                    response.status()
                ));
            }

            #[derive(Deserialize)]
            struct ProjectInfo {
                title: String,
                description: String,
                icon_url: Option<String>,
                downloads: i64,
            }

            let project: ProjectInfo = response
                .json()
                .await
                .map_err(|e| format!("Failed to parse project: {}", e))?;

            // Get version info to count mods
            let (_version_id, download_url) =
                get_modrinth_modpack_version(&project_slug, &game_version).await?;

            let (index, _) = download_mrpack(&download_url).await?;

            let mod_count = index
                .files
                .iter()
                .filter(|f| f.path.starts_with("mods/"))
                .count();

            Ok(ModpackInfo {
                name: project.title,
                description: project.description,
                icon_url: project.icon_url,
                mod_count: mod_count as u32,
                minecraft_version: game_version,
                source,
            })
        }
        _ => Err("Source not supported".to_string()),
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModpackInfo {
    pub name: String,
    pub description: String,
    pub icon_url: Option<String>,
    pub mod_count: u32,
    pub minecraft_version: String,
    pub source: String,
}

// ============================================================================
// MultiMC/Prism Launcher Import
// ============================================================================

/// MultiMC mmc-pack.json structure
#[derive(Debug, Clone, Serialize, Deserialize)]
struct MmcPack {
    components: Vec<MmcComponent>,
    #[serde(rename = "formatVersion")]
    format_version: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct MmcComponent {
    #[serde(default)]
    uid: String,
    #[serde(default)]
    version: String,
    #[serde(default)]
    important: bool,
}

/// Parse instance.cfg (INI-like format)
fn parse_instance_cfg(content: &str) -> HashMap<String, String> {
    let mut map = HashMap::new();
    for line in content.lines() {
        if let Some((key, value)) = line.split_once('=') {
            map.insert(key.trim().to_string(), value.trim().to_string());
        }
    }
    map
}

/// Install a Modrinth App profile folder (copies mods directly)
async fn install_modrinth_profile_folder(profile_path: &std::path::Path) -> Result<String, String> {
    tracing::info!("Importing Modrinth profile from: {:?}", profile_path);

    let mods_dir = profile_path.join("mods");
    if !mods_dir.exists() {
        return Err("No mods folder found in profile".to_string());
    }

    // Get profile name from folder
    let profile_name = profile_path
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "Imported Modrinth Profile".to_string());

    // Try to detect Minecraft version from mod filenames
    let mut game_version = "1.21.4".to_string(); // Default
    let mut version_counts: std::collections::HashMap<String, u32> =
        std::collections::HashMap::new();

    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let name = entry.file_name().to_string_lossy().to_string();
            if name.ends_with(".jar") {
                // Look for patterns like "+1.21.4", "-1.21.1", "mc1.21", "MC1.21.4", "for-MC1.21"
                if let Some(version) = extract_mc_version_from_mod(&name) {
                    *version_counts.entry(version).or_insert(0) += 1;
                }
            }
        }
    }

    // Use the most common version found
    if let Some((version, _)) = version_counts.iter().max_by_key(|(_, count)| *count) {
        game_version = version.clone();
    }

    // Create profile
    let mut profile_manager = crate::profiles::ProfileManager::new();
    let profile =
        profile_manager.create_modpack_profile(&profile_name, &game_version, "modrinth")?;
    let profile_id = profile.id.clone();

    // Get destination mods directory (uses sanitized profile name, not UUID)
    let dest_mods_dir = profile_manager.get_mods_dir(&game_version, &profile_id);
    tokio::fs::create_dir_all(&dest_mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    // Copy all .jar files from source mods folder (case-insensitive, handles .jar.disabled)
    let mut copied = 0;
    let mut skipped = 0;
    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let filename = path
                .file_name()
                .unwrap_or_default()
                .to_string_lossy()
                .to_lowercase();

            // Check for .jar (case-insensitive) or .jar.disabled
            let is_jar = filename.ends_with(".jar");
            let is_disabled = filename.ends_with(".jar.disabled");

            if is_jar || is_disabled {
                // For disabled mods, rename to .jar on import
                let dest_filename = if is_disabled {
                    path.file_name()
                        .unwrap()
                        .to_string_lossy()
                        .replace(".jar.disabled", ".jar")
                } else {
                    path.file_name().unwrap().to_string_lossy().to_string()
                };
                let dest_path = dest_mods_dir.join(&dest_filename);

                if let Err(e) = tokio::fs::copy(&path, &dest_path).await {
                    tracing::warn!("Failed to copy mod {:?}: {}", dest_filename, e);
                    skipped += 1;
                } else {
                    if is_disabled {
                        tracing::info!("Copied and enabled: {}", dest_filename);
                    }
                    copied += 1;
                }
            } else if path.is_file() {
                tracing::info!("Skipped non-jar file: {}", filename);
                skipped += 1;
            }
        }
    }

    tracing::info!(
        "Copied {} mods from Modrinth profile ({} skipped)",
        copied,
        skipped
    );
    Ok(profile_id)
}

/// Install a MultiMC/Prism Launcher instance from a folder path
async fn install_multimc_instance(instance_path: &std::path::Path) -> Result<String, String> {
    tracing::info!("Importing MultiMC/Prism instance from: {:?}", instance_path);

    // Read instance.cfg for the name
    let instance_cfg_path = instance_path.join("instance.cfg");
    let instance_name = if instance_cfg_path.exists() {
        let content = tokio::fs::read_to_string(&instance_cfg_path)
            .await
            .map_err(|e| format!("Failed to read instance.cfg: {}", e))?;
        let config = parse_instance_cfg(&content);
        config
            .get("name")
            .cloned()
            .unwrap_or_else(|| "Imported Instance".to_string())
    } else {
        "Imported Instance".to_string()
    };

    // Read mmc-pack.json for version info
    let mmc_pack_path = instance_path.join("mmc-pack.json");
    let (minecraft_version, _fabric_version) = if mmc_pack_path.exists() {
        let content = tokio::fs::read_to_string(&mmc_pack_path)
            .await
            .map_err(|e| format!("Failed to read mmc-pack.json: {}", e))?;
        let pack: MmcPack = serde_json::from_str(&content)
            .map_err(|e| format!("Failed to parse mmc-pack.json: {}", e))?;

        let mc_version = pack
            .components
            .iter()
            .find(|c| c.uid == "net.minecraft")
            .map(|c| c.version.clone())
            .unwrap_or_else(|| "1.21.4".to_string());

        let fabric_version = pack
            .components
            .iter()
            .find(|c| c.uid == "net.fabricmc.fabric-loader")
            .map(|c| c.version.clone());

        (mc_version, fabric_version)
    } else {
        return Err("Not a valid MultiMC/Prism instance (missing mmc-pack.json)".to_string());
    };

    // Find the .minecraft folder (could be .minecraft or minecraft or just the instance root)
    let minecraft_dir = if instance_path.join(".minecraft").exists() {
        instance_path.join(".minecraft")
    } else if instance_path.join("minecraft").exists() {
        instance_path.join("minecraft")
    } else {
        instance_path.to_path_buf()
    };

    let source_mods_dir = minecraft_dir.join("mods");
    if !source_mods_dir.exists() {
        return Err("No mods folder found in instance".to_string());
    }

    // Create a new profile
    let mut profile_manager = ProfileManager::new();
    let profile =
        profile_manager.create_modpack_profile(&instance_name, &minecraft_version, "multimc")?;

    let profile_id = profile.id.clone();
    let dest_mods_dir = profile_manager.get_mods_dir(&minecraft_version, &profile_id);

    tokio::fs::create_dir_all(&dest_mods_dir)
        .await
        .map_err(|e| format!("Failed to create mods directory: {}", e))?;

    // Copy all .jar files from source mods to destination (case-insensitive, handles .jar.disabled)
    let mut copied = 0;
    let mut skipped = 0;
    let mut entries = tokio::fs::read_dir(&source_mods_dir)
        .await
        .map_err(|e| format!("Failed to read mods directory: {}", e))?;

    while let Some(entry) = entries.next_entry().await.map_err(|e| e.to_string())? {
        let path = entry.path();
        let filename = path
            .file_name()
            .unwrap_or_default()
            .to_string_lossy()
            .to_lowercase();

        // Check for .jar (case-insensitive) or .jar.disabled
        let is_jar = filename.ends_with(".jar");
        let is_disabled = filename.ends_with(".jar.disabled");

        if is_jar || is_disabled {
            // For disabled mods, rename to .jar on import
            let dest_filename = if is_disabled {
                path.file_name()
                    .unwrap()
                    .to_string_lossy()
                    .replace(".jar.disabled", ".jar")
            } else {
                path.file_name().unwrap().to_string_lossy().to_string()
            };
            let dest_path = dest_mods_dir.join(&dest_filename);

            match tokio::fs::copy(&path, &dest_path).await {
                Ok(_) => {
                    copied += 1;
                    if is_disabled {
                        tracing::info!("Copied and enabled: {}", dest_filename);
                    } else {
                        tracing::info!("Copied: {}", dest_filename);
                    }
                }
                Err(e) => {
                    tracing::warn!("Failed to copy {}: {}", dest_filename, e);
                    skipped += 1;
                }
            }
        } else if path.is_file() {
            tracing::info!("Skipped non-jar file: {}", filename);
        }
    }

    // Copy config folder if it exists
    let source_config = minecraft_dir.join("config");
    if source_config.exists() {
        let game_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient")
            .join("config");

        copy_dir_recursive(&source_config, &game_dir).await.ok();
        tracing::info!("Copied config folder");
    }

    // Set as active profile
    profile_manager.set_active_profile(&minecraft_version, &profile_id)?;

    tracing::info!(
        "MultiMC instance '{}' imported: {} mods copied, {} skipped",
        instance_name,
        copied,
        skipped
    );

    Ok(profile_id)
}

/// Recursively copy a directory
async fn copy_dir_recursive(src: &std::path::Path, dst: &std::path::Path) -> Result<(), String> {
    tokio::fs::create_dir_all(dst)
        .await
        .map_err(|e| e.to_string())?;

    let mut entries = tokio::fs::read_dir(src).await.map_err(|e| e.to_string())?;
    while let Some(entry) = entries.next_entry().await.map_err(|e| e.to_string())? {
        let path = entry.path();
        let dest_path = dst.join(entry.file_name());

        if path.is_dir() {
            Box::pin(copy_dir_recursive(&path, &dest_path)).await?;
        } else {
            tokio::fs::copy(&path, &dest_path)
                .await
                .map_err(|e| e.to_string())?;
        }
    }

    Ok(())
}

// ============================================================================
// File-Based Import Commands
// ============================================================================

/// Preview info returned before importing
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModpackPreview {
    pub name: String,
    pub minecraft_version: String,
    pub mod_count: u32,
    pub format: String,         // "modrinth", "curseforge", "multimc"
    pub loader: Option<String>, // "fabric", "forge", etc.
    pub warnings: Vec<String>,
}

/// Result of importing a modpack
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModpackImportResult {
    pub profile_id: String,
    pub name: String,
    pub mods_installed: u32,
    pub mods_failed: u32,
    pub warnings: Vec<String>,
}

/// Preview a modpack file before importing
#[tauri::command]
pub async fn preview_modpack_file(file_path: String) -> Result<ModpackPreview, String> {
    let path = std::path::Path::new(&file_path);

    if !path.exists() {
        return Err("File or folder not found".to_string());
    }

    // Check if it's a directory (MultiMC instance)
    if path.is_dir() {
        return preview_multimc_instance(path).await;
    }

    // Read the file
    let bytes = tokio::fs::read(path)
        .await
        .map_err(|e| format!("Failed to read file: {}", e))?;

    // Try to open as zip
    let cursor = std::io::Cursor::new(&bytes);
    let mut archive =
        ZipArchive::new(cursor).map_err(|e| format!("Not a valid zip/modpack file: {}", e))?;

    // Check for modrinth.index.json (Modrinth format)
    if archive.by_name("modrinth.index.json").is_ok() {
        return preview_mrpack(&bytes).await;
    }

    // Check for manifest.json (CurseForge format)
    if archive.by_name("manifest.json").is_ok() {
        return preview_curseforge_zip(&bytes).await;
    }

    // Check for mmc-pack.json (MultiMC exported zip)
    if archive.by_name("mmc-pack.json").is_ok() {
        return preview_multimc_zip(&bytes).await;
    }

    Err("Unrecognized modpack format. Expected .mrpack (Modrinth), CurseForge zip, or MultiMC instance.".to_string())
}

async fn preview_mrpack(bytes: &[u8]) -> Result<ModpackPreview, String> {
    let cursor = std::io::Cursor::new(bytes);
    let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

    let mut index_file = archive
        .by_name("modrinth.index.json")
        .map_err(|e| e.to_string())?;
    let mut json = String::new();
    index_file
        .read_to_string(&mut json)
        .map_err(|e| e.to_string())?;

    let index: MrpackIndex = serde_json::from_str(&json).map_err(|e| e.to_string())?;

    let minecraft_version = index
        .dependencies
        .get("minecraft")
        .cloned()
        .unwrap_or_else(|| "unknown".to_string());

    let loader = if index.dependencies.contains_key("fabric-loader") {
        Some("fabric".to_string())
    } else if index.dependencies.contains_key("forge") {
        Some("forge".to_string())
    } else if index.dependencies.contains_key("quilt-loader") {
        Some("quilt".to_string())
    } else {
        None
    };

    let mod_count = index
        .files
        .iter()
        .filter(|f| f.path.starts_with("mods/"))
        .count() as u32;

    let mut warnings = Vec::new();
    if loader.as_deref() != Some("fabric") {
        warnings.push(format!(
            "This modpack uses {:?} loader. Only Fabric mods are supported.",
            loader
        ));
    }

    Ok(ModpackPreview {
        name: index.name,
        minecraft_version,
        mod_count,
        format: "modrinth".to_string(),
        loader,
        warnings,
    })
}

async fn preview_curseforge_zip(bytes: &[u8]) -> Result<ModpackPreview, String> {
    let cursor = std::io::Cursor::new(bytes);
    let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

    let mut manifest_file = archive
        .by_name("manifest.json")
        .map_err(|e| e.to_string())?;
    let mut json = String::new();
    manifest_file
        .read_to_string(&mut json)
        .map_err(|e| e.to_string())?;

    let manifest: CurseForgeManifest = serde_json::from_str(&json).map_err(|e| e.to_string())?;

    let loader = manifest.minecraft.mod_loaders.first().map(|l| l.id.clone());

    let is_fabric = loader
        .as_ref()
        .map(|l| l.to_lowercase().contains("fabric"))
        .unwrap_or(false);

    let mut warnings = Vec::new();
    if !is_fabric {
        warnings.push(format!(
            "This modpack uses {:?} loader. Only Fabric mods will be installed.",
            loader
        ));
    }

    Ok(ModpackPreview {
        name: manifest.name,
        minecraft_version: manifest.minecraft.version,
        mod_count: manifest.files.len() as u32,
        format: "curseforge".to_string(),
        loader,
        warnings,
    })
}

async fn preview_multimc_zip(bytes: &[u8]) -> Result<ModpackPreview, String> {
    let cursor = std::io::Cursor::new(bytes);
    let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

    // Read mmc-pack.json
    let mut pack_file = archive
        .by_name("mmc-pack.json")
        .map_err(|e| e.to_string())?;
    let mut json = String::new();
    pack_file
        .read_to_string(&mut json)
        .map_err(|e| e.to_string())?;
    drop(pack_file);

    let pack: MmcPack = serde_json::from_str(&json).map_err(|e| e.to_string())?;

    let minecraft_version = pack
        .components
        .iter()
        .find(|c| c.uid == "net.minecraft")
        .map(|c| c.version.clone())
        .unwrap_or_else(|| "unknown".to_string());

    let has_fabric = pack
        .components
        .iter()
        .any(|c| c.uid == "net.fabricmc.fabric-loader");
    let has_forge = pack.components.iter().any(|c| c.uid.contains("forge"));

    let loader = if has_fabric {
        Some("fabric".to_string())
    } else if has_forge {
        Some("forge".to_string())
    } else {
        None
    };

    // Try to read instance.cfg for the name
    let name = if let Ok(mut cfg_file) = archive.by_name("instance.cfg") {
        let mut cfg = String::new();
        cfg_file.read_to_string(&mut cfg).ok();
        parse_instance_cfg(&cfg).get("name").cloned()
    } else {
        None
    }
    .unwrap_or_else(|| "MultiMC Instance".to_string());

    // Count mods in .minecraft/mods/
    let mut mod_count = 0u32;
    for i in 0..archive.len() {
        if let Ok(f) = archive.by_index(i) {
            let name = f.name();
            if (name.contains("/mods/") || name.contains("\\mods\\")) && name.ends_with(".jar") {
                mod_count += 1;
            }
        }
    }

    let mut warnings = Vec::new();
    if !has_fabric {
        warnings.push("This instance may not use Fabric. Some mods may not work.".to_string());
    }

    Ok(ModpackPreview {
        name,
        minecraft_version,
        mod_count,
        format: "multimc".to_string(),
        loader,
        warnings,
    })
}

async fn preview_multimc_instance(path: &std::path::Path) -> Result<ModpackPreview, String> {
    // Read mmc-pack.json
    let mmc_pack_path = path.join("mmc-pack.json");
    if !mmc_pack_path.exists() {
        return Err("Not a valid MultiMC/Prism instance (missing mmc-pack.json)".to_string());
    }

    let content = tokio::fs::read_to_string(&mmc_pack_path)
        .await
        .map_err(|e| e.to_string())?;
    let pack: MmcPack = serde_json::from_str(&content).map_err(|e| e.to_string())?;

    let minecraft_version = pack
        .components
        .iter()
        .find(|c| c.uid == "net.minecraft")
        .map(|c| c.version.clone())
        .unwrap_or_else(|| "unknown".to_string());

    let has_fabric = pack
        .components
        .iter()
        .any(|c| c.uid == "net.fabricmc.fabric-loader");

    let loader = if has_fabric {
        Some("fabric".to_string())
    } else {
        None
    };

    // Read instance name
    let instance_cfg_path = path.join("instance.cfg");
    let name = if instance_cfg_path.exists() {
        let content = tokio::fs::read_to_string(&instance_cfg_path).await.ok();
        content.and_then(|c| parse_instance_cfg(&c).get("name").cloned())
    } else {
        None
    }
    .unwrap_or_else(|| "MultiMC Instance".to_string());

    // Find mods folder and count
    let minecraft_dir = if path.join(".minecraft").exists() {
        path.join(".minecraft")
    } else if path.join("minecraft").exists() {
        path.join("minecraft")
    } else {
        path.to_path_buf()
    };

    let mods_dir = minecraft_dir.join("mods");
    let mod_count = if mods_dir.exists() {
        let mut count = 0u32;
        if let Ok(mut entries) = tokio::fs::read_dir(&mods_dir).await {
            while let Ok(Some(entry)) = entries.next_entry().await {
                if entry.path().extension().map_or(false, |e| e == "jar") {
                    count += 1;
                }
            }
        }
        count
    } else {
        0
    };

    let mut warnings = Vec::new();
    if !has_fabric {
        warnings.push("This instance may not use Fabric. Some mods may not work.".to_string());
    }

    Ok(ModpackPreview {
        name,
        minecraft_version,
        mod_count,
        format: "multimc".to_string(),
        loader,
        warnings,
    })
}

/// Import a modpack from a file path
#[tauri::command]
pub async fn import_modpack_file(file_path: String) -> Result<ModpackImportResult, String> {
    let path = std::path::Path::new(&file_path);

    if !path.exists() {
        return Err("File or folder not found".to_string());
    }

    // Check if it's a directory
    if path.is_dir() {
        // Check what type of instance folder this is
        let mmc_pack = path.join("mmc-pack.json");
        let instance_cfg = path.join("instance.cfg");
        let mods_dir = path.join("mods");
        let minecraft_dir = path.join(".minecraft").join("mods");

        if mmc_pack.exists() || instance_cfg.exists() {
            // MultiMC/Prism style instance
            let profile_id = install_multimc_instance(path).await?;
            return Ok(ModpackImportResult {
                profile_id,
                name: "Imported Instance".to_string(),
                mods_installed: 0,
                mods_failed: 0,
                warnings: vec![],
            });
        } else if mods_dir.exists() {
            // Modrinth App style - mods folder directly in profile
            let profile_id = install_modrinth_profile_folder(path).await?;
            let folder_name = path
                .file_name()
                .map(|n| n.to_string_lossy().to_string())
                .unwrap_or_else(|| "Imported Profile".to_string());
            return Ok(ModpackImportResult {
                profile_id,
                name: folder_name,
                mods_installed: 0,
                mods_failed: 0,
                warnings: vec![],
            });
        } else if minecraft_dir.exists() {
            // Some other launcher with .minecraft subfolder
            let profile_id = install_modrinth_profile_folder(&path.join(".minecraft")).await?;
            let folder_name = path
                .file_name()
                .map(|n| n.to_string_lossy().to_string())
                .unwrap_or_else(|| "Imported Profile".to_string());
            return Ok(ModpackImportResult {
                profile_id,
                name: folder_name,
                mods_installed: 0,
                mods_failed: 0,
                warnings: vec![],
            });
        } else {
            return Err("Not a valid instance folder (no mods directory found)".to_string());
        }
    }

    // Read the file
    let bytes = tokio::fs::read(path)
        .await
        .map_err(|e| format!("Failed to read file: {}", e))?;

    // Try to open as zip
    let cursor = std::io::Cursor::new(&bytes);
    let mut archive =
        ZipArchive::new(cursor).map_err(|e| format!("Not a valid zip/modpack file: {}", e))?;

    // Detect format and install
    let mut file_names: Vec<String> = Vec::new();
    for i in 0..archive.len() {
        if let Ok(file) = archive.by_index(i) {
            file_names.push(file.name().to_string());
        }
    }
    drop(archive); // Drop before async operations

    if file_names.iter().any(|n| n == "modrinth.index.json") {
        // Modrinth format
        let (index, _) = {
            let cursor = std::io::Cursor::new(&bytes);
            let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;
            let mut index_file = archive
                .by_name("modrinth.index.json")
                .map_err(|e| e.to_string())?;
            let mut json = String::new();
            index_file
                .read_to_string(&mut json)
                .map_err(|e| e.to_string())?;
            let index: MrpackIndex = serde_json::from_str(&json).map_err(|e| e.to_string())?;
            (index, bytes.clone())
        };

        let minecraft_version = index
            .dependencies
            .get("minecraft")
            .cloned()
            .unwrap_or_else(|| "1.21.4".to_string());

        // Use existing mrpack install logic but with the bytes we already have
        let profile_id = install_mrpack_from_bytes(&bytes, &minecraft_version).await?;

        Ok(ModpackImportResult {
            profile_id,
            name: index.name,
            mods_installed: index
                .files
                .iter()
                .filter(|f| f.path.starts_with("mods/"))
                .count() as u32,
            mods_failed: 0,
            warnings: vec![],
        })
    } else if file_names.iter().any(|n| n == "manifest.json") {
        // CurseForge format
        let profile_id = install_curseforge_modpack_from_bytes(&bytes).await?;

        Ok(ModpackImportResult {
            profile_id,
            name: "CurseForge Modpack".to_string(),
            mods_installed: 0,
            mods_failed: 0,
            warnings: vec![],
        })
    } else if file_names.iter().any(|n| n == "mmc-pack.json") {
        // MultiMC exported zip - extract and install
        let profile_id = install_multimc_zip(&bytes).await?;

        Ok(ModpackImportResult {
            profile_id,
            name: "MultiMC Instance".to_string(),
            mods_installed: 0,
            mods_failed: 0,
            warnings: vec![],
        })
    } else {
        Err("Unrecognized modpack format".to_string())
    }
}

/// Install a Modrinth modpack from raw bytes
async fn install_mrpack_from_bytes(bytes: &[u8], game_version: &str) -> Result<String, String> {
    // Parse index synchronously to avoid holding ZipArchive across await
    let index: MrpackIndex = {
        let cursor = std::io::Cursor::new(bytes);
        let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

        let mut index_file = archive
            .by_name("modrinth.index.json")
            .map_err(|e| e.to_string())?;
        let mut json = String::new();
        index_file
            .read_to_string(&mut json)
            .map_err(|e| e.to_string())?;

        serde_json::from_str(&json).map_err(|e| e.to_string())?
    };

    // Create profile
    let mut profile_manager = ProfileManager::new();
    let profile = profile_manager.create_modpack_profile(&index.name, game_version, "modrinth")?;
    let profile_id = profile.id.clone();
    let mods_dir = profile_manager.get_mods_dir(game_version, &profile_id);

    tokio::fs::create_dir_all(&mods_dir)
        .await
        .map_err(|e| e.to_string())?;

    let client = create_client()?;

    // Download mods
    for file in &index.files {
        if let Some(env) = &file.env {
            if env.client == "unsupported" {
                continue;
            }
        }

        if !file.path.starts_with("mods/") {
            continue;
        }

        let filename = file.path.strip_prefix("mods/").unwrap_or(&file.path);
        let dest_path = mods_dir.join(filename);

        if dest_path.exists() {
            continue;
        }

        for url in &file.downloads {
            if let Ok(response) = client.get(url).send().await {
                if response.status().is_success() {
                    if let Ok(data) = response.bytes().await {
                        tokio::fs::write(&dest_path, &data).await.ok();
                        break;
                    }
                }
            }
        }
    }

    // Extract overrides
    let overrides: Vec<(PathBuf, Vec<u8>)> = {
        let cursor = std::io::Cursor::new(bytes);
        let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

        let game_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient");

        let mut files = Vec::new();

        for i in 0..archive.len() {
            if let Ok(mut file) = archive.by_index(i) {
                let name = file.name().to_string();
                if name.starts_with("overrides/") {
                    let relative = name.strip_prefix("overrides/").unwrap_or(&name);
                    if !relative.is_empty() && !name.ends_with('/') {
                        let dest = game_dir.join(relative);
                        let mut contents = Vec::new();
                        file.read_to_end(&mut contents).ok();
                        if !contents.is_empty() {
                            files.push((dest, contents));
                        }
                    }
                }
            }
        }

        files
    };

    for (dest, contents) in overrides {
        if let Some(parent) = dest.parent() {
            tokio::fs::create_dir_all(parent).await.ok();
        }
        tokio::fs::write(&dest, &contents).await.ok();
    }

    profile_manager.set_active_profile(game_version, &profile_id)?;

    Ok(profile_id)
}

/// Install a MultiMC instance from a zip file
async fn install_multimc_zip(bytes: &[u8]) -> Result<String, String> {
    // Extract to a temp directory and then install
    let temp_dir = std::env::temp_dir().join(format!("miracle_import_{}", uuid::Uuid::new_v4()));

    // Extract files synchronously first to avoid holding ZipArchive across await
    let files_to_write: Vec<(PathBuf, Vec<u8>)> = {
        let cursor = std::io::Cursor::new(bytes);
        let mut archive = ZipArchive::new(cursor).map_err(|e| e.to_string())?;

        let mut files = Vec::new();
        for i in 0..archive.len() {
            let mut file = archive.by_index(i).map_err(|e| e.to_string())?;
            let name = file.name().to_string();

            if !name.ends_with('/') {
                let dest = temp_dir.join(&name);
                let mut contents = Vec::new();
                file.read_to_end(&mut contents).ok();
                if !contents.is_empty() {
                    files.push((dest, contents));
                }
            }
        }
        files
    };

    // Now create directories and write files asynchronously
    tokio::fs::create_dir_all(&temp_dir)
        .await
        .map_err(|e| e.to_string())?;

    for (dest, contents) in files_to_write {
        if let Some(parent) = dest.parent() {
            tokio::fs::create_dir_all(parent).await.ok();
        }
        tokio::fs::write(&dest, &contents).await.ok();
    }

    // Install from the extracted directory
    let result = install_multimc_instance(&temp_dir).await;

    // Clean up temp directory
    tokio::fs::remove_dir_all(&temp_dir).await.ok();

    result
}

// ============================================================================
// Auto-Detection of Installed Instances
// ============================================================================

/// Detected instance from another launcher
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DetectedInstance {
    pub name: String,
    pub path: String,
    pub source: String, // "modrinth", "curseforge", "prism", "multimc"
    pub minecraft_version: Option<String>,
    pub loader: Option<String>,
    pub mod_count: u32,
}

/// Detect all installed instances from other launchers
#[tauri::command]
pub async fn detect_installed_instances() -> Result<Vec<DetectedInstance>, String> {
    let mut instances = Vec::new();

    // Get user directories
    let appdata = std::env::var("APPDATA").ok();
    let userprofile = std::env::var("USERPROFILE").ok();

    // Scan Modrinth App (check both new and legacy paths)
    if let Some(ref appdata) = appdata {
        // New path (0.8.0+): %APPDATA%/ModrinthApp/profiles
        // Legacy path: %APPDATA%/com.modrinth.theseus/profiles
        for modrinth_dir in &["ModrinthApp", "com.modrinth.theseus"] {
            let modrinth_path = PathBuf::from(appdata).join(modrinth_dir).join("profiles");
            if modrinth_path.exists() {
                if let Ok(entries) = std::fs::read_dir(&modrinth_path) {
                    for entry in entries.flatten() {
                        if entry.path().is_dir() {
                            if let Some(instance) = detect_modrinth_instance(&entry.path()) {
                                // Avoid duplicates (in case both paths exist during migration)
                                if !instances
                                    .iter()
                                    .any(|i: &DetectedInstance| i.name == instance.name)
                                {
                                    instances.push(instance);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scan CurseForge
    if let Some(ref userprofile) = userprofile {
        let curseforge_path = PathBuf::from(userprofile)
            .join("curseforge")
            .join("minecraft")
            .join("Instances");
        if curseforge_path.exists() {
            if let Ok(entries) = std::fs::read_dir(&curseforge_path) {
                for entry in entries.flatten() {
                    if entry.path().is_dir() {
                        if let Some(instance) = detect_curseforge_instance(&entry.path()) {
                            instances.push(instance);
                        }
                    }
                }
            }
        }
    }

    // Scan Prism Launcher
    if let Some(ref appdata) = appdata {
        let prism_path = PathBuf::from(appdata)
            .join("PrismLauncher")
            .join("instances");
        if prism_path.exists() {
            if let Ok(entries) = std::fs::read_dir(&prism_path) {
                for entry in entries.flatten() {
                    if entry.path().is_dir() {
                        if let Some(instance) =
                            detect_multimc_style_instance(&entry.path(), "prism")
                        {
                            instances.push(instance);
                        }
                    }
                }
            }
        }
    }

    // Scan MultiMC (common locations)
    if let Some(ref appdata) = appdata {
        let multimc_path = PathBuf::from(appdata).join("MultiMC").join("instances");
        if multimc_path.exists() {
            if let Ok(entries) = std::fs::read_dir(&multimc_path) {
                for entry in entries.flatten() {
                    if entry.path().is_dir() {
                        if let Some(instance) =
                            detect_multimc_style_instance(&entry.path(), "multimc")
                        {
                            instances.push(instance);
                        }
                    }
                }
            }
        }
    }

    // Also check Program Files for MultiMC
    if let Some(ref userprofile) = userprofile {
        for multimc_dir in &["MultiMC", "MultiMC5"] {
            let multimc_path = PathBuf::from(userprofile)
                .join(multimc_dir)
                .join("instances");
            if multimc_path.exists() {
                if let Ok(entries) = std::fs::read_dir(&multimc_path) {
                    for entry in entries.flatten() {
                        if entry.path().is_dir() {
                            if let Some(instance) =
                                detect_multimc_style_instance(&entry.path(), "multimc")
                            {
                                // Avoid duplicates
                                if !instances.iter().any(|i| i.path == instance.path) {
                                    instances.push(instance);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Scan ATLauncher
    if let Some(ref appdata) = appdata {
        let atlauncher_path = PathBuf::from(appdata).join("ATLauncher").join("instances");
        if atlauncher_path.exists() {
            if let Ok(entries) = std::fs::read_dir(&atlauncher_path) {
                for entry in entries.flatten() {
                    if entry.path().is_dir() {
                        if let Some(instance) = detect_atlauncher_instance(&entry.path()) {
                            instances.push(instance);
                        }
                    }
                }
            }
        }
    }

    tracing::info!(
        "Detected {} instances from other launchers",
        instances.len()
    );
    Ok(instances)
}

fn detect_modrinth_instance(path: &std::path::Path) -> Option<DetectedInstance> {
    // Modrinth stores profile metadata in app.db (SQLite), not individual JSON files
    // We detect by checking for a mods folder and use the directory name as instance name
    let mods_dir = path.join("mods");
    if !mods_dir.exists() {
        return None;
    }

    // Use folder name as instance name
    let name = path.file_name()?.to_string_lossy().to_string();

    // Skip internal folders (those starting with .)
    if name.starts_with('.') {
        return None;
    }

    // Count mods and detect version from filenames
    let mut mod_count = 0;
    let mut version_counts: std::collections::HashMap<String, u32> =
        std::collections::HashMap::new();

    if let Ok(entries) = std::fs::read_dir(&mods_dir) {
        for entry in entries.flatten() {
            let entry_path = entry.path();
            if entry_path.extension().map(|e| e == "jar").unwrap_or(false) {
                mod_count += 1;
                let name = entry.file_name().to_string_lossy().to_string();
                if let Some(version) = extract_mc_version_from_mod(&name) {
                    *version_counts.entry(version).or_insert(0) += 1;
                }
            }
        }
    }

    // Use the most common version found
    let minecraft_version = version_counts
        .iter()
        .max_by_key(|(_, count)| *count)
        .map(|(v, _)| v.clone());

    // Check for fabric
    let loader = if path.join(".fabric").exists() {
        Some("fabric".to_string())
    } else {
        None
    };

    Some(DetectedInstance {
        name,
        path: path.to_string_lossy().to_string(),
        source: "modrinth".to_string(),
        minecraft_version,
        loader,
        mod_count,
    })
}

fn extract_mc_version_from_mod(filename: &str) -> Option<String> {
    // Look for Minecraft version patterns in mod filenames
    // Common patterns: "+1.21.4", "-1.21.1", "mc1.21", "MC1.21.4", "for-MC1.21", "_1.21.1"
    let lower = filename.to_lowercase();

    // Find position after common prefixes
    let search_positions: Vec<usize> = vec![
        lower.find("+1."),
        lower.find("-1."),
        lower.find("mc1."),
        lower.find("_1."),
        lower.find(".1."),
    ]
    .into_iter()
    .flatten()
    .collect();

    for pos in search_positions {
        // Adjust position to start at "1."
        let start = if lower[pos..].starts_with("+")
            || lower[pos..].starts_with("-")
            || lower[pos..].starts_with("_")
            || lower[pos..].starts_with(".")
        {
            pos + 1
        } else if lower[pos..].starts_with("mc") {
            pos + 2
        } else {
            pos
        };

        if let Some(version) = extract_mc_version(&filename[start..]) {
            // Validate it looks like a real MC version (1.18+ range)
            let parts: Vec<&str> = version.split('.').collect();
            if parts.len() >= 2 {
                if let (Ok(major), Ok(minor)) = (parts[0].parse::<u32>(), parts[1].parse::<u32>()) {
                    if major == 1 && minor >= 18 {
                        return Some(version);
                    }
                }
            }
        }
    }
    None
}

fn extract_mc_version(text: &str) -> Option<String> {
    // Look for Minecraft version patterns like 1.20.1, 1.21.4, etc.
    // Simple approach: find "1." followed by digits
    let chars: Vec<char> = text.chars().collect();
    for i in 0..chars.len().saturating_sub(3) {
        if chars[i] == '1' && chars[i + 1] == '.' && chars[i + 2].is_ascii_digit() {
            let mut end = i + 3;
            // Consume the minor version number
            while end < chars.len() && chars[end].is_ascii_digit() {
                end += 1;
            }
            // Check for patch version (e.g., .4 in 1.21.4)
            if end < chars.len() - 1 && chars[end] == '.' && chars[end + 1].is_ascii_digit() {
                end += 2;
                while end < chars.len() && chars[end].is_ascii_digit() {
                    end += 1;
                }
            }
            return Some(chars[i..end].iter().collect());
        }
    }
    None
}

fn detect_curseforge_instance(path: &std::path::Path) -> Option<DetectedInstance> {
    // CurseForge stores minecraftinstance.json
    let instance_json = path.join("minecraftinstance.json");
    if !instance_json.exists() {
        return None;
    }

    #[derive(Deserialize)]
    struct CurseForgeInstance {
        name: String,
        #[serde(rename = "gameVersion")]
        game_version: Option<String>,
        #[serde(rename = "baseModLoader")]
        base_mod_loader: Option<CurseForgeLoader>,
    }

    #[derive(Deserialize)]
    struct CurseForgeLoader {
        name: Option<String>,
    }

    let content = std::fs::read_to_string(&instance_json).ok()?;
    let instance: CurseForgeInstance = serde_json::from_str(&content).ok()?;

    let loader = instance.base_mod_loader.and_then(|l| l.name);

    // Count mods
    let mods_dir = path.join("mods");
    let mod_count = count_jar_files(&mods_dir);

    Some(DetectedInstance {
        name: instance.name,
        path: path.to_string_lossy().to_string(),
        source: "curseforge".to_string(),
        minecraft_version: instance.game_version,
        loader,
        mod_count,
    })
}

fn detect_multimc_style_instance(path: &std::path::Path, source: &str) -> Option<DetectedInstance> {
    // MultiMC/Prism use mmc-pack.json and instance.cfg
    let mmc_pack = path.join("mmc-pack.json");
    if !mmc_pack.exists() {
        return None;
    }

    // Get name from instance.cfg
    let instance_cfg = path.join("instance.cfg");
    let name = if instance_cfg.exists() {
        std::fs::read_to_string(&instance_cfg)
            .ok()
            .and_then(|content| parse_instance_cfg(&content).get("name").cloned())
    } else {
        None
    }
    .unwrap_or_else(|| path.file_name().unwrap().to_string_lossy().to_string());

    // Parse mmc-pack.json for version info
    let content = std::fs::read_to_string(&mmc_pack).ok()?;
    let pack: MmcPack = serde_json::from_str(&content).ok()?;

    let minecraft_version = pack
        .components
        .iter()
        .find(|c| c.uid == "net.minecraft")
        .map(|c| c.version.clone());

    let loader = if pack
        .components
        .iter()
        .any(|c| c.uid == "net.fabricmc.fabric-loader")
    {
        Some("fabric".to_string())
    } else if pack.components.iter().any(|c| c.uid.contains("forge")) {
        Some("forge".to_string())
    } else if pack.components.iter().any(|c| c.uid.contains("quilt")) {
        Some("quilt".to_string())
    } else {
        None
    };

    // Find mods folder
    let minecraft_dir = if path.join(".minecraft").exists() {
        path.join(".minecraft")
    } else if path.join("minecraft").exists() {
        path.join("minecraft")
    } else {
        path.to_path_buf()
    };
    let mods_dir = minecraft_dir.join("mods");
    let mod_count = count_jar_files(&mods_dir);

    Some(DetectedInstance {
        name,
        path: path.to_string_lossy().to_string(),
        source: source.to_string(),
        minecraft_version,
        loader,
        mod_count,
    })
}

fn detect_atlauncher_instance(path: &std::path::Path) -> Option<DetectedInstance> {
    // ATLauncher stores instance.json
    let instance_json = path.join("instance.json");
    if !instance_json.exists() {
        return None;
    }

    #[derive(Deserialize)]
    struct ATLInstance {
        launcher: Option<ATLLauncher>,
    }

    #[derive(Deserialize)]
    struct ATLLauncher {
        name: Option<String>,
        #[serde(rename = "minecraftVersion")]
        minecraft_version: Option<String>,
        #[serde(rename = "loaderVersion")]
        loader_version: Option<ATLLoader>,
    }

    #[derive(Deserialize)]
    struct ATLLoader {
        #[serde(rename = "type")]
        loader_type: Option<String>,
    }

    let content = std::fs::read_to_string(&instance_json).ok()?;
    let instance: ATLInstance = serde_json::from_str(&content).ok()?;

    let launcher = instance.launcher?;
    let name = launcher
        .name
        .unwrap_or_else(|| path.file_name().unwrap().to_string_lossy().to_string());

    let loader = launcher.loader_version.and_then(|l| l.loader_type);

    // Count mods
    let mods_dir = path.join("mods");
    let mod_count = count_jar_files(&mods_dir);

    Some(DetectedInstance {
        name,
        path: path.to_string_lossy().to_string(),
        source: "atlauncher".to_string(),
        minecraft_version: launcher.minecraft_version,
        loader,
        mod_count,
    })
}

fn count_jar_files(mods_dir: &std::path::Path) -> u32 {
    if !mods_dir.exists() {
        return 0;
    }

    std::fs::read_dir(mods_dir)
        .map(|entries| {
            entries
                .flatten()
                .filter(|e| e.path().extension().map_or(false, |ext| ext == "jar"))
                .count() as u32
        })
        .unwrap_or(0)
}
