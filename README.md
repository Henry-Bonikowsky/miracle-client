# Miracle Client

A custom Minecraft launcher with a Fabric mod. Built with Tauri (Rust + React) for the launcher and Java for the mod.

**Minecraft 1.21+** | **Tauri 2.0** | **React 18** | **Rust**

## What it is

A Minecraft launcher that handles:
- Downloading and launching different Minecraft versions
- Installing and managing mods from CurseForge and Modrinth
- Cloud profiles (save your mod configs across devices)
- Friends system with online status
- News feed

Plus a Fabric mod that gets auto-installed and runs client-side.

## Features

### Launcher
- Modern UI with background animations
- Mod browser (CurseForge + Modrinth APIs)
- Automatic dependency resolution for mods
- Profile system - different mod setups per profile
- Cloud sync via Supabase
- Update checker
- Friends list with online/offline status

### Fabric Mod
- Multi-version support using Stonecutter
  - Supports MC 1.21 through 1.21.9+
  - 4 build targets for different API versions
- Auto-installs on every game launch
- Mod compatibility checking

### Backend
- Rust/Axum API server
- JWT auth
- User profiles and friends management
- Update distribution
- Cosmetics system

## Project Structure

```
launcher/           # Tauri desktop app
├── src/           # React frontend (42 files)
└── src-tauri/     # Rust backend (15 files)

mod/               # Fabric mod (702 Java files)
└── versions/      # Multi-version support
    ├── 1.21.4/
    ├── 1.21.5/
    ├── 1.21.8/
    └── 1.21.11/

backend/           # Rust API server
└── routes/        # Auth, users, friends, updates
```

## Tech Stack

**Frontend:** React 18, TypeScript 5.9, Vite, TailwindCSS, Framer Motion, Zustand

**Desktop:** Tauri 2.0, Rust

**Mod:** Fabric, Stonecutter, Gradle, Java 21

**Backend:** Rust, Axum, Supabase, JWT

## Development

```bash
# Install deps
pnpm install

# Run launcher
pnpm --filter launcher tauri dev

# Build mod
cd mod && ./gradlew build

# Run backend
cd backend && cargo run
```

## Stats

- Launcher: 42 TypeScript/React files + 15 Rust files
- Mod: 702 Java files
- Backend: 6 Rust route modules

## License

Proprietary - All Rights Reserved
