use crate::models::{ModVersion, UpdateCheck};
use crate::AppState;
use axum::{extract::State, http::StatusCode, Json};
use std::sync::Arc;

pub async fn check_updates(
    State(_state): State<Arc<AppState>>,
) -> Result<Json<UpdateCheck>, (StatusCode, String)> {
    // TODO: Check for launcher updates from database/config
    Ok(Json(UpdateCheck {
        launcher_version: "1.0.0".to_string(),
        update_available: false,
        download_url: None,
        changelog: None,
    }))
}

pub async fn get_mod_versions(
    State(_state): State<Arc<AppState>>,
) -> Result<Json<Vec<ModVersion>>, (StatusCode, String)> {
    // Return current mod versions
    Ok(Json(vec![
        ModVersion {
            mod_id: "miracle".to_string(),
            version: "1.0.0".to_string(),
            minecraft_version: "1.21.4".to_string(),
            download_url: "https://cdn.miracle.gg/mods/miracle-1.0.0.jar".to_string(),
            sha256: "placeholder".to_string(),
        },
        ModVersion {
            mod_id: "sodium".to_string(),
            version: "0.6.5".to_string(),
            minecraft_version: "1.21.4".to_string(),
            download_url: "https://cdn.miracle.gg/mods/sodium-0.6.5.jar".to_string(),
            sha256: "placeholder".to_string(),
        },
        ModVersion {
            mod_id: "iris".to_string(),
            version: "1.8.0".to_string(),
            minecraft_version: "1.21.4".to_string(),
            download_url: "https://cdn.miracle.gg/mods/iris-1.8.0.jar".to_string(),
            sha256: "placeholder".to_string(),
        },
    ]))
}
