use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tokio::fs;

// Supabase project configuration
const SUPABASE_URL: &str = "https://xthsogqpmclnafmzpape.supabase.co";
const SUPABASE_ANON_KEY: &str = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh0aHNvZ3FwbWNsbmFmbXpwYXBlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY0NDM0MTQsImV4cCI6MjA4MjAxOTQxNH0.vccOvqRARfhzG3YAllbyyN664T3otr2M2os0jNdSc44";

// ============================================
// USER & FRIENDS TYPES
// ============================================

/// A user profile from the database
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: String,
    pub minecraft_uuid: String,
    pub username: String,
    pub is_online: Option<bool>,
    pub current_server: Option<String>,
    pub last_seen: Option<String>,
}

/// A friendship record
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Friendship {
    pub id: String,
    pub user_id: String,
    pub friend_id: String,
    pub status: String, // "pending" or "accepted"
    pub created_at: Option<String>,
    pub accepted_at: Option<String>,
}

/// A friend with their user profile (for display)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Friend {
    pub friendship_id: String,
    pub user: User,
    pub status: String,
    pub is_incoming: bool, // true if they sent the request to us
}

/// Result of a friend request operation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FriendRequestResult {
    pub success: bool,
    pub message: String,
}

/// Represents a mod release in Supabase
///
/// Create this table in Supabase:
/// ```sql
/// CREATE TABLE mod_releases (
///     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
///     mod_id TEXT NOT NULL,              -- e.g., "miracle-client"
///     version TEXT NOT NULL,             -- e.g., "1.0.1"
///     minecraft_version TEXT NOT NULL,   -- e.g., "1.21.8" or "1.21.4"
///     download_url TEXT NOT NULL,        -- URL to the .jar file
///     changelog TEXT,                    -- Release notes
///     checksum TEXT,                     -- SHA256 hash for verification
///     mandatory BOOLEAN DEFAULT false,   -- Force update?
///     created_at TIMESTAMPTZ DEFAULT now(),
///     UNIQUE(mod_id, version, minecraft_version)
/// );
///
/// -- Enable RLS and add a policy for public read access
/// ALTER TABLE mod_releases ENABLE ROW LEVEL SECURITY;
/// CREATE POLICY "Allow public read" ON mod_releases FOR SELECT USING (true);
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModRelease {
    pub id: String,
    pub mod_id: String,
    pub version: String,
    pub minecraft_version: String,
    pub download_url: String,
    pub changelog: Option<String>,
    pub checksum: Option<String>,
    pub mandatory: Option<bool>,
}

/// Result of checking for mod updates
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModUpdateInfo {
    pub mod_id: String,
    pub current_version: String,
    pub latest_version: String,
    pub download_url: String,
    pub changelog: Option<String>,
    pub mandatory: bool,
    pub has_update: bool,
}

pub struct SupabaseClient {
    client: Client,
    base_url: String,
    api_key: String,
}

impl SupabaseClient {
    pub fn new() -> Self {
        Self {
            client: Client::builder()
                .timeout(std::time::Duration::from_secs(30))
                .build()
                .expect("Failed to create HTTP client"),
            base_url: SUPABASE_URL.to_string(),
            api_key: SUPABASE_ANON_KEY.to_string(),
        }
    }

    /// Check if Supabase is configured
    pub fn is_configured(&self) -> bool {
        !self.api_key.contains("YOUR_ANON_KEY")
    }

    /// Get the latest release for a specific mod and Minecraft version
    pub async fn get_latest_release(
        &self,
        mod_id: &str,
        minecraft_version: &str,
    ) -> Result<Option<ModRelease>, String> {
        if !self.is_configured() {
            tracing::warn!("Supabase not configured, skipping update check");
            return Ok(None);
        }

        let url = format!(
            "{}/rest/v1/mod_releases?mod_id=eq.{}&minecraft_version=eq.{}&order=created_at.desc&limit=1",
            self.base_url, mod_id, minecraft_version
        );

        let response = self
            .client
            .get(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch from Supabase: {}", e))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(format!("Supabase request failed: {} - {}", status, body));
        }

        let releases: Vec<ModRelease> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse Supabase response: {}", e))?;

        Ok(releases.into_iter().next())
    }

    /// Check for updates for the Miracle Client mod
    pub async fn check_miracle_client_update(
        &self,
        current_version: &str,
        minecraft_version: &str,
    ) -> Result<ModUpdateInfo, String> {
        let latest = self
            .get_latest_release("miracle-client", minecraft_version)
            .await?;

        match latest {
            Some(release) => {
                let has_update = is_newer_version(&release.version, current_version);

                Ok(ModUpdateInfo {
                    mod_id: "miracle-client".to_string(),
                    current_version: current_version.to_string(),
                    latest_version: release.version,
                    download_url: release.download_url,
                    changelog: release.changelog,
                    mandatory: release.mandatory.unwrap_or(false),
                    has_update,
                })
            }
            None => Ok(ModUpdateInfo {
                mod_id: "miracle-client".to_string(),
                current_version: current_version.to_string(),
                latest_version: current_version.to_string(),
                download_url: String::new(),
                changelog: None,
                mandatory: false,
                has_update: false,
            }),
        }
    }

    /// Download a mod update to a specified path
    pub async fn download_mod_update(
        &self,
        update_info: &ModUpdateInfo,
        dest_dir: &PathBuf,
    ) -> Result<PathBuf, String> {
        if update_info.download_url.is_empty() {
            return Err("No download URL provided".to_string());
        }

        tracing::info!("Downloading mod update from: {}", update_info.download_url);

        let response = self
            .client
            .get(&update_info.download_url)
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

        // Validate the download
        if bytes.len() < 1000 {
            return Err("Downloaded file is too small, may be corrupted".to_string());
        }

        // Create destination directory if needed
        fs::create_dir_all(dest_dir)
            .await
            .map_err(|e| format!("Failed to create mods directory: {}", e))?;

        // Save the file
        let filename = format!("miracle-client-{}.jar", update_info.latest_version);
        let dest_path = dest_dir.join(&filename);

        fs::write(&dest_path, bytes)
            .await
            .map_err(|e| format!("Failed to write mod file: {}", e))?;

        tracing::info!("Mod update downloaded to: {:?}", dest_path);

        Ok(dest_path)
    }
}

impl Default for SupabaseClient {
    fn default() -> Self {
        Self::new()
    }
}

// ============================================
// FRIENDS SYSTEM IMPLEMENTATION
// ============================================

impl SupabaseClient {
    /// Get or create a user by Minecraft UUID
    pub async fn get_or_create_user(
        &self,
        minecraft_uuid: &str,
        username: &str,
    ) -> Result<User, String> {
        let clean_uuid = minecraft_uuid.replace("-", "").to_lowercase();

        // First try to get existing user
        let url = format!(
            "{}/rest/v1/users?minecraft_uuid=eq.{}",
            self.base_url, clean_uuid
        );

        let response = self
            .client
            .get(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch user: {}", e))?;

        let users: Vec<User> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse user response: {}", e))?;

        if let Some(mut user) = users.into_iter().next() {
            // Update username if changed (and new username is not empty)
            if !username.is_empty() && user.username != username {
                let update_url = format!(
                    "{}/rest/v1/users?minecraft_uuid=eq.{}",
                    self.base_url, clean_uuid
                );
                let _ = self
                    .client
                    .patch(&update_url)
                    .header("apikey", &self.api_key)
                    .header("Authorization", format!("Bearer {}", self.api_key))
                    .header("Content-Type", "application/json")
                    .json(&serde_json::json!({
                        "username": username,
                        "last_seen": chrono::Utc::now().to_rfc3339()
                    }))
                    .send()
                    .await;
                user.username = username.to_string();
            }
            return Ok(user);
        }

        // Create new user
        let insert_url = format!("{}/rest/v1/users", self.base_url);
        let response = self
            .client
            .post(&insert_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")
            .json(&serde_json::json!({
                "minecraft_uuid": clean_uuid,
                "username": username
            }))
            .send()
            .await
            .map_err(|e| format!("Failed to create user: {}", e))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(format!("Failed to create user: {} - {}", status, body));
        }

        let users: Vec<User> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse created user: {}", e))?;

        users
            .into_iter()
            .next()
            .ok_or_else(|| "No user returned after creation".to_string())
    }

    /// Search for users by username (partial match)
    pub async fn search_users(&self, query: &str, exclude_uuid: &str) -> Result<Vec<User>, String> {
        let clean_exclude = exclude_uuid.replace("-", "").to_lowercase();

        // Case-insensitive search using ilike
        let url = format!(
            "{}/rest/v1/users?username=ilike.*{}*&minecraft_uuid=neq.{}&limit=10",
            self.base_url, query, clean_exclude
        );

        let response = self
            .client
            .get(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to search users: {}", e))?;

        let users: Vec<User> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse search results: {}", e))?;

        Ok(users)
    }

    /// Get all friends and pending requests for a user
    pub async fn get_friends(&self, minecraft_uuid: &str) -> Result<Vec<Friend>, String> {
        let clean_uuid = minecraft_uuid.replace("-", "").to_lowercase();

        // Look up the user (don't create if not exists)
        let url = format!(
            "{}/rest/v1/users?minecraft_uuid=eq.{}",
            self.base_url, clean_uuid
        );

        let response = self
            .client
            .get(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch user: {}", e))?;

        let users: Vec<User> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse user response: {}", e))?;

        let user = match users.into_iter().next() {
            Some(u) => u,
            None => return Ok(vec![]), // User not registered yet
        };

        let user_id = &user.id;

        // Get friendships where we are user_id (outgoing)
        let outgoing_url = format!(
            "{}/rest/v1/friendships?user_id=eq.{}&select=*",
            self.base_url, user_id
        );

        let outgoing_response = self
            .client
            .get(&outgoing_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch outgoing friendships: {}", e))?;

        let outgoing: Vec<Friendship> = outgoing_response
            .json()
            .await
            .map_err(|e| format!("Failed to parse outgoing friendships: {}", e))?;

        // Get friendships where we are friend_id (incoming)
        let incoming_url = format!(
            "{}/rest/v1/friendships?friend_id=eq.{}&select=*",
            self.base_url, user_id
        );

        let incoming_response = self
            .client
            .get(&incoming_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch incoming friendships: {}", e))?;

        let incoming: Vec<Friendship> = incoming_response
            .json()
            .await
            .map_err(|e| format!("Failed to parse incoming friendships: {}", e))?;

        // Collect all friend user IDs we need to fetch
        let mut friend_ids: Vec<String> = outgoing.iter().map(|f| f.friend_id.clone()).collect();
        friend_ids.extend(incoming.iter().map(|f| f.user_id.clone()));

        if friend_ids.is_empty() {
            return Ok(vec![]);
        }

        // Fetch all friend user profiles
        let ids_param = friend_ids
            .iter()
            .map(|id| format!("\"{}\"", id))
            .collect::<Vec<_>>()
            .join(",");
        let users_url = format!("{}/rest/v1/users?id=in.({})", self.base_url, ids_param);

        let users_response = self
            .client
            .get(&users_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch friend profiles: {}", e))?;

        let users: Vec<User> = users_response
            .json()
            .await
            .map_err(|e| format!("Failed to parse friend profiles: {}", e))?;

        let users_map: std::collections::HashMap<String, User> =
            users.into_iter().map(|u| (u.id.clone(), u)).collect();

        // Build friend list
        let mut friends: Vec<Friend> = vec![];

        // Add outgoing (we sent the request)
        for friendship in outgoing {
            if let Some(friend_user) = users_map.get(&friendship.friend_id) {
                friends.push(Friend {
                    friendship_id: friendship.id,
                    user: friend_user.clone(),
                    status: friendship.status,
                    is_incoming: false,
                });
            }
        }

        // Add incoming (they sent the request)
        for friendship in incoming {
            if let Some(friend_user) = users_map.get(&friendship.user_id) {
                friends.push(Friend {
                    friendship_id: friendship.id,
                    user: friend_user.clone(),
                    status: friendship.status,
                    is_incoming: true,
                });
            }
        }

        Ok(friends)
    }

    /// Send a friend request
    pub async fn send_friend_request(
        &self,
        from_uuid: &str,
        from_username: &str,
        to_user_id: &str,
    ) -> Result<FriendRequestResult, String> {
        // Get or create the sender
        let from_user = self.get_or_create_user(from_uuid, from_username).await?;

        // Check if friendship already exists (in either direction)
        let check_url = format!(
            "{}/rest/v1/friendships?or=(and(user_id.eq.{},friend_id.eq.{}),and(user_id.eq.{},friend_id.eq.{}))",
            self.base_url, from_user.id, to_user_id, to_user_id, from_user.id
        );

        let check_response = self
            .client
            .get(&check_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to check existing friendship: {}", e))?;

        let existing: Vec<Friendship> = check_response
            .json()
            .await
            .map_err(|e| format!("Failed to parse friendship check: {}", e))?;

        if !existing.is_empty() {
            let friendship = &existing[0];
            if friendship.status == "accepted" {
                return Ok(FriendRequestResult {
                    success: false,
                    message: "Already friends".to_string(),
                });
            } else {
                return Ok(FriendRequestResult {
                    success: false,
                    message: "Friend request already pending".to_string(),
                });
            }
        }

        // Create friendship
        let insert_url = format!("{}/rest/v1/friendships", self.base_url);
        let response = self
            .client
            .post(&insert_url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&serde_json::json!({
                "user_id": from_user.id,
                "friend_id": to_user_id,
                "status": "pending"
            }))
            .send()
            .await
            .map_err(|e| format!("Failed to send friend request: {}", e))?;

        if response.status().is_success() {
            Ok(FriendRequestResult {
                success: true,
                message: "Friend request sent".to_string(),
            })
        } else {
            let body = response.text().await.unwrap_or_default();
            Err(format!("Failed to send friend request: {}", body))
        }
    }

    /// Accept a friend request
    pub async fn accept_friend_request(
        &self,
        friendship_id: &str,
    ) -> Result<FriendRequestResult, String> {
        let url = format!(
            "{}/rest/v1/friendships?id=eq.{}",
            self.base_url, friendship_id
        );

        let response = self
            .client
            .patch(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&serde_json::json!({
                "status": "accepted",
                "accepted_at": chrono::Utc::now().to_rfc3339()
            }))
            .send()
            .await
            .map_err(|e| format!("Failed to accept friend request: {}", e))?;

        if response.status().is_success() {
            Ok(FriendRequestResult {
                success: true,
                message: "Friend request accepted".to_string(),
            })
        } else {
            let body = response.text().await.unwrap_or_default();
            Err(format!("Failed to accept friend request: {}", body))
        }
    }

    /// Decline/remove a friendship
    pub async fn remove_friendship(
        &self,
        friendship_id: &str,
    ) -> Result<FriendRequestResult, String> {
        let url = format!(
            "{}/rest/v1/friendships?id=eq.{}",
            self.base_url, friendship_id
        );

        let response = self
            .client
            .delete(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to remove friendship: {}", e))?;

        if response.status().is_success() {
            Ok(FriendRequestResult {
                success: true,
                message: "Friendship removed".to_string(),
            })
        } else {
            let body = response.text().await.unwrap_or_default();
            Err(format!("Failed to remove friendship: {}", body))
        }
    }

    /// Update user's online status
    pub async fn update_user_status(
        &self,
        minecraft_uuid: &str,
        is_online: bool,
        current_server: Option<&str>,
    ) -> Result<(), String> {
        let clean_uuid = minecraft_uuid.replace("-", "").to_lowercase();

        let url = format!(
            "{}/rest/v1/users?minecraft_uuid=eq.{}",
            self.base_url, clean_uuid
        );

        let mut body = serde_json::json!({
            "is_online": is_online,
            "last_seen": chrono::Utc::now().to_rfc3339()
        });

        if let Some(server) = current_server {
            body["current_server"] = serde_json::json!(server);
        } else {
            body["current_server"] = serde_json::json!(null);
        }

        self.client
            .patch(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&body)
            .send()
            .await
            .map_err(|e| format!("Failed to update status: {}", e))?;

        Ok(())
    }
}

// ============================================
// PROFILE SHARING IMPLEMENTATION
// ============================================

/// A shared profile in the database
///
/// Create this table in Supabase:
/// ```sql
/// CREATE TABLE shared_profiles (
///     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
///     short_code TEXT UNIQUE NOT NULL,
///     name TEXT NOT NULL,
///     version TEXT NOT NULL,
///     mods JSONB NOT NULL,
///     creator_uuid TEXT,
///     creator_username TEXT,
///     downloads INTEGER DEFAULT 0,
///     created_at TIMESTAMPTZ DEFAULT now()
/// );
///
/// ALTER TABLE shared_profiles ENABLE ROW LEVEL SECURITY;
/// CREATE POLICY "Allow public read" ON shared_profiles FOR SELECT USING (true);
/// CREATE POLICY "Allow insert" ON shared_profiles FOR INSERT WITH CHECK (true);
/// CREATE POLICY "Allow update downloads" ON shared_profiles FOR UPDATE USING (true);
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SharedProfile {
    pub id: String,
    pub short_code: String,
    pub name: String,
    pub version: String,
    pub mods: Vec<String>,
    pub creator_uuid: Option<String>,
    pub creator_username: Option<String>,
    pub downloads: i32,
    pub created_at: Option<String>,
}

/// Result of sharing a profile
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ShareProfileResult {
    pub success: bool,
    pub short_code: Option<String>,
    pub message: String,
}

impl SupabaseClient {
    /// Generate a random 8-character short code
    fn generate_short_code() -> String {
        use rand::Rng;
        const CHARSET: &[u8] = b"ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        let mut rng = rand::thread_rng();
        (0..8)
            .map(|_| {
                let idx = rng.gen_range(0..CHARSET.len());
                CHARSET[idx] as char
            })
            .collect()
    }

    /// Share a profile to Supabase, returns a short code
    pub async fn share_profile(
        &self,
        name: &str,
        version: &str,
        mods: &[String],
        creator_uuid: Option<&str>,
        creator_username: Option<&str>,
    ) -> Result<ShareProfileResult, String> {
        if !self.is_configured() {
            return Ok(ShareProfileResult {
                success: false,
                short_code: None,
                message: "Supabase not configured".to_string(),
            });
        }

        let short_code = Self::generate_short_code();
        let clean_uuid = creator_uuid.map(|u| u.replace("-", "").to_lowercase());

        let url = format!("{}/rest/v1/shared_profiles", self.base_url);

        let response = self
            .client
            .post(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")
            .json(&serde_json::json!({
                "short_code": short_code,
                "name": name,
                "version": version,
                "mods": mods,
                "creator_uuid": clean_uuid,
                "creator_username": creator_username,
                "downloads": 0
            }))
            .send()
            .await
            .map_err(|e| format!("Failed to share profile: {}", e))?;

        if response.status().is_success() {
            Ok(ShareProfileResult {
                success: true,
                short_code: Some(short_code),
                message: "Profile shared successfully".to_string(),
            })
        } else {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();

            // Check if it was a duplicate code collision (unlikely but possible)
            if body.contains("duplicate") || body.contains("unique") {
                // Generate a new code and retry once more (non-recursive)
                let new_code = Self::generate_short_code();
                let retry_response = self
                    .client
                    .post(&url)
                    .header("apikey", &self.api_key)
                    .header("Authorization", format!("Bearer {}", self.api_key))
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .json(&serde_json::json!({
                        "short_code": new_code,
                        "name": name,
                        "version": version,
                        "mods": mods,
                        "creator_uuid": clean_uuid,
                        "creator_username": creator_username,
                        "downloads": 0
                    }))
                    .send()
                    .await
                    .map_err(|e| format!("Failed to share profile (retry): {}", e))?;

                if retry_response.status().is_success() {
                    return Ok(ShareProfileResult {
                        success: true,
                        short_code: Some(new_code),
                        message: "Profile shared successfully".to_string(),
                    });
                }
            }

            Err(format!("Failed to share profile: {} - {}", status, body))
        }
    }

    /// Get a shared profile by its short code
    pub async fn get_shared_profile(
        &self,
        short_code: &str,
    ) -> Result<Option<SharedProfile>, String> {
        if !self.is_configured() {
            return Err("Supabase not configured".to_string());
        }

        let url = format!(
            "{}/rest/v1/shared_profiles?short_code=eq.{}",
            self.base_url,
            short_code.to_uppercase()
        );

        let response = self
            .client
            .get(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .send()
            .await
            .map_err(|e| format!("Failed to fetch shared profile: {}", e))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(format!("Failed to fetch profile: {} - {}", status, body));
        }

        let profiles: Vec<SharedProfile> = response
            .json()
            .await
            .map_err(|e| format!("Failed to parse shared profile: {}", e))?;

        if let Some(profile) = profiles.into_iter().next() {
            // Increment download count
            let _ = self.increment_profile_downloads(&profile.id).await;
            Ok(Some(profile))
        } else {
            Ok(None)
        }
    }

    /// Increment download count for a shared profile
    async fn increment_profile_downloads(&self, profile_id: &str) -> Result<(), String> {
        let url = format!("{}/rest/v1/rpc/increment_profile_downloads", self.base_url);

        // Try using RPC function, fall back to direct update if it doesn't exist
        let response = self
            .client
            .post(&url)
            .header("apikey", &self.api_key)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&serde_json::json!({
                "profile_id": profile_id
            }))
            .send()
            .await;

        // If RPC fails, just ignore - download count is not critical
        if response.is_err() {
            tracing::debug!("Could not increment profile downloads (RPC may not exist)");
        }

        Ok(())
    }
}

/// Compare two semver-style versions
/// Returns true if `latest` is newer than `current`
fn is_newer_version(latest: &str, current: &str) -> bool {
    let parse_version = |v: &str| -> Vec<u32> {
        v.split('.')
            .filter_map(|part| part.parse::<u32>().ok())
            .collect()
    };

    let latest_parts = parse_version(latest);
    let current_parts = parse_version(current);

    for i in 0..latest_parts.len().max(current_parts.len()) {
        let l = latest_parts.get(i).copied().unwrap_or(0);
        let c = current_parts.get(i).copied().unwrap_or(0);

        if l > c {
            return true;
        } else if l < c {
            return false;
        }
    }

    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_version_comparison() {
        assert!(is_newer_version("1.0.1", "1.0.0"));
        assert!(is_newer_version("1.1.0", "1.0.9"));
        assert!(is_newer_version("2.0.0", "1.9.9"));
        assert!(!is_newer_version("1.0.0", "1.0.0"));
        assert!(!is_newer_version("1.0.0", "1.0.1"));
        assert!(!is_newer_version("0.9.9", "1.0.0"));
    }
}
