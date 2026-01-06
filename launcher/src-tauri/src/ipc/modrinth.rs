use reqwest::Client;
use serde::{Deserialize, Serialize};
use tauri::Manager;

const MODRINTH_API_BASE: &str = "https://api.modrinth.com/v2";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthProject {
    pub slug: String,
    pub title: String,
    pub description: String,
    pub categories: Vec<String>,
    pub client_side: String,
    pub server_side: String,
    pub project_type: String,
    pub downloads: i64,
    pub icon_url: Option<String>,
    pub color: Option<i32>,
    pub author: String,
    pub display_categories: Vec<String>,
    pub versions: Vec<String>,
    pub follows: i64,
    pub date_created: String,
    pub date_modified: String,
    pub latest_version: Option<String>,
    pub license: String,
    pub gallery: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthSearchResult {
    pub slug: String,
    pub title: String,
    pub description: String,
    pub categories: Vec<String>,
    pub client_side: String,
    pub server_side: String,
    pub project_type: String,
    pub downloads: i64,
    pub icon_url: Option<String>,
    pub color: Option<i32>,
    pub author: String,
    pub display_categories: Vec<String>,
    pub versions: Vec<String>,
    pub follows: i64,
    pub date_created: String,
    pub date_modified: String,
    pub latest_version: Option<String>,
    pub license: String,
    pub gallery: Vec<String>,
    pub project_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthSearchResponse {
    pub hits: Vec<ModrinthSearchResult>,
    pub offset: i32,
    pub limit: i32,
    pub total_hits: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthVersion {
    pub id: String,
    pub project_id: String,
    pub name: String,
    pub version_number: String,
    pub changelog: Option<String>,
    pub game_versions: Vec<String>,
    pub loaders: Vec<String>,
    pub files: Vec<ModrinthFile>,
    pub downloads: i64,
    pub date_published: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthFile {
    pub url: String,
    pub filename: String,
    pub primary: bool,
    pub size: i64,
    pub hashes: ModrinthHashes,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthHashes {
    pub sha1: String,
    pub sha512: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModrinthCategory {
    pub icon: String,
    pub name: String,
    pub project_type: String,
    pub header: String,
}

fn create_client() -> Result<Client, String> {
    Client::builder()
        .timeout(std::time::Duration::from_secs(30))
        .user_agent("MiracleClient/1.0 (https://github.com/miracle-client)")
        .build()
        .map_err(|e| e.to_string())
}

/// Search Modrinth for content
#[tauri::command]
pub async fn search_modrinth(
    query: String,
    content_type: String, // "mod", "resourcepack", "shader", "datapack"
    categories: Vec<String>,
    sort: String, // "relevance", "downloads", "follows", "newest", "updated"
    version: String,
    offset: u32,
    limit: u32,
) -> Result<ModrinthSearchResponse, String> {
    let client = create_client()?;

    // Build facets array
    let mut facets: Vec<Vec<String>> = vec![];

    // Project type facet
    let project_type = match content_type.as_str() {
        "mod" => "mod",
        "resourcepack" => "resourcepack",
        "shader" => "shader",
        "datapack" => "datapack",
        "modpack" => "modpack",
        _ => "mod",
    };
    facets.push(vec![format!("project_type:{}", project_type)]);

    // Version facet
    if !version.is_empty() {
        facets.push(vec![format!("versions:{}", version)]);
    }

    // Categories facet
    if !categories.is_empty() {
        let cat_facet: Vec<String> = categories
            .iter()
            .map(|c| format!("categories:{}", c))
            .collect();
        facets.push(cat_facet);
    }

    // For mods and modpacks, add fabric loader filter
    if content_type == "mod" || content_type == "modpack" {
        facets.push(vec!["categories:fabric".to_string()]);
    }

    // Convert facets to JSON string
    let facets_json = serde_json::to_string(&facets).unwrap_or_else(|_| "[]".to_string());

    // Map sort parameter
    let index = match sort.as_str() {
        "downloads" => "downloads",
        "follows" => "follows",
        "newest" => "newest",
        "updated" => "updated",
        _ => "relevance",
    };

    let url = format!(
        "{}/search?query={}&facets={}&index={}&offset={}&limit={}",
        MODRINTH_API_BASE,
        urlencoding::encode(&query),
        urlencoding::encode(&facets_json),
        index,
        offset,
        limit.min(100) // Modrinth max is 100
    );

    tracing::info!("Searching Modrinth: {}", url);

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to search Modrinth: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Modrinth search failed: HTTP {}",
            response.status()
        ));
    }

    let search_result: ModrinthSearchResponse = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse Modrinth response: {}", e))?;

    Ok(search_result)
}

/// Get a specific project from Modrinth
#[tauri::command]
pub async fn get_modrinth_project(id_or_slug: String) -> Result<ModrinthProject, String> {
    let client = create_client()?;

    let url = format!("{}/project/{}", MODRINTH_API_BASE, id_or_slug);

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to get project: {}", e))?;

    if !response.status().is_success() {
        return Err(format!("Failed to get project: HTTP {}", response.status()));
    }

    let project: ModrinthProject = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse project: {}", e))?;

    Ok(project)
}

/// Get versions for a project
#[tauri::command]
pub async fn get_modrinth_versions(
    project_id: String,
    game_version: Option<String>,
    loader: Option<String>,
) -> Result<Vec<ModrinthVersion>, String> {
    let client = create_client()?;

    let mut url = format!("{}/project/{}/version", MODRINTH_API_BASE, project_id);

    // Add query params
    let mut params: Vec<String> = vec![];
    if let Some(ver) = game_version {
        params.push(format!("game_versions=[\"{}\"]", ver));
    }
    if let Some(ldr) = loader {
        params.push(format!("loaders=[\"{}\"]", ldr));
    }

    if !params.is_empty() {
        url = format!("{}?{}", url, params.join("&"));
    }

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to get versions: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Failed to get versions: HTTP {}",
            response.status()
        ));
    }

    let versions: Vec<ModrinthVersion> = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse versions: {}", e))?;

    Ok(versions)
}

/// Get available categories from Modrinth
#[tauri::command]
pub async fn get_modrinth_categories() -> Result<Vec<ModrinthCategory>, String> {
    let client = create_client()?;

    let url = format!("{}/tag/category", MODRINTH_API_BASE);

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to get categories: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Failed to get categories: HTTP {}",
            response.status()
        ));
    }

    let categories: Vec<ModrinthCategory> = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse categories: {}", e))?;

    Ok(categories)
}

/// Download a mod to a specific directory (for performance mods installation)
pub async fn download_mod_to_dir(
    project_slug: &str,
    game_version: &str,
    dest_dir: &std::path::Path,
) -> Result<String, String> {
    let client = create_client()?;

    // Get the appropriate version
    let versions = get_modrinth_versions(
        project_slug.to_string(),
        Some(game_version.to_string()),
        Some("fabric".to_string()),
    )
    .await?;

    let version = versions.first().ok_or_else(|| {
        format!(
            "No compatible version found for {} on {}",
            project_slug, game_version
        )
    })?;

    let file = version
        .files
        .iter()
        .find(|f| f.primary)
        .or_else(|| version.files.first())
        .ok_or("No download file found")?;

    // Create directory if needed
    tokio::fs::create_dir_all(dest_dir)
        .await
        .map_err(|e| format!("Failed to create directory: {}", e))?;

    let dest_path = dest_dir.join(&file.filename);

    // Check if already exists
    if dest_path.exists() {
        return Ok(format!("Already installed: {}", file.filename));
    }

    tracing::info!("Downloading {} to {:?}", file.filename, dest_path);

    // Download the file
    let response = client
        .get(&file.url)
        .send()
        .await
        .map_err(|e| format!("Failed to download: {}", e))?;

    if !response.status().is_success() {
        return Err(format!("Download failed: HTTP {}", response.status()));
    }

    let bytes = response
        .bytes()
        .await
        .map_err(|e| format!("Failed to read download: {}", e))?;

    // Verify size
    if bytes.len() < 1000 {
        return Err("Downloaded file is too small".to_string());
    }

    // Write file
    tokio::fs::write(&dest_path, bytes)
        .await
        .map_err(|e| format!("Failed to write file: {}", e))?;

    tracing::info!("Downloaded {} successfully", file.filename);
    Ok(file.filename.clone())
}

/// Download and install content from Modrinth
#[tauri::command]
pub async fn download_modrinth_content(
    app: tauri::AppHandle,
    project_slug: String,
    content_type: String,
    game_version: String,
    profile_id: Option<String>,
) -> Result<String, String> {
    let client = create_client()?;

    // Get the appropriate version
    let loader = if content_type == "mod" {
        Some("fabric".to_string())
    } else {
        None
    };
    let versions =
        get_modrinth_versions(project_slug.clone(), Some(game_version.clone()), loader).await?;

    let version = versions
        .first()
        .ok_or_else(|| format!("No compatible version found for {}", game_version))?;

    let file = version
        .files
        .iter()
        .find(|f| f.primary)
        .or_else(|| version.files.first())
        .ok_or("No download file found")?;

    // Determine destination directory based on content type
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| std::path::PathBuf::from("."))
        .join("MiracleClient");

    let dest_dir = match content_type.as_str() {
        "mod" => {
            if let Some(ref pid) = profile_id {
                // Get sanitized profile directory name
                let profile_dir = super::get_profile_dir_name(&app.state(), pid);
                game_dir.join("mods").join(&game_version).join(&profile_dir)
            } else {
                game_dir.join("mods").join(&game_version)
            }
        }
        "resourcepack" => {
            if let Some(ref pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), pid);
                game_dir
                    .join("resourcepacks")
                    .join(&game_version)
                    .join(&profile_dir)
            } else {
                game_dir.join("resourcepacks").join(&game_version)
            }
        }
        "shader" => {
            if let Some(ref pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), pid);
                game_dir
                    .join("shaderpacks")
                    .join(&game_version)
                    .join(&profile_dir)
            } else {
                game_dir.join("shaderpacks").join(&game_version)
            }
        }
        "datapack" => {
            if let Some(ref pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), pid);
                game_dir
                    .join("datapacks")
                    .join(&game_version)
                    .join(&profile_dir)
            } else {
                game_dir.join("datapacks").join(&game_version)
            }
        }
        _ => return Err(format!("Unknown content type: {}", content_type)),
    };

    // Create directory if needed
    tokio::fs::create_dir_all(&dest_dir)
        .await
        .map_err(|e| format!("Failed to create directory: {}", e))?;

    let dest_path = dest_dir.join(&file.filename);

    // Check if already exists
    if dest_path.exists() {
        return Ok(format!("Already installed: {}", file.filename));
    }

    tracing::info!("Downloading {} to {:?}", file.filename, dest_path);

    // Download the file
    let response = client
        .get(&file.url)
        .send()
        .await
        .map_err(|e| format!("Failed to download: {}", e))?;

    if !response.status().is_success() {
        return Err(format!("Download failed: HTTP {}", response.status()));
    }

    let bytes = response
        .bytes()
        .await
        .map_err(|e| format!("Failed to read download: {}", e))?;

    // Verify size
    if bytes.len() < 1000 {
        return Err("Downloaded file is too small".to_string());
    }

    // Write file
    tokio::fs::write(&dest_path, bytes)
        .await
        .map_err(|e| format!("Failed to write file: {}", e))?;

    tracing::info!("Downloaded {} successfully", file.filename);

    // Save mod metadata for update tracking (only for mods with a profile)
    if content_type == "mod" {
        if let Some(ref pid) = profile_id {
            let metadata = super::mod_updates::ModMetadata {
                source: "modrinth".to_string(),
                project_slug: project_slug.clone(),
                project_id: version.project_id.clone(),
                installed_version: version.version_number.clone(),
                version_id: version.id.clone(),
                installed_at: chrono::Utc::now().to_rfc3339(),
            };
            if let Err(e) = super::mod_updates::update_mod_metadata(
                &game_version,
                pid,
                &file.filename,
                metadata,
            ) {
                tracing::warn!("Failed to save mod metadata: {}", e);
            }
        }
    }

    Ok(format!("Installed: {}", file.filename))
}
