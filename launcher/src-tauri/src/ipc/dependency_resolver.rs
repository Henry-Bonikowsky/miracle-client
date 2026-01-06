use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::Path;
use zip::ZipArchive;

#[derive(Debug, Serialize, Deserialize, Clone, PartialEq, Eq, Hash)]
pub struct ModDependency {
    pub mod_id: String,
    pub version_requirement: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct FabricModJson {
    pub id: String,
    pub version: String,
    pub name: Option<String>,
    pub depends: Option<HashMap<String, String>>,
    pub recommends: Option<HashMap<String, String>>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct DependencyResolutionResult {
    pub missing_dependencies: Vec<ModDependency>,
    pub installed_mods: Vec<String>,
}

/// Parse fabric.mod.json from a jar file
pub fn parse_mod_metadata(jar_path: &Path) -> Result<FabricModJson, String> {
    let file = fs::File::open(jar_path).map_err(|e| format!("Failed to open jar: {}", e))?;
    let mut archive = ZipArchive::new(file).map_err(|e| format!("Failed to read zip: {}", e))?;

    // Try to find fabric.mod.json
    let mut mod_json_file = archive
        .by_name("fabric.mod.json")
        .map_err(|_| "fabric.mod.json not found in jar".to_string())?;

    let mut contents = String::new();
    std::io::Read::read_to_string(&mut mod_json_file, &mut contents)
        .map_err(|e| format!("Failed to read fabric.mod.json: {}", e))?;

    serde_json::from_str(&contents).map_err(|e| format!("Failed to parse fabric.mod.json: {}", e))
}

/// Get all installed mod IDs from a directory
pub fn get_installed_mod_ids(mods_dir: &Path) -> Result<HashSet<String>, String> {
    let mut installed = HashSet::new();

    if !mods_dir.exists() {
        return Ok(installed);
    }

    let entries =
        fs::read_dir(mods_dir).map_err(|e| format!("Failed to read mods directory: {}", e))?;

    for entry in entries {
        let entry = entry.map_err(|e| format!("Failed to read directory entry: {}", e))?;
        let path = entry.path();

        if path.extension().and_then(|s| s.to_str()) == Some("jar") {
            if let Ok(metadata) = parse_mod_metadata(&path) {
                installed.insert(metadata.id);
            }
        }
    }

    Ok(installed)
}

/// Resolve dependencies for all mods in a directory
pub fn resolve_dependencies(mods_dir: &Path) -> Result<DependencyResolutionResult, String> {
    let installed_ids = get_installed_mod_ids(mods_dir)?;
    let mut all_required_deps = HashSet::new();

    // Scan all mods and collect their dependencies
    let entries =
        fs::read_dir(mods_dir).map_err(|e| format!("Failed to read mods directory: {}", e))?;

    for entry in entries {
        let entry = entry.map_err(|e| format!("Failed to read directory entry: {}", e))?;
        let path = entry.path();

        if path.extension().and_then(|s| s.to_str()) == Some("jar") {
            if let Ok(metadata) = parse_mod_metadata(&path) {
                // Add required dependencies
                if let Some(depends) = metadata.depends {
                    for (dep_id, version_req) in depends {
                        // Skip minecraft, java, and fabricloader as these are handled separately
                        if dep_id == "minecraft" || dep_id == "java" || dep_id == "fabricloader" {
                            continue;
                        }

                        all_required_deps.insert(ModDependency {
                            mod_id: dep_id,
                            version_requirement: Some(version_req),
                        });
                    }
                }
            }
        }
    }

    // Find missing dependencies
    let missing_dependencies: Vec<ModDependency> = all_required_deps
        .into_iter()
        .filter(|dep| !installed_ids.contains(&dep.mod_id))
        .collect();

    Ok(DependencyResolutionResult {
        missing_dependencies,
        installed_mods: installed_ids.into_iter().collect(),
    })
}

/// Download and install a mod from Modrinth
pub async fn install_dependency(
    mod_id: &str,
    minecraft_version: &str,
    mods_dir: &Path,
) -> Result<(), String> {
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(120))
        .user_agent("MiracleClient/1.0")
        .build()
        .map_err(|e| e.to_string())?;

    tracing::info!(
        "Resolving dependency: {} for Minecraft {}",
        mod_id,
        minecraft_version
    );

    // Search for the mod by ID
    let search_url = format!("https://api.modrinth.com/v2/project/{}", mod_id);

    let project_response = client
        .get(&search_url)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch project info: {}", e))?;

    if !project_response.status().is_success() {
        return Err(format!(
            "Mod '{}' not found on Modrinth (status: {})",
            mod_id,
            project_response.status()
        ));
    }

    let project: serde_json::Value = project_response
        .json()
        .await
        .map_err(|e| format!("Failed to parse project response: {}", e))?;

    let project_slug = project["slug"]
        .as_str()
        .ok_or_else(|| format!("Invalid project data for {}", mod_id))?;

    // Get versions for this Minecraft version
    let versions_url = format!(
        "https://api.modrinth.com/v2/project/{}/version?game_versions=[\"{}\"]&loaders=[\"fabric\"]",
        project_slug, minecraft_version
    );

    let versions_response = client
        .get(&versions_url)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch versions: {}", e))?;

    let versions: Vec<serde_json::Value> = versions_response
        .json()
        .await
        .map_err(|e| format!("Failed to parse versions: {}", e))?;

    if versions.is_empty() {
        return Err(format!(
            "No compatible version found for {} (Minecraft {})",
            mod_id, minecraft_version
        ));
    }

    // Get the latest version
    let latest_version = &versions[0];
    let files = latest_version["files"]
        .as_array()
        .ok_or_else(|| "No files found".to_string())?;

    let primary_file = files
        .iter()
        .find(|f| f["primary"].as_bool().unwrap_or(false))
        .or_else(|| files.first())
        .ok_or_else(|| "No downloadable file found".to_string())?;

    let download_url = primary_file["url"]
        .as_str()
        .ok_or_else(|| "No download URL".to_string())?;

    let filename = primary_file["filename"]
        .as_str()
        .ok_or_else(|| "No filename".to_string())?;

    tracing::info!("Downloading dependency {} from {}", mod_id, download_url);

    // Download the file
    let file_response = client
        .get(download_url)
        .send()
        .await
        .map_err(|e| format!("Failed to download: {}", e))?;

    let file_bytes = file_response
        .bytes()
        .await
        .map_err(|e| format!("Failed to read file bytes: {}", e))?;

    // Save to mods directory
    let output_path = mods_dir.join(filename);
    fs::write(&output_path, file_bytes).map_err(|e| format!("Failed to write file: {}", e))?;

    tracing::info!("Successfully installed dependency: {}", mod_id);

    Ok(())
}

/// Resolve and install all missing dependencies
pub async fn resolve_and_install_dependencies(
    mods_dir: &Path,
    minecraft_version: &str,
) -> Result<Vec<String>, String> {
    let resolution = resolve_dependencies(mods_dir)?;

    if resolution.missing_dependencies.is_empty() {
        tracing::info!("All dependencies are satisfied!");
        return Ok(Vec::new());
    }

    tracing::info!(
        "Found {} missing dependencies",
        resolution.missing_dependencies.len()
    );

    let mut installed = Vec::new();

    for dep in resolution.missing_dependencies {
        tracing::info!("Installing missing dependency: {}", dep.mod_id);

        match install_dependency(&dep.mod_id, minecraft_version, mods_dir).await {
            Ok(_) => {
                installed.push(dep.mod_id.clone());
            }
            Err(e) => {
                tracing::warn!("Failed to install dependency {}: {}", dep.mod_id, e);
                // Continue with other dependencies even if one fails
            }
        }

        // Add delay to avoid rate limiting
        tokio::time::sleep(std::time::Duration::from_millis(300)).await;
    }

    // Recursively check for dependencies of newly installed mods
    if !installed.is_empty() {
        tracing::info!("Checking for transitive dependencies...");
        let additional = Box::pin(resolve_and_install_dependencies(
            mods_dir,
            minecraft_version,
        ))
        .await?;
        installed.extend(additional);
    }

    Ok(installed)
}
