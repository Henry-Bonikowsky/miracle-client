use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct ModCompatibility {
    pub mod_id: String,
    pub mod_name: String,
    pub slug: String,
    pub compatible: bool,
}

pub async fn check_mods_compatibility(
    mod_slugs: Vec<String>,
    minecraft_version: &str,
) -> Result<Vec<ModCompatibility>, String> {
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(120))
        .user_agent("MiracleClient/1.0")
        .build()
        .map_err(|e| e.to_string())?;

    let mut results = Vec::new();

    for mod_slug in mod_slugs {
        tracing::info!(
            "Checking compatibility for {} on {}",
            mod_slug,
            minecraft_version
        );

        // Add delay to avoid rate limiting (300ms between requests = ~3 per second)
        tokio::time::sleep(std::time::Duration::from_millis(300)).await;

        let versions_url = format!(
            "https://api.modrinth.com/v2/project/{}/version?game_versions=[\"{}\"]&loaders=[\"fabric\"]",
            mod_slug, minecraft_version
        );

        match client.get(&versions_url).send().await {
            Ok(response) => {
                if response.status().is_success() {
                    match response.json::<Vec<serde_json::Value>>().await {
                        Ok(versions) => {
                            let compatible = !versions.is_empty();
                            results.push(ModCompatibility {
                                mod_id: mod_slug.clone(),
                                mod_name: mod_slug.clone(),
                                slug: mod_slug.clone(),
                                compatible,
                            });
                        }
                        Err(_) => {
                            results.push(ModCompatibility {
                                mod_id: mod_slug.clone(),
                                mod_name: mod_slug.clone(),
                                slug: mod_slug.clone(),
                                compatible: false,
                            });
                        }
                    }
                } else {
                    results.push(ModCompatibility {
                        mod_id: mod_slug.clone(),
                        mod_name: mod_slug.clone(),
                        slug: mod_slug.clone(),
                        compatible: false,
                    });
                }
            }
            Err(_) => {
                results.push(ModCompatibility {
                    mod_id: mod_slug.clone(),
                    mod_name: mod_slug.clone(),
                    slug: mod_slug.clone(),
                    compatible: false,
                });
            }
        }
    }

    Ok(results)
}
