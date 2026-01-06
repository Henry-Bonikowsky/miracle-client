use reqwest::Client;
use serde::{Deserialize, Serialize};
use thiserror::Error;

const DEVICE_CODE_URL: &str = "https://login.live.com/oauth20_connect.srf";
const TOKEN_URL: &str = "https://login.live.com/oauth20_token.srf";
const XBOX_AUTH_URL: &str = "https://user.auth.xboxlive.com/user/authenticate";
const XSTS_AUTH_URL: &str = "https://xsts.auth.xboxlive.com/xsts/authorize";
const MINECRAFT_AUTH_URL: &str = "https://api.minecraftservices.com/authentication/login_with_xbox";
const MINECRAFT_PROFILE_URL: &str = "https://api.minecraftservices.com/minecraft/profile";

// Official Minecraft launcher client ID (works without whitelisting)
const CLIENT_ID: &str = "00000000402b5328";

#[derive(Error, Debug)]
pub enum AuthError {
    #[error("HTTP request failed: {0}")]
    HttpError(#[from] reqwest::Error),
    #[error("Failed to parse response: {0}")]
    ParseError(String),
    #[error("Authentication failed: {0}")]
    AuthFailed(String),
    #[error("User does not own Minecraft")]
    NoMinecraft,
    #[error("Authentication timed out")]
    Timeout,
    #[error("Authentication cancelled")]
    Cancelled,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MinecraftProfile {
    pub id: String,
    pub name: String,
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceCodeResponse {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    #[serde(default = "default_expires")]
    pub expires_in: u64,
    #[serde(default = "default_interval")]
    pub interval: u64,
    #[serde(default)]
    pub message: Option<String>,
}

fn default_expires() -> u64 {
    900
}
fn default_interval() -> u64 {
    5
}

/// Parse URL-encoded response from Microsoft OAuth endpoints
fn parse_form_response(text: &str) -> Result<std::collections::HashMap<String, String>, AuthError> {
    let mut map = std::collections::HashMap::new();
    for pair in text.split('&') {
        if let Some((key, value)) = pair.split_once('=') {
            map.insert(
                urlencoding::decode(key)
                    .map_err(|e| AuthError::ParseError(e.to_string()))?
                    .to_string(),
                urlencoding::decode(value)
                    .map_err(|e| AuthError::ParseError(e.to_string()))?
                    .to_string(),
            );
        }
    }
    Ok(map)
}

#[derive(Debug, Deserialize)]
struct TokenResponse {
    access_token: String,
    #[serde(default)]
    refresh_token: Option<String>,
    #[serde(default)]
    expires_in: Option<u64>,
    #[serde(default)]
    user_id: Option<String>,
}

#[derive(Debug, Deserialize)]
struct TokenErrorResponse {
    error: String,
}

#[derive(Debug, Serialize)]
struct XboxAuthRequest {
    #[serde(rename = "Properties")]
    properties: XboxAuthProperties,
    #[serde(rename = "RelyingParty")]
    relying_party: String,
    #[serde(rename = "TokenType")]
    token_type: String,
}

#[derive(Debug, Serialize)]
struct XboxAuthProperties {
    #[serde(rename = "AuthMethod")]
    auth_method: String,
    #[serde(rename = "SiteName")]
    site_name: String,
    #[serde(rename = "RpsTicket")]
    rps_ticket: String,
}

#[derive(Debug, Deserialize)]
struct XboxAuthResponse {
    #[serde(rename = "Token")]
    token: String,
    #[serde(rename = "DisplayClaims")]
    display_claims: DisplayClaims,
}

#[derive(Debug, Deserialize)]
struct DisplayClaims {
    xui: Vec<XuiClaim>,
}

#[derive(Debug, Deserialize)]
struct XuiClaim {
    uhs: String,
}

#[derive(Debug, Serialize)]
struct MinecraftAuthRequest {
    #[serde(rename = "identityToken")]
    identity_token: String,
}

#[derive(Debug, Deserialize)]
struct MinecraftAuthResponse {
    access_token: String,
    expires_in: u64,
}

#[derive(Debug, Deserialize)]
struct MinecraftProfileResponse {
    id: String,
    name: String,
}

pub struct AuthManager {
    client: Client,
}

impl AuthManager {
    pub fn new() -> Self {
        Self {
            client: Client::new(),
        }
    }

    /// Start device code flow - returns the code info for the user
    pub async fn start_device_code_flow(&self) -> Result<DeviceCodeResponse, AuthError> {
        let params = [
            ("client_id", CLIENT_ID),
            ("response_type", "device_code"),
            ("scope", "service::user.auth.xboxlive.com::MBI_SSL"),
        ];

        let response = self
            .client
            .post(DEVICE_CODE_URL)
            .form(&params)
            .send()
            .await?;

        let status = response.status();
        let text = response.text().await?;

        tracing::info!("Device code response status: {}", status);
        tracing::info!("Device code response body: {}", text);
        tracing::info!("Response body length: {} bytes", text.len());
        tracing::info!(
            "First 100 chars: {}",
            &text.chars().take(100).collect::<String>()
        );

        if !status.is_success() {
            return Err(AuthError::AuthFailed(format!(
                "Device code request failed (status {}): {}",
                status, text
            )));
        }

        // Try parsing as JSON first (Microsoft might return JSON)
        let device_code =
            if let Ok(json_response) = serde_json::from_str::<DeviceCodeResponse>(&text) {
                tracing::info!("Parsed as JSON successfully");
                json_response
            } else {
                tracing::info!("JSON parse failed, trying form-encoded");
                // Try form-encoded
                let data = parse_form_response(&text)?;
                tracing::info!("Parsed data keys: {:?}", data.keys().collect::<Vec<_>>());

                // Check for error in response
                if let Some(error) = data.get("error") {
                    return Err(AuthError::AuthFailed(format!(
                        "Microsoft returned error: {} - {}",
                        error,
                        data.get("error_description").unwrap_or(&String::new())
                    )));
                }

                DeviceCodeResponse {
                    device_code: data
                        .get("device_code")
                        .ok_or_else(|| {
                            AuthError::ParseError(format!(
                                "Missing device_code. Available fields: {:?}. Raw response: {}",
                                data.keys().collect::<Vec<_>>(),
                                text
                            ))
                        })?
                        .clone(),
                    user_code: data
                        .get("user_code")
                        .ok_or_else(|| {
                            AuthError::ParseError(format!(
                                "Missing user_code. Available fields: {:?}",
                                data.keys().collect::<Vec<_>>()
                            ))
                        })?
                        .clone(),
                    verification_uri: data
                        .get("verification_uri")
                        .or_else(|| data.get("verification_url"))
                        .ok_or_else(|| {
                            AuthError::ParseError(format!(
                                "Missing verification_uri/url. Available fields: {:?}",
                                data.keys().collect::<Vec<_>>()
                            ))
                        })?
                        .clone(),
                    expires_in: data
                        .get("expires_in")
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(900),
                    interval: data
                        .get("interval")
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(5),
                    message: data.get("message").cloned(),
                }
            };

        tracing::info!("User code: {}", device_code.user_code);
        tracing::info!("Verification URI: {}", device_code.verification_uri);
        tracing::info!(
            "Device code (first 20 chars): {}...",
            &device_code.device_code[..20.min(device_code.device_code.len())]
        );
        tracing::info!("Expires in: {} seconds", device_code.expires_in);

        Ok(device_code)
    }

    /// Poll for token after user completes device code flow and complete authentication
    pub async fn poll_and_authenticate(
        &self,
        device_code: &str,
        interval: u64,
    ) -> Result<MinecraftProfile, AuthError> {
        let params = [
            ("grant_type", "device_code"),
            ("client_id", CLIENT_ID),
            ("device_code", device_code),
        ];

        loop {
            tokio::time::sleep(tokio::time::Duration::from_secs(interval)).await;

            let response = self.client.post(TOKEN_URL).form(&params).send().await?;

            let text = response.text().await?;

            tracing::info!("Token poll response: {}", text);

            // Try parsing as JSON first
            if let Ok(token_response) = serde_json::from_str::<TokenResponse>(&text) {
                tracing::info!("Token parsed as JSON, got access token");
                return self.complete_authentication(token_response).await;
            }

            // Try parsing as JSON error
            #[derive(serde::Deserialize)]
            struct JsonError {
                error: String,
            }
            if let Ok(json_error) = serde_json::from_str::<JsonError>(&text) {
                match json_error.error.as_str() {
                    "authorization_pending" => {
                        tracing::info!("Authorization pending, continuing to poll...");
                        continue;
                    }
                    "slow_down" => {
                        tracing::info!("Slow down requested, waiting extra 5 seconds");
                        tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
                        continue;
                    }
                    "expired_token" => return Err(AuthError::Timeout),
                    "authorization_declined" => return Err(AuthError::Cancelled),
                    _ => return Err(AuthError::AuthFailed(json_error.error)),
                }
            }

            // Try form-encoded as fallback
            let data = parse_form_response(&text)?;

            // Check for errors first
            if let Some(error) = data.get("error") {
                match error.as_str() {
                    "authorization_pending" => {
                        tracing::info!("Authorization pending (form), continuing to poll...");
                        continue;
                    }
                    "slow_down" => {
                        tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
                        continue;
                    }
                    "expired_token" => return Err(AuthError::Timeout),
                    "authorization_declined" => return Err(AuthError::Cancelled),
                    _ => return Err(AuthError::AuthFailed(error.clone())),
                }
            }

            // Parse success response
            if let Some(access_token) = data.get("access_token") {
                let token = TokenResponse {
                    access_token: access_token.clone(),
                    refresh_token: data.get("refresh_token").cloned(),
                    expires_in: data.get("expires_in").and_then(|s| s.parse().ok()),
                    user_id: data.get("user_id").cloned(),
                };
                tracing::info!("Got access token (form), proceeding to Xbox auth");
                return self.complete_authentication(token).await;
            }

            return Err(AuthError::ParseError(format!(
                "Unexpected response (neither JSON nor form): {}",
                text
            )));
        }
    }

    /// Complete authentication after getting MS token
    async fn complete_authentication(
        &self,
        ms_token: TokenResponse,
    ) -> Result<MinecraftProfile, AuthError> {
        tracing::info!("Step 1: Authenticating with Xbox Live...");
        let xbox_token = self.authenticate_xbox(&ms_token.access_token).await?;
        tracing::info!("Xbox auth successful");

        tracing::info!("Step 2: Getting XSTS token...");
        let xsts_response = self.get_xsts_token(&xbox_token.token).await?;
        tracing::info!("XSTS token obtained");

        tracing::info!("Step 3: Authenticating with Minecraft...");
        let uhs = &xsts_response.display_claims.xui[0].uhs;
        let mc_token = self
            .authenticate_minecraft(uhs, &xsts_response.token)
            .await?;
        tracing::info!("Minecraft auth successful");

        tracing::info!("Step 4: Getting Minecraft profile...");
        let profile = self.get_minecraft_profile(&mc_token.access_token).await?;
        tracing::info!("Got profile: {}", profile.name);

        let expires_at = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64
            + (mc_token.expires_in * 1000);

        Ok(MinecraftProfile {
            id: profile.id,
            name: profile.name,
            access_token: mc_token.access_token,
            refresh_token: ms_token.refresh_token.unwrap_or_default(),
            expires_at,
        })
    }

    /// Refresh an existing token
    pub async fn refresh(&self, refresh_token: &str) -> Result<MinecraftProfile, AuthError> {
        let params = [
            ("client_id", CLIENT_ID),
            ("refresh_token", refresh_token),
            ("grant_type", "refresh_token"),
            ("scope", "service::user.auth.xboxlive.com::MBI_SSL"),
        ];

        let response = self
            .client
            .post(TOKEN_URL)
            .form(&params)
            .send()
            .await?
            .json::<TokenResponse>()
            .await?;

        self.complete_authentication(response).await
    }

    async fn authenticate_xbox(&self, access_token: &str) -> Result<XboxAuthResponse, AuthError> {
        // For login.live.com tokens, use "t=<token>" format, not "d=<token>"
        let request = XboxAuthRequest {
            properties: XboxAuthProperties {
                auth_method: "RPS".to_string(),
                site_name: "user.auth.xboxlive.com".to_string(),
                rps_ticket: format!("t={}", access_token),
            },
            relying_party: "http://auth.xboxlive.com".to_string(),
            token_type: "JWT".to_string(),
        };

        let response = self
            .client
            .post(XBOX_AUTH_URL)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .json(&request)
            .send()
            .await?;

        let status = response.status();
        let text = response.text().await?;
        tracing::info!("Xbox auth response (status {}): {}", status, text);

        if !status.is_success() {
            return Err(AuthError::AuthFailed(format!("Xbox auth failed: {}", text)));
        }

        serde_json::from_str(&text).map_err(|e| {
            AuthError::ParseError(format!("Failed to parse Xbox response: {} - {}", e, text))
        })
    }

    async fn get_xsts_token(&self, xbox_token: &str) -> Result<XboxAuthResponse, AuthError> {
        let request = serde_json::json!({
            "Properties": {
                "SandboxId": "RETAIL",
                "UserTokens": [xbox_token]
            },
            "RelyingParty": "rp://api.minecraftservices.com/",
            "TokenType": "JWT"
        });

        let response = self
            .client
            .post(XSTS_AUTH_URL)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .json(&request)
            .send()
            .await?;

        let status = response.status();
        let text = response.text().await?;
        tracing::info!("XSTS response (status {}): {}", status, text);

        if !status.is_success() {
            return Err(AuthError::AuthFailed(format!("XSTS auth failed: {}", text)));
        }

        serde_json::from_str(&text).map_err(|e| {
            AuthError::ParseError(format!("Failed to parse XSTS response: {} - {}", e, text))
        })
    }

    async fn authenticate_minecraft(
        &self,
        uhs: &str,
        xsts_token: &str,
    ) -> Result<MinecraftAuthResponse, AuthError> {
        let request = MinecraftAuthRequest {
            identity_token: format!("XBL3.0 x={};{}", uhs, xsts_token),
        };

        let response = self
            .client
            .post(MINECRAFT_AUTH_URL)
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await?;

        let status = response.status();
        let text = response.text().await?;
        tracing::info!("Minecraft auth response (status {}): {}", status, text);

        if !status.is_success() {
            return Err(AuthError::AuthFailed(format!(
                "Minecraft auth failed: {}",
                text
            )));
        }

        serde_json::from_str(&text).map_err(|e| {
            AuthError::ParseError(format!(
                "Failed to parse Minecraft response: {} - {}",
                e, text
            ))
        })
    }

    async fn get_minecraft_profile(
        &self,
        access_token: &str,
    ) -> Result<MinecraftProfileResponse, AuthError> {
        let response = self
            .client
            .get(MINECRAFT_PROFILE_URL)
            .header("Authorization", format!("Bearer {}", access_token))
            .send()
            .await?;

        if response.status() == 404 {
            return Err(AuthError::NoMinecraft);
        }

        let profile = response.json::<MinecraftProfileResponse>().await?;
        Ok(profile)
    }
}
