use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;
use uuid::Uuid;

/// Sanitize a profile name for use as a filesystem directory name.
/// Replaces spaces with underscores, removes special characters, and ensures valid path.
pub fn sanitize_profile_name(name: &str) -> String {
    let sanitized: String = name
        .chars()
        .map(|c| {
            if c.is_alphanumeric() || c == '-' || c == '_' {
                c
            } else if c == ' ' {
                '_'
            } else {
                '_'
            }
        })
        .collect();

    // Ensure not empty
    if sanitized.is_empty() {
        "profile".to_string()
    } else {
        sanitized
    }
}

/// Performance mods that are always included in every profile
pub const PERFORMANCE_MODS: &[&str] = &["sodium", "lithium", "ferrite-core", "modmenu"];

/// Official preset definitions
pub const PRESET_SKYBLOCK: &[&str] = &[
    "skyhanni",
    "skyblocker-liap",
    "neu",
    "xaeros-minimap",
    "xaeros-world-map",
];

pub const PRESET_PVP: &[&str] = &["ok-zoomer", "armor-hud"];

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Profile {
    pub id: String,
    pub name: String,
    pub version: String,
    pub is_default: bool,
    pub is_preset: bool,
    pub preset_type: Option<String>, // "skyblock", "pvp", or None for custom
    pub mods: Vec<String>,           // User-selected mod slugs (excludes performance mods)
    pub created_at: String,
}

impl Profile {
    pub fn new_default(version: &str) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            name: "Default".to_string(),
            version: version.to_string(),
            is_default: true,
            is_preset: false,
            preset_type: None,
            mods: Vec::new(),
            created_at: chrono::Utc::now().to_rfc3339(),
        }
    }

    pub fn new_custom(name: &str, version: &str) -> Self {
        Self {
            id: Uuid::new_v4().to_string(),
            name: name.to_string(),
            version: version.to_string(),
            is_default: false,
            is_preset: false,
            preset_type: None,
            mods: Vec::new(),
            created_at: chrono::Utc::now().to_rfc3339(),
        }
    }

    pub fn new_preset(name: &str, version: &str, preset_type: &str) -> Self {
        let mods = match preset_type {
            "skyblock" => PRESET_SKYBLOCK.iter().map(|s| s.to_string()).collect(),
            "pvp" => PRESET_PVP.iter().map(|s| s.to_string()).collect(),
            _ => Vec::new(),
        };

        Self {
            id: Uuid::new_v4().to_string(),
            name: name.to_string(),
            version: version.to_string(),
            is_default: false,
            is_preset: true,
            preset_type: Some(preset_type.to_string()),
            mods,
            created_at: chrono::Utc::now().to_rfc3339(),
        }
    }

    /// Get all mods for this profile (including performance mods)
    pub fn get_all_mods(&self) -> Vec<String> {
        let mut all_mods: Vec<String> = PERFORMANCE_MODS.iter().map(|s| s.to_string()).collect();
        all_mods.extend(self.mods.clone());
        all_mods
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ProfilesIndex {
    /// Map of version -> active profile ID
    pub active_profiles: HashMap<String, String>,
    /// All profiles, keyed by ID
    pub profiles: HashMap<String, Profile>,
}

pub struct ProfileManager {
    profiles_dir: PathBuf,
    index: ProfilesIndex,
}

impl ProfileManager {
    pub fn new() -> Self {
        let profiles_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient")
            .join("profiles");

        let mut manager = Self {
            profiles_dir,
            index: ProfilesIndex::default(),
        };

        // Load existing profiles
        if let Err(e) = manager.load() {
            tracing::warn!("Failed to load profiles: {}", e);
        }

        manager
    }

    fn index_path(&self) -> PathBuf {
        self.profiles_dir.join("profiles.json")
    }

    fn load(&mut self) -> Result<(), String> {
        let path = self.index_path();
        if path.exists() {
            let contents = std::fs::read_to_string(&path)
                .map_err(|e| format!("Failed to read profiles: {}", e))?;
            self.index = serde_json::from_str(&contents)
                .map_err(|e| format!("Failed to parse profiles: {}", e))?;
        }
        Ok(())
    }

    fn save(&self) -> Result<(), String> {
        std::fs::create_dir_all(&self.profiles_dir)
            .map_err(|e| format!("Failed to create profiles directory: {}", e))?;

        let contents = serde_json::to_string_pretty(&self.index)
            .map_err(|e| format!("Failed to serialize profiles: {}", e))?;

        std::fs::write(self.index_path(), contents)
            .map_err(|e| format!("Failed to write profiles: {}", e))?;

        Ok(())
    }

    /// Get the mods directory for a specific version and profile.
    /// Uses sanitized profile name for the directory (not UUID).
    pub fn get_mods_dir(&self, version: &str, profile_id: &str) -> PathBuf {
        // Look up profile to get the name, fallback to sanitized ID if not found
        let dir_name = if let Some(profile) = self.index.profiles.get(profile_id) {
            sanitize_profile_name(&profile.name)
        } else {
            sanitize_profile_name(profile_id)
        };

        dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient")
            .join("mods")
            .join(version)
            .join(dir_name)
    }

    /// Get the mods directory using a profile name directly (for when you have the name).
    pub fn get_mods_dir_by_name(version: &str, profile_name: &str) -> PathBuf {
        dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient")
            .join("mods")
            .join(version)
            .join(sanitize_profile_name(profile_name))
    }

    /// Get a profile by ID
    pub fn get_profile(&self, profile_id: &str) -> Option<&Profile> {
        self.index.profiles.get(profile_id)
    }

    /// Get all profiles for a specific version
    pub fn get_profiles(&self, version: &str) -> Vec<Profile> {
        self.index
            .profiles
            .values()
            .filter(|p| p.version == version)
            .cloned()
            .collect()
    }

    /// Get all profiles regardless of version
    pub fn get_all_profiles(&self) -> &HashMap<String, Profile> {
        &self.index.profiles
    }

    /// Get the active profile for a version, creating default if needed
    pub fn get_active_profile(&mut self, version: &str) -> Result<Profile, String> {
        // Check if there's an active profile for this version
        if let Some(active_id) = self.index.active_profiles.get(version) {
            if let Some(profile) = self.index.profiles.get(active_id) {
                return Ok(profile.clone());
            }
        }

        // No active profile, check if any profiles exist for this version
        let profiles = self.get_profiles(version);
        if let Some(profile) = profiles.first() {
            self.index
                .active_profiles
                .insert(version.to_string(), profile.id.clone());
            self.save()?;
            return Ok(profile.clone());
        }

        // No profiles exist, create default
        let default_profile = Profile::new_default(version);
        self.index
            .profiles
            .insert(default_profile.id.clone(), default_profile.clone());
        self.index
            .active_profiles
            .insert(version.to_string(), default_profile.id.clone());
        self.save()?;

        Ok(default_profile)
    }

    /// Set the active profile for a version
    pub fn set_active_profile(&mut self, version: &str, profile_id: &str) -> Result<(), String> {
        // Verify the profile exists
        if !self.index.profiles.contains_key(profile_id) {
            return Err(format!("Profile {} not found", profile_id));
        }

        self.index
            .active_profiles
            .insert(version.to_string(), profile_id.to_string());
        self.save()
    }

    /// Create a new custom profile
    pub fn create_profile(
        &mut self,
        name: &str,
        version: &str,
        base_profile_id: Option<&str>,
    ) -> Result<Profile, String> {
        let mut profile = Profile::new_custom(name, version);

        // Copy mods from base profile if specified
        if let Some(base_id) = base_profile_id {
            if let Some(base_profile) = self.index.profiles.get(base_id) {
                profile.mods = base_profile.mods.clone();
            }
        }

        self.index
            .profiles
            .insert(profile.id.clone(), profile.clone());
        self.save()?;

        Ok(profile)
    }

    /// Create a preset profile
    pub fn create_preset(&mut self, version: &str, preset_type: &str) -> Result<Profile, String> {
        let name = match preset_type {
            "skyblock" => "Skyblock",
            "pvp" => "PvP",
            _ => return Err(format!("Unknown preset type: {}", preset_type)),
        };

        let profile = Profile::new_preset(name, version, preset_type);
        self.index
            .profiles
            .insert(profile.id.clone(), profile.clone());
        self.save()?;

        Ok(profile)
    }

    /// Create a profile for a modpack installation
    pub fn create_modpack_profile(
        &mut self,
        name: &str,
        version: &str,
        source: &str,
    ) -> Result<Profile, String> {
        let mut profile = Profile::new_custom(name, version);
        // Mark as a modpack profile by setting preset_type to the source
        profile.preset_type = Some(format!("modpack:{}", source));

        self.index
            .profiles
            .insert(profile.id.clone(), profile.clone());
        self.save()?;

        Ok(profile)
    }

    /// Delete a profile
    pub fn delete_profile(&mut self, profile_id: &str) -> Result<(), String> {
        let profile = self
            .index
            .profiles
            .get(profile_id)
            .ok_or_else(|| format!("Profile {} not found", profile_id))?;

        if profile.is_default {
            return Err("Cannot delete the default profile".to_string());
        }

        let version = profile.version.clone();
        self.index.profiles.remove(profile_id);

        // If this was the active profile, switch to default
        if self.index.active_profiles.get(&version) == Some(&profile_id.to_string()) {
            // Find the default profile for this version
            if let Some(default_profile) = self
                .index
                .profiles
                .values()
                .find(|p| p.version == version && p.is_default)
            {
                self.index
                    .active_profiles
                    .insert(version.clone(), default_profile.id.clone());
            }
        }

        self.save()?;

        // Also delete the mods folder for this profile
        let mods_dir = self.get_mods_dir(&version, profile_id);
        if mods_dir.exists() {
            let _ = std::fs::remove_dir_all(&mods_dir);
        }

        Ok(())
    }

    /// Duplicate a profile
    pub fn duplicate_profile(
        &mut self,
        profile_id: &str,
        new_name: &str,
    ) -> Result<Profile, String> {
        let source = self
            .index
            .profiles
            .get(profile_id)
            .ok_or_else(|| format!("Profile {} not found", profile_id))?
            .clone();

        let mut new_profile = Profile::new_custom(new_name, &source.version);
        new_profile.mods = source.mods.clone();

        self.index
            .profiles
            .insert(new_profile.id.clone(), new_profile.clone());
        self.save()?;

        // Copy the mods folder
        let source_dir = self.get_mods_dir(&source.version, profile_id);
        let dest_dir = self.get_mods_dir(&source.version, &new_profile.id);

        if source_dir.exists() {
            std::fs::create_dir_all(&dest_dir)
                .map_err(|e| format!("Failed to create profile mods directory: {}", e))?;

            for entry in std::fs::read_dir(&source_dir).map_err(|e| e.to_string())? {
                if let Ok(entry) = entry {
                    let dest_path = dest_dir.join(entry.file_name());
                    let _ = std::fs::copy(entry.path(), dest_path);
                }
            }
        }

        Ok(new_profile)
    }

    /// Export a profile to a shareable format
    pub fn export_profile(&self, profile_id: &str) -> Result<ProfileExport, String> {
        let profile = self
            .index
            .profiles
            .get(profile_id)
            .ok_or_else(|| format!("Profile {} not found", profile_id))?;

        Ok(ProfileExport {
            name: profile.name.clone(),
            version: profile.version.clone(),
            mods: profile.get_all_mods(),
        })
    }

    /// Import a profile from exported format
    pub fn import_profile(
        &mut self,
        export: ProfileExport,
        version: &str,
    ) -> Result<Profile, String> {
        let mut profile = Profile::new_custom(&export.name, version);

        // Filter out performance mods (they're auto-included)
        profile.mods = export
            .mods
            .into_iter()
            .filter(|m| !PERFORMANCE_MODS.contains(&m.as_str()))
            .collect();

        self.index
            .profiles
            .insert(profile.id.clone(), profile.clone());
        self.save()?;

        Ok(profile)
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProfileExport {
    pub name: String,
    pub version: String,
    pub mods: Vec<String>,
}

impl Default for ProfileManager {
    fn default() -> Self {
        Self::new()
    }
}
