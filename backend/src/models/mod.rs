use serde::{Deserialize, Serialize};
use sqlx::FromRow;

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct User {
    pub id: String,
    pub minecraft_uuid: String,
    pub username: String,
    pub email: Option<String>,
    pub password_hash: Option<String>,
    pub created_at: String,
    pub updated_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserResponse {
    pub id: String,
    pub minecraft_uuid: String,
    pub username: String,
    pub created_at: String,
}

impl From<User> for UserResponse {
    fn from(user: User) -> Self {
        Self {
            id: user.id,
            minecraft_uuid: user.minecraft_uuid,
            username: user.username,
            created_at: user.created_at,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Cosmetic {
    pub id: String,
    pub name: String,
    pub description: String,
    pub cosmetic_type: String,
    pub asset_url: String,
    pub price: i32,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct UserCosmetic {
    pub id: String,
    pub user_id: String,
    pub cosmetic_id: String,
    pub equipped: i32,
    pub purchased_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Friendship {
    pub id: String,
    pub user_id: String,
    pub friend_id: String,
    pub status: String,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FriendInfo {
    pub id: String,
    pub username: String,
    pub minecraft_uuid: String,
    pub online: bool,
    pub status: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModVersion {
    pub mod_id: String,
    pub version: String,
    pub minecraft_version: String,
    pub download_url: String,
    pub sha256: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UpdateCheck {
    pub launcher_version: String,
    pub update_available: bool,
    pub download_url: Option<String>,
    pub changelog: Option<String>,
}
