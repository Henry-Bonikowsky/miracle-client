use crate::models::Cosmetic;
use crate::AppState;
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use std::sync::Arc;

pub async fn list_cosmetics(
    State(state): State<Arc<AppState>>,
) -> Result<Json<Vec<Cosmetic>>, (StatusCode, String)> {
    let cosmetics = sqlx::query_as::<_, Cosmetic>(
        "SELECT * FROM cosmetics ORDER BY created_at DESC",
    )
    .fetch_all(&state.db)
    .await
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    Ok(Json(cosmetics))
}

pub async fn get_cosmetic(
    State(state): State<Arc<AppState>>,
    Path(id): Path<String>,
) -> Result<Json<Cosmetic>, (StatusCode, String)> {
    let cosmetic = sqlx::query_as::<_, Cosmetic>(
        "SELECT * FROM cosmetics WHERE id = ?",
    )
    .bind(&id)
    .fetch_optional(&state.db)
    .await
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
    .ok_or_else(|| (StatusCode::NOT_FOUND, "Cosmetic not found".to_string()))?;

    Ok(Json(cosmetic))
}

pub async fn get_owned(
    State(_state): State<Arc<AppState>>,
) -> Result<Json<Vec<Cosmetic>>, (StatusCode, String)> {
    // TODO: Get user from JWT and return their owned cosmetics
    Ok(Json(vec![]))
}

pub async fn equip(
    State(_state): State<Arc<AppState>>,
    Path(_id): Path<String>,
) -> Result<StatusCode, (StatusCode, String)> {
    // TODO: Equip cosmetic for user
    Ok(StatusCode::OK)
}
