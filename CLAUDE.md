# Miracle Client

Tauri app (React + Rust) + Fabric mod for Minecraft.

---

## Scripts

**All workflow scripts are in `scripts/` folder.** Run these directly:

- `scripts/dev.bat` - Start dev server (kills existing node processes first)
- `scripts/build-mod.bat` - Build Fabric mod and copy to launcher resources
- `scripts/build-launcher.bat` - Production build of Tauri app
- `scripts/clean.bat` - Clean all build artifacts

**Note**: If Rust temp directory error occurs, run from PowerShell not Git Bash.

**For Claude**: When user asks to start dev server, build mod, etc., remind them to run the appropriate script from `scripts/` folder. Cannot execute directly since Bash is disabled.

---

## Architecture

```
launcher/
├── src/              # React frontend
└── src-tauri/src/
    ├── ipc/mod.rs        # IPC commands (launch_game, check updates)
    ├── minecraft/mod.rs  # MC download & launch
    ├── supabase.rs       # Updates, friends, profiles
    └── profiles/mod.rs   # Profile management

mod/                  # Fabric mod (Stonecutter multi-version)
```

---

## Version Mapping (CRITICAL)

Mod has 4 build targets due to API changes:

| MC Version | Mod Target |
|------------|------------|
| 1.21 - 1.21.4 | 1.21.4 |
| 1.21.5 - 1.21.7 | 1.21.5 |
| 1.21.8 | 1.21.8 |
| 1.21.9+ | 1.21.11 |

**Supabase `mod_releases`** uses mod version (1.21.4/1.21.5/1.21.8/1.21.11), not raw MC version.

---

## Mod Development

After building the mod, copy to bundled resources for the launcher to use:

```bash
# Build mod
cd mod && ./gradlew build -x test

# Update bundled resources (launcher copies from here on each launch)
cp mod/versions/1.21.11/build/libs/miracle-client-1.0.0.jar launcher/src-tauri/resources/miracle-client-1.21.11.jar
cp mod/versions/1.21.8/build/libs/miracle-client-1.0.0.jar launcher/src-tauri/resources/miracle-client-1.21.8.jar
cp mod/versions/1.21.5/build/libs/miracle-client-1.0.0.jar launcher/src-tauri/resources/miracle-client-1.21.5.jar
cp mod/versions/1.21.4/build/libs/miracle-client-1.0.0.jar launcher/src-tauri/resources/miracle-client-1.21.4.jar
```

The launcher installs bundled mod on every game launch from `launcher/src-tauri/resources/`.

---

## Windows Console

For game log output, use `CREATE_NEW_CONSOLE` flag (0x10) WITHOUT `Stdio::inherit()` - Tauri apps don't have a console.
