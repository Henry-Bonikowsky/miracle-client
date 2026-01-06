use crate::models::FriendInfo;
use crate::AppState;
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use serde::Deserialize;
use std::sync::Arc;

#[derive(Debug, Deserialize)]
pub struct AddFriendRequest {
    pub username: String,
}

pub async fn list_friends(
    State(_state): State<Arc<AppState>>,
) -> Result<Json<Vec<FriendInfo>>, (StatusCode, String)> {
    // TODO: Get user from JWT and return their friends
    Ok(Json(vec![]))
}

pub async fn list_requests(
    State(_state): State<Arc<AppState>>,
) -> Result<Json<Vec<FriendInfo>>, (StatusCode, String)> {
    // TODO: Get pending friend requests
    Ok(Json(vec![]))
}

pub async fn send_request(
    State(_state): State<Arc<AppState>>,
    Json(_req): Json<AddFriendRequest>,
) -> Result<StatusCode, (StatusCode, String)> {
    // TODO: Send friend request
    Ok(StatusCode::OK)
}

pub async fn accept_request(
    State(_state): State<Arc<AppState>>,
    Path(_id): Path<String>,
) -> Result<StatusCode, (StatusCode, String)> {
    // TODO: Accept friend request
    Ok(StatusCode::OK)
}

pub async fn remove_friend(
    State(_state): State<Arc<AppState>>,
    Path(_id): Path<String>,
) -> Result<StatusCode, (StatusCode, String)> {
    // TODO: Remove friend
    Ok(StatusCode::OK)
}
