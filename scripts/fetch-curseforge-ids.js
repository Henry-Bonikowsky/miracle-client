const https = require('https');
const fs = require('fs');

const CURSEFORGE_API_KEY = '$2a$10$JerFj3jTqK5z2SJlzO4i.e0/7O3wSdh27GyM4vHIRinf7VJvuJnfe';

// CurseForge mods from the catalog that need IDs
const curseforgeUrls = [
  { url: 'https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor', id: 'cosmetic-armor' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/freelook', id: 'freelook' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/armor-hud', id: 'armor-hud' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/direction-hud', id: 'direction-hud' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/damage-indicators', id: 'damage-indicators' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/toggle-sneak-sprint', id: 'toggle-sneak-sprint' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/perspective-mod', id: 'perspective-mod' },
  { url: 'https://www.curseforge.com/minecraft/mc-mods/old-animations-mod', id: 'old-animations' },
];

function searchCurseForgeBySlug(slug) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'api.curseforge.com',
      path: `/v1/mods/search?gameId=432&searchFilter=${encodeURIComponent(slug)}&classId=6`,
      method: 'GET',
      headers: {
        'x-api-key': CURSEFORGE_API_KEY,
        'Accept': 'application/json'
      }
    };

    const req = https.request(options, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (json.data && json.data.length > 0) {
            // Find exact match by slug
            const exactMatch = json.data.find(mod =>
              mod.slug === slug || mod.links?.websiteUrl?.includes(slug)
            );
            resolve(exactMatch || json.data[0]);
          } else {
            resolve(null);
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', reject);
    req.end();
  });
}

async function fetchAllIds() {
  const results = {};

  console.log('Fetching CurseForge project IDs...\n');

  for (const mod of curseforgeUrls) {
    const slug = mod.url.split('/').pop();
    console.log(`Fetching ${mod.id} (${slug})...`);

    try {
      const data = await searchCurseForgeBySlug(slug);
      if (data && data.id) {
        results[mod.id] = data.id;
        console.log(`  ✓ Found: ${data.id} - ${data.name}`);
      } else {
        console.log(`  ✗ Not found`);
        results[mod.id] = null;
      }
    } catch (error) {
      console.log(`  ✗ Error: ${error.message}`);
      results[mod.id] = null;
    }

    // Rate limit: wait 500ms between requests
    await new Promise(resolve => setTimeout(resolve, 500));
  }

  console.log('\n=== Results ===\n');
  console.log('Copy these to your modCatalog.ts:\n');

  for (const [id, projectId] of Object.entries(results)) {
    if (projectId) {
      console.log(`  curseforgeId: ${projectId}, // ${id}`);
    }
  }

  // Write to file
  fs.writeFileSync('curseforge-ids.json', JSON.stringify(results, null, 2));
  console.log('\nFull results saved to curseforge-ids.json');
}

fetchAllIds().catch(console.error);
