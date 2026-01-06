use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;

const MODRINTH_API_BASE: &str = "https://api.modrinth.com/v2";

/// Metadata stored for each installed mod to track updates
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModMetadata {
    pub source: String, // "modrinth" | "curseforge"
    pub project_slug: String,
    pub project_id: String,
    pub installed_version: String,
    pub version_id: String,
    pub installed_at: String,
}

/// The metadata file that stores info about all installed mods
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ModMetadataIndex {
    pub mods: HashMap<String, ModMetadata>, // filename -> metadata
}

/// Result of checking a mod for updates
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModUpdateCheck {
    pub filename: String,
    pub mod_name: String,
    pub current_version: String,
    pub latest_version: String,
    pub latest_version_id: String,
    pub has_update: bool,
    pub source: String,
    pub project_slug: String,
}

fn create_client() -> Result<reqwest::Client, String> {
    reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(30))
        .user_agent("MiracleClient/1.0 (https://github.com/miracle-client)")
        .build()
        .map_err(|e| e.to_string())
}

/// Get the metadata file path for a profile
fn get_metadata_path(version: &str, profile_id: &str) -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("mods")
        .join(version)
        .join(profile_id)
        .join("mod_metadata.json")
}

/// Load the metadata index for a profile
pub fn load_metadata(version: &str, profile_id: &str) -> ModMetadataIndex {
    let path = get_metadata_path(version, profile_id);
    if path.exists() {
        if let Ok(contents) = std::fs::read_to_string(&path) {
            if let Ok(index) = serde_json::from_str(&contents) {
                return index;
            }
        }
    }
    ModMetadataIndex::default()
}

/// Save the metadata index for a profile
pub fn save_metadata(
    version: &str,
    profile_id: &str,
    index: &ModMetadataIndex,
) -> Result<(), String> {
    let path = get_metadata_path(version, profile_id);

    // Ensure parent directory exists
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)
            .map_err(|e| format!("Failed to create metadata directory: {}", e))?;
    }

    let contents = serde_json::to_string_pretty(index)
        .map_err(|e| format!("Failed to serialize metadata: {}", e))?;

    std::fs::write(&path, contents).map_err(|e| format!("Failed to write metadata: {}", e))?;

    Ok(())
}

/// Add or update metadata for a mod
pub fn update_mod_metadata(
    version: &str,
    profile_id: &str,
    filename: &str,
    metadata: ModMetadata,
) -> Result<(), String> {
    let mut index = load_metadata(version, profile_id);
    index.mods.insert(filename.to_string(), metadata);
    save_metadata(version, profile_id, &index)
}

/// Remove metadata for a mod
pub fn remove_mod_metadata(version: &str, profile_id: &str, filename: &str) -> Result<(), String> {
    let mut index = load_metadata(version, profile_id);
    index.mods.remove(filename);
    save_metadata(version, profile_id, &index)
}

/// Check all mods in a profile for updates
#[tauri::command]
pub async fn check_mod_updates(
    version: String,
    profile_id: String,
) -> Result<Vec<ModUpdateCheck>, String> {
    let index = load_metadata(&version, &profile_id);

    if index.mods.is_empty() {
        return Ok(Vec::new());
    }

    let client = create_client()?;
    let mut updates = Vec::new();

    for (filename, metadata) in &index.mods {
        match metadata.source.as_str() {
            "modrinth" => {
                if let Some(update) =
                    check_modrinth_update(&client, filename, metadata, &version).await?
                {
                    updates.push(update);
                }
            }
            "curseforge" => {
                // CurseForge update checking would go here
                // For now, skip - requires more complex API handling
            }
            _ => {}
        }
    }

    Ok(updates)
}

/// Check a single Modrinth mod for updates
async fn check_modrinth_update(
    client: &reqwest::Client,
    filename: &str,
    metadata: &ModMetadata,
    game_version: &str,
) -> Result<Option<ModUpdateCheck>, String> {
    let url = format!(
        "{}/project/{}/version?game_versions=[\"{}\"]&loaders=[\"fabric\"]",
        MODRINTH_API_BASE, metadata.project_slug, game_version
    );

    let response = client.get(&url).send().await.map_err(|e| {
        format!(
            "Failed to check updates for {}: {}",
            metadata.project_slug, e
        )
    })?;

    if !response.status().is_success() {
        return Ok(None); // Skip if API error
    }

    #[derive(Deserialize)]
    struct VersionInfo {
        id: String,
        version_number: String,
        name: String,
    }

    let versions: Vec<VersionInfo> = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse versions: {}", e))?;

    if let Some(latest) = versions.first() {
        // Check if there's a newer version
        if latest.id != metadata.version_id {
            return Ok(Some(ModUpdateCheck {
                filename: filename.to_string(),
                mod_name: latest.name.clone(),
                current_version: metadata.installed_version.clone(),
                latest_version: latest.version_number.clone(),
                latest_version_id: latest.id.clone(),
                has_update: true,
                source: "modrinth".to_string(),
                project_slug: metadata.project_slug.clone(),
            }));
        }
    }

    Ok(None)
}

/// Update a single mod to the latest version
#[tauri::command]
pub async fn update_mod(
    filename: String,
    version: String,
    profile_id: String,
) -> Result<String, String> {
    let index = load_metadata(&version, &profile_id);

    let metadata = index
        .mods
        .get(&filename)
        .ok_or_else(|| format!("No metadata found for {}", filename))?;

    let mods_dir = dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("mods")
        .join(&version)
        .join(&profile_id);

    match metadata.source.as_str() {
        "modrinth" => {
            // Delete the old file
            let old_path = mods_dir.join(&filename);
            if old_path.exists() {
                std::fs::remove_file(&old_path)
                    .map_err(|e| format!("Failed to remove old mod: {}", e))?;
            }

            // Download the new version
            let new_filename =
                super::modrinth::download_mod_to_dir(&metadata.project_slug, &version, &mods_dir)
                    .await?;

            Ok(format!("Updated {} to {}", filename, new_filename))
        }
        _ => Err("Unsupported source for updates".to_string()),
    }
}

/// Update all mods that have available updates
#[tauri::command]
pub async fn update_all_mods(version: String, profile_id: String) -> Result<Vec<String>, String> {
    let updates = check_mod_updates(version.clone(), profile_id.clone()).await?;

    let mut updated = Vec::new();

    for update in updates {
        if update.has_update {
            match update_mod(update.filename.clone(), version.clone(), profile_id.clone()).await {
                Ok(msg) => {
                    tracing::info!("{}", msg);
                    updated.push(update.mod_name);
                }
                Err(e) => {
                    tracing::warn!("Failed to update {}: {}", update.filename, e);
                }
            }
        }
    }

    Ok(updated)
}

/// Get metadata for a specific mod
#[tauri::command]
pub async fn get_mod_metadata(
    filename: String,
    version: String,
    profile_id: String,
) -> Result<Option<ModMetadata>, String> {
    let index = load_metadata(&version, &profile_id);
    Ok(index.mods.get(&filename).cloned())
}
