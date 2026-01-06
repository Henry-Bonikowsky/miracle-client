use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use tauri::{AppHandle, Manager};

/// Metadata for a saved clip
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClipInfo {
    pub id: String,
    pub filename: String,
    pub path: String,
    pub thumbnail_path: Option<String>,
    pub duration_ms: u64,
    pub size_bytes: u64,
    pub created_at: i64,
    pub width: u32,
    pub height: u32,
}

/// Get the clips directory path
fn get_clips_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("MiracleClient")
        .join("clips")
}

/// List all clips in the clips directory
#[tauri::command]
pub async fn list_clips() -> Result<Vec<ClipInfo>, String> {
    let clips_dir = get_clips_dir();

    // Create directory if it doesn't exist
    if !clips_dir.exists() {
        fs::create_dir_all(&clips_dir)
            .map_err(|e| format!("Failed to create clips directory: {}", e))?;
        return Ok(Vec::new());
    }

    let mut clips = Vec::new();

    let entries =
        fs::read_dir(&clips_dir).map_err(|e| format!("Failed to read clips directory: {}", e))?;

    for entry in entries {
        let entry = match entry {
            Ok(e) => e,
            Err(_) => continue,
        };

        let path = entry.path();

        // Only process video files
        if let Some(ext) = path.extension() {
            let ext_str = ext.to_string_lossy().to_lowercase();
            if ext_str != "mp4" && ext_str != "webm" && ext_str != "mkv" {
                continue;
            }
        } else {
            continue;
        }

        let metadata = match fs::metadata(&path) {
            Ok(m) => m,
            Err(_) => continue,
        };

        let filename = path
            .file_name()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_default();

        let id = path
            .file_stem()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_else(|| filename.clone());

        // Check for thumbnail
        let thumbnail_path = {
            let thumb = path.with_extension("jpg");
            if thumb.exists() {
                Some(thumb.to_string_lossy().to_string())
            } else {
                let thumb_png = path.with_extension("png");
                if thumb_png.exists() {
                    Some(thumb_png.to_string_lossy().to_string())
                } else {
                    None
                }
            }
        };

        // Try to read metadata from JSON file if exists
        let json_path = path.with_extension("json");
        let (duration_ms, width, height) = if json_path.exists() {
            match fs::read_to_string(&json_path) {
                Ok(content) => match serde_json::from_str::<serde_json::Value>(&content) {
                    Ok(json) => {
                        let dur = json
                            .get("duration_ms")
                            .and_then(|v| v.as_u64())
                            .unwrap_or(0);
                        let w = json.get("width").and_then(|v| v.as_u64()).unwrap_or(1920) as u32;
                        let h = json.get("height").and_then(|v| v.as_u64()).unwrap_or(1080) as u32;
                        (dur, w, h)
                    }
                    Err(_) => (0, 1920, 1080),
                },
                Err(_) => (0, 1920, 1080),
            }
        } else {
            // Default values if no metadata file
            (0, 1920, 1080)
        };

        let created_at = metadata
            .created()
            .or_else(|_| metadata.modified())
            .map(|t| {
                t.duration_since(std::time::UNIX_EPOCH)
                    .unwrap_or_default()
                    .as_secs() as i64
            })
            .unwrap_or(0);

        clips.push(ClipInfo {
            id,
            filename,
            path: path.to_string_lossy().to_string(),
            thumbnail_path,
            duration_ms,
            size_bytes: metadata.len(),
            created_at,
            width,
            height,
        });
    }

    // Sort by creation time, newest first
    clips.sort_by(|a, b| b.created_at.cmp(&a.created_at));

    Ok(clips)
}

/// Get metadata for a specific clip
#[tauri::command]
pub async fn get_clip_metadata(clip_id: String) -> Result<ClipInfo, String> {
    let clips = list_clips().await?;

    clips
        .into_iter()
        .find(|c| c.id == clip_id)
        .ok_or_else(|| format!("Clip not found: {}", clip_id))
}

/// Delete a clip and its associated files
#[tauri::command]
pub async fn delete_clip(clip_id: String) -> Result<(), String> {
    let clips_dir = get_clips_dir();

    // Find all files matching the clip ID
    let entries =
        fs::read_dir(&clips_dir).map_err(|e| format!("Failed to read clips directory: {}", e))?;

    let mut deleted_any = false;

    for entry in entries {
        let entry = match entry {
            Ok(e) => e,
            Err(_) => continue,
        };

        let path = entry.path();
        let file_stem = path
            .file_stem()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_default();

        if file_stem == clip_id {
            if let Err(e) = fs::remove_file(&path) {
                tracing::warn!("Failed to delete {}: {}", path.display(), e);
            } else {
                deleted_any = true;
                tracing::info!("Deleted clip file: {}", path.display());
            }
        }
    }

    if deleted_any {
        Ok(())
    } else {
        Err(format!("No files found for clip: {}", clip_id))
    }
}

/// Get the clips directory path
#[tauri::command]
pub async fn get_clips_directory() -> Result<String, String> {
    let clips_dir = get_clips_dir();

    // Create directory if it doesn't exist
    if !clips_dir.exists() {
        fs::create_dir_all(&clips_dir)
            .map_err(|e| format!("Failed to create clips directory: {}", e))?;
    }

    Ok(clips_dir.to_string_lossy().to_string())
}

/// Open the clips folder in the system file explorer
#[tauri::command]
pub async fn open_clips_folder() -> Result<(), String> {
    let clips_dir = get_clips_dir();

    // Create directory if it doesn't exist
    if !clips_dir.exists() {
        fs::create_dir_all(&clips_dir)
            .map_err(|e| format!("Failed to create clips directory: {}", e))?;
    }

    // Open in file explorer
    #[cfg(target_os = "windows")]
    {
        std::process::Command::new("explorer")
            .arg(&clips_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    #[cfg(target_os = "macos")]
    {
        std::process::Command::new("open")
            .arg(&clips_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    #[cfg(target_os = "linux")]
    {
        std::process::Command::new("xdg-open")
            .arg(&clips_dir)
            .spawn()
            .map_err(|e| format!("Failed to open folder: {}", e))?;
    }

    Ok(())
}

/// Watch the clips directory for changes (returns current clips, frontend can poll)
#[tauri::command]
pub async fn refresh_clips() -> Result<Vec<ClipInfo>, String> {
    list_clips().await
}
