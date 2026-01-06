use reqwest::header::{HeaderMap, HeaderValue, ACCEPT};
use serde::{Deserialize, Serialize};
use tauri::Manager;

const CURSEFORGE_API_KEY: &str = "$2a$10$JerFj3jTqK5z2SJlzO4i.e0/7O3wSdh27GyM4vHIRinf7VJvuJnfe";
const CURSEFORGE_API_BASE: &str = "https://api.curseforge.com/v1";
const MINECRAFT_GAME_ID: i32 = 432;
const FABRIC_MOD_LOADER_TYPE: i32 = 4;

// CurseForge class IDs for different content types
const CLASS_MODS: i32 = 6;
const CLASS_RESOURCE_PACKS: i32 = 12;
const CLASS_SHADERS: i32 = 6552;
const CLASS_DATA_PACKS: i32 = 6945;
const CLASS_MODPACKS: i32 = 4471;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeSearchResult {
    pub id: i32,
    pub name: String,
    #[serde(default)]
    pub slug: String,
    #[serde(default)]
    pub summary: String,
    #[serde(alias = "downloadCount", default)]
    pub downloads: i64,
    #[serde(rename = "dateCreated", default)]
    pub date_created: Option<String>,
    #[serde(rename = "dateModified", default)]
    pub date_modified: Option<String>,
    #[serde(rename = "dateReleased", default)]
    pub date_released: Option<String>,
    #[serde(default)]
    pub categories: Vec<CurseForgeCategory>,
    #[serde(default)]
    pub authors: Vec<CurseForgeAuthor>,
    pub logo: Option<CurseForgeLogo>,
    #[serde(rename = "classId", default)]
    pub class_id: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeCategory {
    #[serde(default)]
    pub id: i32,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub slug: String,
    #[serde(rename = "classId")]
    pub class_id: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeAuthor {
    #[serde(default)]
    pub id: i32,
    #[serde(default)]
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeLogo {
    #[serde(default)]
    pub url: String,
    #[serde(rename = "thumbnailUrl", default)]
    pub thumbnail_url: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgeSearchResponse {
    pub data: Vec<CurseForgeSearchResult>,
    pub pagination: CurseForgePagination,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CurseForgePagination {
    pub index: i32,
    #[serde(rename = "pageSize")]
    pub page_size: i32,
    #[serde(rename = "resultCount")]
    pub result_count: i32,
    #[serde(rename = "totalCount")]
    pub total_count: i32,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CurseForgeFile {
    pub id: i32,
    #[serde(rename = "fileName")]
    pub file_name: String,
    #[serde(rename = "downloadUrl")]
    pub download_url: Option<String>,
    #[serde(rename = "gameVersions")]
    pub game_versions: Vec<String>,
    #[serde(rename = "modLoader")]
    pub mod_loader: Option<i32>,
}

#[derive(Debug, Serialize, Deserialize)]
struct CurseForgeFilesResponse {
    data: Vec<CurseForgeFile>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ModCompatibility {
    pub mod_id: String,
    pub mod_name: String,
    pub compatible: bool,
}

fn create_curseforge_client() -> Result<reqwest::Client, String> {
    let mut headers = HeaderMap::new();
    headers.insert("x-api-key", HeaderValue::from_static(CURSEFORGE_API_KEY));
    headers.insert(ACCEPT, HeaderValue::from_static("application/json"));

    reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(120))
        .user_agent("MiracleClient/1.0")
        .default_headers(headers)
        .build()
        .map_err(|e| e.to_string())
}

/// Get a specific file by project ID and file ID
pub async fn get_file_by_id(project_id: i64, file_id: i64) -> Result<CurseForgeFile, String> {
    let client = create_curseforge_client()?;

    let url = format!(
        "{}/mods/{}/files/{}",
        CURSEFORGE_API_BASE, project_id, file_id
    );

    tracing::info!(
        "Fetching CurseForge file: project={} file={}",
        project_id,
        file_id
    );

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch file: {}", e))?;

    if !response.status().is_success() {
        return Err(format!("Failed to fetch file: HTTP {}", response.status()));
    }

    #[derive(Deserialize)]
    struct FileResponse {
        data: CurseForgeFile,
    }

    let file_response: FileResponse = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse file response: {}", e))?;

    Ok(file_response.data)
}

/// Download a file directly by URL, returns the bytes
pub async fn download_file_bytes(url: &str) -> Result<Vec<u8>, String> {
    let client = create_curseforge_client()?;

    let response = client
        .get(url)
        .send()
        .await
        .map_err(|e| format!("Failed to download: {}", e))?;

    if !response.status().is_success() {
        return Err(format!("Download failed: HTTP {}", response.status()));
    }

    let bytes = response
        .bytes()
        .await
        .map_err(|e| format!("Failed to read bytes: {}", e))?;

    Ok(bytes.to_vec())
}

pub async fn get_mod_files(
    project_id: i32,
    minecraft_version: &str,
) -> Result<Vec<CurseForgeFile>, String> {
    let client = create_curseforge_client()?;

    let url = format!("{}/mods/{}/files", CURSEFORGE_API_BASE, project_id);

    tracing::info!(
        "Fetching CurseForge files for project {} (MC {})",
        project_id,
        minecraft_version
    );

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to fetch mod files: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "Failed to fetch mod files: HTTP {}",
            response.status()
        ));
    }

    let files_response: CurseForgeFilesResponse = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse files response: {}", e))?;

    // Filter files that match the Minecraft version and are for Fabric
    let compatible_files: Vec<CurseForgeFile> = files_response
        .data
        .into_iter()
        .filter(|file| {
            file.game_versions.contains(&minecraft_version.to_string())
                && file
                    .mod_loader
                    .map_or(false, |loader| loader == FABRIC_MOD_LOADER_TYPE)
        })
        .collect();

    Ok(compatible_files)
}

pub async fn check_mod_compatibility(
    project_id: i32,
    minecraft_version: &str,
) -> Result<bool, String> {
    let files = get_mod_files(project_id, minecraft_version).await?;
    Ok(!files.is_empty())
}

/// Search CurseForge for content
#[tauri::command]
pub async fn search_curseforge(
    query: String,
    content_type: String, // "mod", "resourcepack", "shader", "datapack"
    category: Option<i32>,
    sort: String, // "popularity", "downloads", "updated", "name"
    version: String,
    offset: u32,
    limit: u32,
) -> Result<CurseForgeSearchResponse, String> {
    let client = create_curseforge_client()?;

    // Map content type to class ID
    let class_id = match content_type.as_str() {
        "mod" => CLASS_MODS,
        "resourcepack" => CLASS_RESOURCE_PACKS,
        "shader" => CLASS_SHADERS,
        "datapack" => CLASS_DATA_PACKS,
        "modpack" => CLASS_MODPACKS,
        _ => CLASS_MODS,
    };

    // Map sort parameter to CurseForge sort field
    let sort_field = match sort.as_str() {
        "downloads" => 2,  // TotalDownloads
        "updated" => 3,    // LastUpdated
        "name" => 4,       // Name
        "popularity" => 6, // Popularity
        _ => 2,            // Default to downloads
    };

    let mut url = format!(
        "{}/mods/search?gameId={}&classId={}&sortField={}&sortOrder=desc&index={}&pageSize={}",
        CURSEFORGE_API_BASE,
        MINECRAFT_GAME_ID,
        class_id,
        sort_field,
        offset,
        limit.min(50) // CurseForge max is 50
    );

    // Add search query if provided
    if !query.is_empty() {
        url = format!("{}&searchFilter={}", url, urlencoding::encode(&query));
    }

    // Add version filter
    if !version.is_empty() {
        url = format!("{}&gameVersion={}", url, urlencoding::encode(&version));
    }

    // Add category filter
    if let Some(cat_id) = category {
        url = format!("{}&categoryId={}", url, cat_id);
    }

    // For mods (not modpacks), filter to Fabric only
    if content_type == "mod" {
        url = format!("{}&modLoaderType={}", url, FABRIC_MOD_LOADER_TYPE);
    }
    // For modpacks, also filter to Fabric
    if content_type == "modpack" {
        url = format!("{}&modLoaderType={}", url, FABRIC_MOD_LOADER_TYPE);
    }

    tracing::info!("Searching CurseForge: {}", url);

    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| format!("Failed to search CurseForge: {}", e))?;

    if !response.status().is_success() {
        return Err(format!(
            "CurseForge search failed: HTTP {}",
            response.status()
        ));
    }

    let response_text = response
        .text()
        .await
        .map_err(|e| format!("Failed to read CurseForge response: {}", e))?;

    let search_result: CurseForgeSearchResponse =
        serde_json::from_str(&response_text).map_err(|e| {
            tracing::error!(
                "CurseForge parse error: {}. Response preview: {}",
                e,
                &response_text[..response_text.len().min(500)]
            );
            format!("Failed to parse CurseForge response: {}", e)
        })?;

    Ok(search_result)
}

/// Download and install content from CurseForge
#[tauri::command]
pub async fn download_curseforge_content(
    app: tauri::AppHandle,
    project_id: i32,
    content_type: String,
    game_version: String,
    profile_id: Option<String>,
) -> Result<String, String> {
    let game_dir = dirs::data_dir()
        .unwrap_or_else(|| std::path::PathBuf::from("."))
        .join("MiracleClient");

    let dest_dir = match content_type.as_str() {
        "mod" => {
            if let Some(pid) = profile_id {
                // Get sanitized profile directory name
                let profile_dir = super::get_profile_dir_name(&app.state(), &pid);
                game_dir.join("mods").join(&game_version).join(&profile_dir)
            } else {
                game_dir.join("mods").join(&game_version)
            }
        }
        "resourcepack" => {
            if let Some(pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), &pid);
                game_dir
                    .join("resourcepacks")
                    .join(&game_version)
                    .join(&profile_dir)
            } else {
                game_dir.join("resourcepacks").join(&game_version)
            }
        }
        "shader" => {
            if let Some(pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), &pid);
                game_dir
                    .join("shaderpacks")
                    .join(&game_version)
                    .join(&profile_dir)
            } else {
                game_dir.join("shaderpacks").join(&game_version)
            }
        }
        "datapack" => {
            if let Some(pid) = profile_id {
                let profile_dir = super::get_profile_dir_name(&app.state(), &pid);
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

    download_curseforge_mod(project_id, &game_version, &dest_dir).await
}

pub async fn download_curseforge_mod(
    project_id: i32,
    minecraft_version: &str,
    mods_dir: &std::path::Path,
) -> Result<String, String> {
    let files = get_mod_files(project_id, minecraft_version).await?;

    if files.is_empty() {
        return Err(format!(
            "No compatible version found for Minecraft {} (Fabric)",
            minecraft_version
        ));
    }

    // Get the latest file (files are usually sorted by date, newest first)
    let latest_file = files.first().ok_or("No files found")?;

    let download_url = latest_file
        .download_url
        .as_ref()
        .ok_or("No download URL available for this file")?;

    let mod_path = mods_dir.join(&latest_file.file_name);

    // Check if already downloaded
    if mod_path.exists() {
        return Ok(format!("Mod already installed: {}", latest_file.file_name));
    }

    tracing::info!("Downloading CurseForge mod: {}", latest_file.file_name);

    let client = create_curseforge_client()?;
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

    tracing::info!(
        "CurseForge mod downloaded successfully: {}",
        latest_file.file_name
    );
    Ok(format!("Successfully installed: {}", latest_file.file_name))
}
