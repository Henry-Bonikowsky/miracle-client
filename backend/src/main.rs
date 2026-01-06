mod routes;
mod models;
mod services;

use axum::{
    routing::{get, post},
    Router,
};
use sqlx::sqlite::SqlitePoolOptions;
use std::sync::Arc;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

pub struct AppState {
    pub db: sqlx::SqlitePool,
    pub jwt_secret: String,
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Load .env file
    dotenvy::dotenv().ok();

    // Initialize logging
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            std::env::var("RUST_LOG").unwrap_or_else(|_| "info".into()),
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    tracing::info!("Starting Miracle Backend API");

    // Database connection - SQLite (no external database needed)
    let database_url = std::env::var("DATABASE_URL")
        .unwrap_or_else(|_| "sqlite:miracle.db?mode=rwc".to_string());

    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect(&database_url)
        .await?;

    tracing::info!("Connected to SQLite database");

    // Run migrations
    sqlx::migrate!("./migrations").run(&pool).await?;

    let state = Arc::new(AppState {
        db: pool,
        jwt_secret: std::env::var("JWT_SECRET").unwrap_or_else(|_| "miracle-secret-key".to_string()),
    });

    // CORS configuration
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    // Build router
    let app = Router::new()
        // Health check
        .route("/health", get(|| async { "OK" }))
        // Auth routes
        .route("/api/auth/register", post(routes::auth::register))
        .route("/api/auth/login", post(routes::auth::login))
        .route("/api/auth/refresh", post(routes::auth::refresh))
        // User routes
        .route("/api/users/me", get(routes::users::get_me))
        .route("/api/users/:id", get(routes::users::get_user))
        // Cosmetics routes
        .route("/api/cosmetics", get(routes::cosmetics::list_cosmetics))
        .route("/api/cosmetics/:id", get(routes::cosmetics::get_cosmetic))
        .route("/api/cosmetics/owned", get(routes::cosmetics::get_owned))
        .route("/api/cosmetics/:id/equip", post(routes::cosmetics::equip))
        // Friends routes
        .route("/api/friends", get(routes::friends::list_friends))
        .route("/api/friends/requests", get(routes::friends::list_requests))
        .route("/api/friends/add", post(routes::friends::send_request))
        .route("/api/friends/accept/:id", post(routes::friends::accept_request))
        .route("/api/friends/remove/:id", post(routes::friends::remove_friend))
        // Updates routes
        .route("/api/updates/check", get(routes::updates::check_updates))
        .route("/api/updates/mods", get(routes::updates::get_mod_versions))
        // Layer middleware
        .layer(cors)
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    // Start server
    let addr = std::env::var("BIND_ADDRESS").unwrap_or_else(|_| "0.0.0.0:3000".to_string());
    let listener = tokio::net::TcpListener::bind(&addr).await?;

    tracing::info!("Listening on {}", addr);

    axum::serve(listener, app).await?;

    Ok(())
}
