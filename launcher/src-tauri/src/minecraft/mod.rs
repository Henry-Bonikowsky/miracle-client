use reqwest::Client;
use serde::{Deserialize, Serialize};
use sha1::{Digest, Sha1};
use std::collections::HashMap;
use std::path::PathBuf;
use thiserror::Error;
use tokio::fs;

const VERSION_MANIFEST_URL: &str =
    "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
const FABRIC_META_URL: &str = "https://meta.fabricmc.net/v2";
const RESOURCES_URL: &str = "https://resources.download.minecraft.net";

#[derive(Error, Debug)]
pub enum MinecraftError {
    #[error("HTTP request failed: {0}")]
    HttpError(#[from] reqwest::Error),
    #[error("IO error: {0}")]
    IoError(#[from] std::io::Error),
    #[error("JSON parse error: {0}")]
    JsonError(#[from] serde_json::Error),
    #[error("Version not found: {0}")]
    VersionNotFound(String),
    #[error("Download failed: {0}")]
    DownloadFailed(String),
    #[error("Launch failed: {0}")]
    LaunchFailed(String),
    #[error("Java not found")]
    JavaNotFound,
}

// ==================== Version Manifest ====================

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GameVersion {
    pub id: String,
    #[serde(rename = "type")]
    pub version_type: String,
    #[serde(rename = "releaseTime")]
    pub release_time: String,
    #[serde(default)]
    pub url: String,
    #[serde(default)]
    pub sha1: String,
}

#[derive(Debug, Deserialize)]
struct VersionManifest {
    versions: Vec<GameVersion>,
}

// ==================== Version Details ====================

#[derive(Debug, Deserialize)]
struct VersionDetails {
    id: String,
    downloads: VersionDownloads,
    libraries: Vec<Library>,
    #[serde(rename = "assetIndex")]
    asset_index: AssetIndexInfo,
    assets: String,
    #[serde(rename = "mainClass")]
    main_class: String,
    #[serde(rename = "minecraftArguments", default)]
    minecraft_arguments: Option<String>,
    #[serde(default)]
    arguments: Option<Arguments>,
}

#[derive(Debug, Deserialize)]
struct Arguments {
    #[serde(default)]
    game: Vec<serde_json::Value>,
    #[serde(default)]
    jvm: Vec<serde_json::Value>,
}

#[derive(Debug, Deserialize)]
struct VersionDownloads {
    client: DownloadInfo,
}

#[derive(Debug, Deserialize, Clone)]
struct DownloadInfo {
    sha1: String,
    size: u64,
    url: String,
}

// ==================== Libraries ====================

#[derive(Debug, Deserialize)]
struct Library {
    name: String,
    downloads: Option<LibraryDownloads>,
    #[serde(default)]
    rules: Option<Vec<Rule>>,
    #[serde(default)]
    natives: Option<HashMap<String, String>>,
}

#[derive(Debug, Deserialize)]
struct LibraryDownloads {
    artifact: Option<Artifact>,
    #[serde(default)]
    classifiers: Option<HashMap<String, Artifact>>,
}

#[derive(Debug, Deserialize, Clone)]
struct Artifact {
    path: String,
    sha1: String,
    size: u64,
    url: String,
}

#[derive(Debug, Deserialize)]
struct Rule {
    action: String,
    #[serde(default)]
    os: Option<OsRule>,
}

#[derive(Debug, Deserialize)]
struct OsRule {
    #[serde(default)]
    name: Option<String>,
    #[serde(default)]
    arch: Option<String>,
}

// ==================== Assets ====================

#[derive(Debug, Deserialize)]
struct AssetIndexInfo {
    id: String,
    sha1: String,
    size: u64,
    url: String,
    #[serde(rename = "totalSize")]
    total_size: u64,
}

#[derive(Debug, Deserialize)]
struct AssetIndex {
    objects: HashMap<String, AssetObject>,
}

#[derive(Debug, Deserialize)]
struct AssetObject {
    hash: String,
    size: u64,
}

// ==================== Fabric ====================

#[derive(Debug, Deserialize)]
struct FabricVersionInfo {
    loader: FabricLoader,
}

#[derive(Debug, Deserialize)]
struct FabricLoader {
    version: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct FabricProfile {
    id: String,
    #[serde(rename = "mainClass")]
    main_class: String,
    libraries: Vec<FabricLibrary>,
    #[serde(default)]
    arguments: Option<FabricArguments>,
}

#[derive(Debug, Serialize, Deserialize)]
struct FabricLibrary {
    name: String,
    url: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
struct FabricArguments {
    #[serde(default)]
    game: Vec<String>,
    #[serde(default)]
    jvm: Vec<String>,
}

// ==================== Manager ====================

pub struct MinecraftManager {
    client: Client,
    game_dir: PathBuf,
}

impl MinecraftManager {
    pub fn new() -> Self {
        let game_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("MiracleClient");

        Self {
            client: Client::builder()
                .user_agent("MiracleClient/1.0")
                .build()
                .unwrap_or_else(|_| Client::new()),
            game_dir,
        }
    }

    pub fn game_dir(&self) -> &PathBuf {
        &self.game_dir
    }

    // ==================== Version Management ====================

    pub async fn get_versions(&self) -> Result<Vec<GameVersion>, MinecraftError> {
        tracing::info!("Fetching version manifest...");
        let manifest: VersionManifest = self
            .client
            .get(VERSION_MANIFEST_URL)
            .send()
            .await?
            .json()
            .await?;

        let versions: Vec<GameVersion> = manifest
            .versions
            .into_iter()
            .filter(|v| v.version_type == "release" && v.id.starts_with("1.2"))
            .collect();

        tracing::info!("Found {} versions", versions.len());
        Ok(versions)
    }

    async fn get_version_details(&self, version: &str) -> Result<VersionDetails, MinecraftError> {
        // First get the manifest to find the version URL
        let manifest: VersionManifest = self
            .client
            .get(VERSION_MANIFEST_URL)
            .send()
            .await?
            .json()
            .await?;

        let version_info = manifest
            .versions
            .iter()
            .find(|v| v.id == version)
            .ok_or_else(|| MinecraftError::VersionNotFound(version.to_string()))?;

        // Download version details
        let details: VersionDetails = self
            .client
            .get(&version_info.url)
            .send()
            .await?
            .json()
            .await?;

        Ok(details)
    }

    // ==================== Download Minecraft ====================

    pub async fn download_minecraft<F>(
        &self,
        version: &str,
        progress_callback: F,
    ) -> Result<(), MinecraftError>
    where
        F: Fn(u64, u64, &str) + Send + Sync,
    {
        tracing::info!("Starting Minecraft {} download", version);

        // Create directories
        let versions_dir = self.game_dir.join("versions").join(version);
        fs::create_dir_all(&versions_dir).await?;

        let libraries_dir = self.game_dir.join("libraries");
        fs::create_dir_all(&libraries_dir).await?;

        // Get version details
        progress_callback(0, 100, "Fetching version info...");
        let details = self.get_version_details(version).await?;

        // Save version JSON
        let version_json_path = versions_dir.join(format!("{}.json", version));
        let version_json = serde_json::to_string_pretty(&serde_json::json!({
            "id": details.id,
            "mainClass": details.main_class,
            "assets": details.assets,
        }))?;
        fs::write(&version_json_path, version_json).await?;

        // Download client JAR
        progress_callback(5, 100, "Downloading client...");
        let client_path = versions_dir.join(format!("{}.jar", version));
        self.download_file_verified(
            &details.downloads.client.url,
            &client_path,
            &details.downloads.client.sha1,
        )
        .await?;
        tracing::info!("Downloaded client JAR");

        // Download libraries
        let total_libs = details.libraries.len();
        for (i, lib) in details.libraries.iter().enumerate() {
            let progress = 10 + (i * 60 / total_libs) as u64;

            if !self.should_use_library(lib) {
                continue;
            }

            if let Some(downloads) = &lib.downloads {
                // Download main artifact
                if let Some(artifact) = &downloads.artifact {
                    let lib_path = libraries_dir.join(&artifact.path);
                    if !lib_path.exists() {
                        progress_callback(progress, 100, &format!("Downloading {}", lib.name));
                        if let Some(parent) = lib_path.parent() {
                            fs::create_dir_all(parent).await?;
                        }
                        self.download_file_verified(&artifact.url, &lib_path, &artifact.sha1)
                            .await?;
                    }
                }

                // Download natives if needed
                if let Some(natives) = &lib.natives {
                    if let Some(native_key) = natives.get("windows") {
                        if let Some(classifiers) = &downloads.classifiers {
                            let native_key = native_key.replace("${arch}", "64");
                            if let Some(native_artifact) = classifiers.get(&native_key) {
                                let native_path = libraries_dir.join(&native_artifact.path);
                                if !native_path.exists() {
                                    progress_callback(
                                        progress,
                                        100,
                                        &format!("Downloading native {}", lib.name),
                                    );
                                    if let Some(parent) = native_path.parent() {
                                        fs::create_dir_all(parent).await?;
                                    }
                                    self.download_file_verified(
                                        &native_artifact.url,
                                        &native_path,
                                        &native_artifact.sha1,
                                    )
                                    .await?;
                                }
                            }
                        }
                    }
                }
            }
        }
        tracing::info!("Downloaded all libraries");

        // Download assets
        progress_callback(70, 100, "Downloading assets...");
        self.download_assets(&details.asset_index, |current, total| {
            let asset_progress = 70 + (current * 25 / total.max(1));
            progress_callback(asset_progress, 100, "Downloading assets...");
        })
        .await?;

        progress_callback(100, 100, "Download complete!");
        tracing::info!("Minecraft {} download complete", version);

        Ok(())
    }

    fn should_use_library(&self, lib: &Library) -> bool {
        if let Some(rules) = &lib.rules {
            let mut dominated = None;

            for rule in rules {
                let dominated_match = match &rule.os {
                    None => true,
                    Some(os_rule) => os_rule.name.as_deref() == Some("windows"),
                };

                if dominated_match {
                    dominated = Some(rule.action == "allow");
                }
            }

            return dominated.unwrap_or(false);
        }
        true
    }

    async fn download_assets<F>(
        &self,
        asset_index: &AssetIndexInfo,
        progress: F,
    ) -> Result<(), MinecraftError>
    where
        F: Fn(u64, u64),
    {
        let assets_dir = self.game_dir.join("assets");
        let indexes_dir = assets_dir.join("indexes");
        let objects_dir = assets_dir.join("objects");

        fs::create_dir_all(&indexes_dir).await?;
        fs::create_dir_all(&objects_dir).await?;

        // Download asset index
        let index_path = indexes_dir.join(format!("{}.json", asset_index.id));
        if !index_path.exists() {
            self.download_file_verified(&asset_index.url, &index_path, &asset_index.sha1)
                .await?;
        }

        // Parse asset index
        let index_content = fs::read_to_string(&index_path).await?;
        let index: AssetIndex = serde_json::from_str(&index_content)?;

        let total = index.objects.len() as u64;
        let mut current = 0u64;

        for (_name, object) in &index.objects {
            let hash_prefix = &object.hash[..2];
            let object_dir = objects_dir.join(hash_prefix);
            let object_path = object_dir.join(&object.hash);

            if !object_path.exists() {
                fs::create_dir_all(&object_dir).await?;
                let url = format!("{}/{}/{}", RESOURCES_URL, hash_prefix, object.hash);
                self.download_file(&url, &object_path).await?;
            }

            current += 1;
            progress(current, total);
        }

        tracing::info!("Downloaded {} assets", total);
        Ok(())
    }

    // ==================== Fabric ====================

    pub async fn get_fabric_loader(&self, mc_version: &str) -> Result<String, MinecraftError> {
        let url = format!("{}/versions/loader/{}", FABRIC_META_URL, mc_version);
        let versions: Vec<FabricVersionInfo> = self.client.get(&url).send().await?.json().await?;

        versions
            .first()
            .map(|v| v.loader.version.clone())
            .ok_or_else(|| MinecraftError::VersionNotFound("Fabric loader".to_string()))
    }

    pub async fn download_fabric<F>(
        &self,
        mc_version: &str,
        loader_version: &str,
        progress_callback: F,
    ) -> Result<(), MinecraftError>
    where
        F: Fn(u64, u64, &str),
    {
        tracing::info!("Installing Fabric {} for MC {}", loader_version, mc_version);
        progress_callback(0, 100, "Fetching Fabric profile...");

        // Download Fabric profile
        let profile_url = format!(
            "{}/versions/loader/{}/{}/profile/json",
            FABRIC_META_URL, mc_version, loader_version
        );

        let profile: FabricProfile = self.client.get(&profile_url).send().await?.json().await?;

        // Save Fabric profile
        let fabric_id = format!("fabric-loader-{}-{}", loader_version, mc_version);
        let fabric_dir = self.game_dir.join("versions").join(&fabric_id);
        fs::create_dir_all(&fabric_dir).await?;

        let profile_path = fabric_dir.join(format!("{}.json", fabric_id));
        let profile_json = serde_json::to_string_pretty(&profile)?;
        fs::write(&profile_path, &profile_json).await?;

        // Download Fabric libraries
        let libraries_dir = self.game_dir.join("libraries");
        let total = profile.libraries.len();

        for (i, lib) in profile.libraries.iter().enumerate() {
            let progress = ((i + 1) * 100 / total) as u64;
            progress_callback(progress, 100, &format!("Installing {}", lib.name));

            let lib_path = self.maven_to_path(&lib.name);
            let full_path = libraries_dir.join(&lib_path);

            if !full_path.exists() {
                if let Some(parent) = full_path.parent() {
                    fs::create_dir_all(parent).await?;
                }

                let base_url = lib.url.as_deref().unwrap_or("https://maven.fabricmc.net/");
                let url = format!("{}{}", base_url, lib_path);

                if let Err(e) = self.download_file(&url, &full_path).await {
                    tracing::warn!("Failed to download {}: {}", lib.name, e);
                }
            }
        }

        progress_callback(100, 100, "Fabric installed!");
        tracing::info!("Fabric installation complete");

        Ok(())
    }

    fn maven_to_path(&self, name: &str) -> String {
        // Convert Maven coordinate to path: group:artifact:version -> group/artifact/version/artifact-version.jar
        let parts: Vec<&str> = name.split(':').collect();
        if parts.len() >= 3 {
            let group = parts[0].replace('.', "/");
            let artifact = parts[1];
            let version = parts[2];
            format!(
                "{}/{}/{}/{}-{}.jar",
                group, artifact, version, artifact, version
            )
        } else {
            name.to_string()
        }
    }

    // ==================== Launch ====================

    pub async fn launch(
        &self,
        mc_version: &str,
        fabric_version: &str,
        access_token: &str,
        username: &str,
        uuid: &str,
        ram_mb: u32,
        show_logs: bool,
        profile_id: Option<&str>,
    ) -> Result<std::process::Child, MinecraftError> {
        tracing::info!(
            "Launching Minecraft {} with Fabric {}",
            mc_version,
            fabric_version
        );

        // Find Java
        let java = self.find_java().await?;
        tracing::info!("Using Java: {}", java);

        // Build classpath
        let classpath = self.build_classpath(mc_version, fabric_version).await?;
        tracing::info!("Classpath entries: {}", classpath.len());

        // Get main class from Fabric profile
        let fabric_id = format!("fabric-loader-{}-{}", fabric_version, mc_version);
        let fabric_profile_path = self
            .game_dir
            .join("versions")
            .join(&fabric_id)
            .join(format!("{}.json", fabric_id));

        let fabric_profile: FabricProfile =
            serde_json::from_str(&fs::read_to_string(&fabric_profile_path).await?)?;

        let main_class = fabric_profile.main_class;

        // Get asset index from version JSON
        let version_json_path = self
            .game_dir
            .join("versions")
            .join(mc_version)
            .join(format!("{}.json", mc_version));
        let version_json: serde_json::Value =
            serde_json::from_str(&fs::read_to_string(&version_json_path).await?)?;
        let asset_index = version_json["assets"].as_str().unwrap_or(mc_version);

        // Build natives directory
        let natives_dir = self.game_dir.join("natives").join(mc_version);
        fs::create_dir_all(&natives_dir).await?;

        // Extract natives
        self.extract_natives(mc_version, &natives_dir).await?;

        // Build arguments
        let classpath_str = classpath.join(";"); // Windows uses ;

        // Point Fabric to the profile-specific mods directory
        let mods_dir = match profile_id {
            Some(pid) => self.game_dir.join("mods").join(mc_version).join(pid),
            None => self.game_dir.join("mods").join(mc_version),
        };
        tracing::info!("Using mods directory: {:?}", mods_dir);

        // Set profile-specific directories for resourcepacks, shaderpacks, and datapacks
        let resourcepacks_dir = match profile_id {
            Some(pid) => self
                .game_dir
                .join("resourcepacks")
                .join(mc_version)
                .join(pid),
            None => self.game_dir.join("resourcepacks").join(mc_version),
        };
        tracing::info!("Using resourcepacks directory: {:?}", resourcepacks_dir);

        let shaderpacks_dir = match profile_id {
            Some(pid) => self.game_dir.join("shaderpacks").join(mc_version).join(pid),
            None => self.game_dir.join("shaderpacks").join(mc_version),
        };
        tracing::info!("Using shaderpacks directory: {:?}", shaderpacks_dir);

        let datapacks_dir = match profile_id {
            Some(pid) => self.game_dir.join("datapacks").join(mc_version).join(pid),
            None => self.game_dir.join("datapacks").join(mc_version),
        };
        tracing::info!("Using datapacks directory: {:?}", datapacks_dir);
        // Create temp directory for native libraries (fixes AccessDeniedException)
        let temp_dir = self.game_dir.join("temp");
        std::fs::create_dir_all(&temp_dir).ok();

        let args: Vec<String> = vec![
            format!("-Xmx{}M", ram_mb),
            format!("-Xms{}M", ram_mb / 2),
            format!("-Djava.library.path={}", natives_dir.display()),
            format!("-Djava.io.tmpdir={}", self.game_dir.join("temp").display()),
            format!("-Dfabric.modsFolder={}", mods_dir.display()),
            format!(
                "-Dmiracle.resourcepacksFolder={}",
                resourcepacks_dir.display()
            ),
            format!("-Dmiracle.shaderpacksFolder={}", shaderpacks_dir.display()),
            format!("-Dmiracle.datapacksFolder={}", datapacks_dir.display()),
            "-Dminecraft.launcher.brand=MiracleClient".to_string(),
            "-Dminecraft.launcher.version=1.0.0".to_string(),
            "-Dorg.lwjgl.opengl.Display.title=Miracle Client".to_string(),
            "-cp".to_string(),
            classpath_str,
            main_class,
            "--username".to_string(),
            username.to_string(),
            "--version".to_string(),
            fabric_id.clone(),
            "--gameDir".to_string(),
            self.game_dir.display().to_string(),
            "--assetsDir".to_string(),
            self.game_dir.join("assets").display().to_string(),
            "--assetIndex".to_string(),
            asset_index.to_string(),
            "--uuid".to_string(),
            uuid.replace("-", ""),
            "--accessToken".to_string(),
            access_token.to_string(),
            "--userType".to_string(),
            "msa".to_string(),
            "--versionType".to_string(),
            "release".to_string(),
            "--clientId".to_string(),
            "Miracle Client".to_string(),
        ];

        tracing::info!("Launch command: {} {}", java, args.join(" "));

        let mut command = std::process::Command::new(&java);
        command.args(&args).current_dir(&self.game_dir);

        // On Windows, show console window if show_logs is enabled
        #[cfg(target_os = "windows")]
        {
            use std::os::windows::process::CommandExt;

            if show_logs {
                // CREATE_NEW_CONSOLE = 0x00000010
                command.creation_flags(0x00000010);
            } else {
                // CREATE_NO_WINDOW = 0x08000000
                command.creation_flags(0x08000000);
            }
        }

        let child = command
            .spawn()
            .map_err(|e| MinecraftError::LaunchFailed(e.to_string()))?;

        tracing::info!("Game launched with PID: {}", child.id());

        Ok(child)
    }

    async fn find_java(&self) -> Result<String, MinecraftError> {
        // Check JAVA_HOME
        if let Ok(java_home) = std::env::var("JAVA_HOME") {
            let java_path = PathBuf::from(&java_home).join("bin").join("java.exe");
            if java_path.exists() {
                return Ok(java_path.display().to_string());
            }
        }

        // Check common locations on Windows
        let program_files =
            std::env::var("ProgramFiles").unwrap_or_else(|_| "C:\\Program Files".to_string());
        let common_paths = [
            format!(
                "{}\\Eclipse Adoptium\\jdk-21.0.1.12-hotspot\\bin\\java.exe",
                program_files
            ),
            format!("{}\\Java\\jdk-21\\bin\\java.exe", program_files),
            format!("{}\\Java\\jre-21\\bin\\java.exe", program_files),
            format!(
                "{}\\Eclipse Adoptium\\jdk-17.0.9.9-hotspot\\bin\\java.exe",
                program_files
            ),
            format!("{}\\Java\\jdk-17\\bin\\java.exe", program_files),
        ];

        for path in &common_paths {
            if PathBuf::from(path).exists() {
                return Ok(path.clone());
            }
        }

        // Try java from PATH
        if let Ok(output) = std::process::Command::new("where").arg("java").output() {
            if output.status.success() {
                if let Ok(path) = String::from_utf8(output.stdout) {
                    if let Some(first_line) = path.lines().next() {
                        return Ok(first_line.trim().to_string());
                    }
                }
            }
        }

        Err(MinecraftError::JavaNotFound)
    }

    async fn build_classpath(
        &self,
        mc_version: &str,
        fabric_version: &str,
    ) -> Result<Vec<String>, MinecraftError> {
        let mut classpath = Vec::new();
        let mut added_artifacts = std::collections::HashSet::new();
        let libraries_dir = self.game_dir.join("libraries");

        // Helper to extract artifact name without version (e.g., "org.ow2.asm:asm")
        let get_artifact_key = |name: &str| -> String {
            let parts: Vec<&str> = name.split(':').collect();
            if parts.len() >= 2 {
                format!("{}:{}", parts[0], parts[1])
            } else {
                name.to_string()
            }
        };

        // Add Minecraft client JAR
        let client_jar = self
            .game_dir
            .join("versions")
            .join(mc_version)
            .join(format!("{}.jar", mc_version));
        if client_jar.exists() {
            classpath.push(client_jar.display().to_string());
        }

        // Add Fabric libraries (these take priority)
        let fabric_id = format!("fabric-loader-{}-{}", fabric_version, mc_version);
        let fabric_profile_path = self
            .game_dir
            .join("versions")
            .join(&fabric_id)
            .join(format!("{}.json", fabric_id));

        if fabric_profile_path.exists() {
            let fabric_profile: FabricProfile =
                serde_json::from_str(&fs::read_to_string(&fabric_profile_path).await?)?;

            for lib in &fabric_profile.libraries {
                let artifact_key = get_artifact_key(&lib.name);
                if added_artifacts.insert(artifact_key) {
                    let lib_path = libraries_dir.join(self.maven_to_path(&lib.name));
                    if lib_path.exists() {
                        classpath.push(lib_path.display().to_string());
                    }
                }
            }
        }

        // Add vanilla libraries (skip if artifact already added by Fabric)
        let details = self.get_version_details(mc_version).await?;
        for lib in &details.libraries {
            if !self.should_use_library(lib) {
                continue;
            }

            let artifact_key = get_artifact_key(&lib.name);
            if added_artifacts.insert(artifact_key) {
                if let Some(downloads) = &lib.downloads {
                    if let Some(artifact) = &downloads.artifact {
                        let lib_path = libraries_dir.join(&artifact.path);
                        if lib_path.exists() {
                            classpath.push(lib_path.display().to_string());
                        }
                    }
                }
            }
        }

        Ok(classpath)
    }

    async fn extract_natives(
        &self,
        mc_version: &str,
        natives_dir: &PathBuf,
    ) -> Result<(), MinecraftError> {
        let libraries_dir = self.game_dir.join("libraries");

        // Helper function to extract DLLs from a JAR
        let extract_jar = |jar_path: &PathBuf| -> Result<(), MinecraftError> {
            if let Ok(file) = std::fs::File::open(jar_path) {
                if let Ok(mut archive) = zip::ZipArchive::new(file) {
                    for i in 0..archive.len() {
                        if let Ok(mut entry) = archive.by_index(i) {
                            let name = entry.name().to_string();
                            // Extract all DLLs and so files
                            if name.ends_with(".dll")
                                || name.ends_with(".so")
                                || name.ends_with(".dylib")
                            {
                                // Get just the filename (remove any directory structure)
                                if let Some(filename) = name.split('/').last() {
                                    let out_path = natives_dir.join(filename);
                                    if let Ok(mut out_file) = std::fs::File::create(&out_path) {
                                        let _ = std::io::copy(&mut entry, &mut out_file);
                                        tracing::debug!("Extracted native: {}", filename);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Ok(())
        };

        // Extract vanilla Minecraft natives
        let details = self.get_version_details(mc_version).await?;
        for lib in &details.libraries {
            if !self.should_use_library(lib) {
                continue;
            }

            if let Some(natives) = &lib.natives {
                if let Some(native_key) = natives.get("windows") {
                    if let Some(downloads) = &lib.downloads {
                        if let Some(classifiers) = &downloads.classifiers {
                            let native_key = native_key.replace("${arch}", "64");
                            if let Some(native_artifact) = classifiers.get(&native_key) {
                                let native_jar = libraries_dir.join(&native_artifact.path);
                                if native_jar.exists() {
                                    extract_jar(&native_jar)?;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Also extract LWJGL natives (look for -natives-windows.jar files)
        let lwjgl_dir = libraries_dir.join("org").join("lwjgl");
        if lwjgl_dir.exists() {
            for entry in walkdir::WalkDir::new(&lwjgl_dir).max_depth(5) {
                if let Ok(entry) = entry {
                    let path = entry.path();
                    if path.is_file()
                        && path.to_string_lossy().contains("-natives-windows")
                        && path.extension().and_then(|s| s.to_str()) == Some("jar")
                    {
                        tracing::info!("Extracting LWJGL native: {:?}", path.file_name());
                        extract_jar(&path.to_path_buf())?;
                    }
                }
            }
        }

        Ok(())
    }

    // ==================== File Downloads ====================

    async fn download_file(&self, url: &str, path: &PathBuf) -> Result<(), MinecraftError> {
        let response = self.client.get(url).send().await?;

        if !response.status().is_success() {
            return Err(MinecraftError::DownloadFailed(format!(
                "HTTP {}: {}",
                response.status(),
                url
            )));
        }

        let bytes = response.bytes().await?;
        fs::write(path, &bytes).await?;

        Ok(())
    }

    async fn download_file_verified(
        &self,
        url: &str,
        path: &PathBuf,
        expected_sha1: &str,
    ) -> Result<(), MinecraftError> {
        // Check if file already exists with correct hash
        if path.exists() {
            if let Ok(existing) = fs::read(path).await {
                let mut hasher = Sha1::new();
                hasher.update(&existing);
                let hash = format!("{:x}", hasher.finalize());
                if hash == expected_sha1 {
                    return Ok(());
                }
            }
        }

        let response = self.client.get(url).send().await?;

        if !response.status().is_success() {
            return Err(MinecraftError::DownloadFailed(format!(
                "HTTP {}: {}",
                response.status(),
                url
            )));
        }

        let bytes = response.bytes().await?;

        // Verify hash
        let mut hasher = Sha1::new();
        hasher.update(&bytes);
        let hash = format!("{:x}", hasher.finalize());

        if hash != expected_sha1 {
            return Err(MinecraftError::DownloadFailed(format!(
                "Hash mismatch for {}: expected {}, got {}",
                url, expected_sha1, hash
            )));
        }

        fs::write(path, &bytes).await?;

        Ok(())
    }
}
