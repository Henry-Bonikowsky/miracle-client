mod auth;
mod clips;
mod ipc;
mod minecraft;
mod profiles;
mod supabase;
mod updater;

use ipc::AppState;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // Initialize logging
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            std::env::var("RUST_LOG").unwrap_or_else(|_| "info".into()),
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    tracing::info!("Starting Miracle Client Launcher v1.0.0");

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_http::init())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_clipboard_manager::init())
        .manage(AppState::default())
        .invoke_handler(tauri::generate_handler![
            // Auth commands
            ipc::auth_start_device_flow,
            ipc::auth_poll_device_flow,
            ipc::auth_logout,
            ipc::auth_refresh,
            ipc::write_accounts_for_game,
            // Game commands
            ipc::get_minecraft_versions,
            ipc::launch_game,
            ipc::stop_game,
            // Update commands
            ipc::check_updates,
            // Mod commands
            ipc::get_installed_mods,
            ipc::toggle_mod,
            ipc::uninstall_mod,
            ipc::download_mod,
            ipc::check_mod_compatibility,
            ipc::delete_all_mod_folders,
            ipc::open_mods_folder,
            ipc::resolve_dependencies_for_version,
            // Miracle Client update commands
            ipc::check_miracle_update,
            ipc::download_miracle_update,
            ipc::is_update_service_configured,
            // Friends commands
            ipc::friends_register_user,
            ipc::friends_search_users,
            ipc::friends_get_list,
            ipc::friends_send_request,
            ipc::friends_accept_request,
            ipc::friends_remove,
            ipc::friends_update_status,
            // Profile commands
            ipc::get_profiles,
            ipc::get_active_profile,
            ipc::set_active_profile,
            ipc::create_profile,
            ipc::create_preset_profile,
            ipc::delete_profile,
            ipc::duplicate_profile,
            ipc::export_profile,
            ipc::import_profile,
            ipc::get_profile_mods_dir,
            ipc::get_performance_mods,
            ipc::ensure_performance_mods,
            // Content browser commands - Modrinth
            ipc::modrinth::search_modrinth,
            ipc::modrinth::get_modrinth_project,
            ipc::modrinth::get_modrinth_versions,
            ipc::modrinth::get_modrinth_categories,
            ipc::modrinth::download_modrinth_content,
            // Content browser commands - CurseForge
            ipc::curseforge::search_curseforge,
            ipc::curseforge::download_curseforge_content,
            // Modpack commands
            ipc::modpack::install_modpack,
            ipc::modpack::get_modpack_info,
            ipc::modpack::preview_modpack_file,
            ipc::modpack::import_modpack_file,
            ipc::modpack::detect_installed_instances,
            // Mod update commands
            ipc::mod_updates::check_mod_updates,
            ipc::mod_updates::update_mod,
            ipc::mod_updates::update_all_mods,
            ipc::mod_updates::get_mod_metadata,
            // Profile sharing commands
            ipc::share_profile_online,
            ipc::get_shared_profile,
            ipc::import_shared_profile,
            // Theme sync commands
            ipc::get_mod_theme,
            // Clips commands
            clips::list_clips,
            clips::get_clip_metadata,
            clips::delete_clip,
            clips::get_clips_directory,
            clips::open_clips_folder,
            clips::refresh_clips,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
