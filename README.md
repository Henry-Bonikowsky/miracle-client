# Miracle Client

A modern, full-stack Minecraft launcher with custom mod support, cloud profiles, and social features.

<img src="https://img.shields.io/badge/Minecraft-1.21+-brightgreen" alt="MC 1.21+"/> <img src="https://img.shields.io/badge/Tauri-2.0-blue" alt="Tauri"/> <img src="https://img.shields.io/badge/React-18-61DAFB" alt="React"/> <img src="https://img.shields.io/badge/Rust-1.75+-orange" alt="Rust"/>

## Overview

Miracle Client is a sophisticated Minecraft launcher built with modern web technologies. It combines a Tauri desktop application (Rust + React) with a custom Fabric mod, providing seamless mod management, cloud sync, and social features for Minecraft 1.21+.

## Key Features

### 🚀 Launcher (Tauri + React)
- **Modern UI** with cinematic backgrounds and smooth animations
- **Mod management** with CurseForge and Modrinth API integration
- **Profile system** with per-profile mod configurations
- **Automatic updates** with version checking
- **Cloud sync** via Supabase for profiles and settings
- **News feed** with admin-controlled content
- **Friends system** with online status tracking

### 🎮 Fabric Mod (Multi-Version)
- **Stonecutter multi-version support** targeting 4 Minecraft API generations
  - 1.21 - 1.21.4
  - 1.21.5 - 1.21.7
  - 1.21.8
  - 1.21.9+
- **Auto-installation** on every game launch
- **Mod compatibility checking** and dependency resolution

### 🔧 Backend (Rust/Axum)
- **RESTful API** for authentication and data management
- **JWT-based auth** with secure session handling
- **User profiles** and friends management
- **Update distribution** system
- **Cosmetics** and customization

## Architecture

```
miracle-client/
├── launcher/                    # Tauri desktop app
│   ├── src/                    # React + TypeScript frontend
│   │   ├── pages/             # Home, News, Friends, Profiles, Settings
│   │   ├── components/        # Reusable UI components
│   │   └── lib/stores/        # Zustand state management
│   └── src-tauri/             # Rust backend
│       ├── ipc/               # IPC commands (launch, updates, mods)
│       ├── minecraft/         # MC download & version management
│       ├── profiles/          # Profile system
│       └── supabase.rs        # Cloud sync integration
│
├── mod/                        # Fabric mod (702 Java files)
│   └── versions/              # Multi-version support via Stonecutter
│       ├── 1.21.4/           # MC 1.21 - 1.21.4
│       ├── 1.21.5/           # MC 1.21.5 - 1.21.7
│       ├── 1.21.8/           # MC 1.21.8
│       └── 1.21.11/          # MC 1.21.9+
│
└── backend/                    # Rust/Axum API server
    └── routes/                # Auth, Users, Friends, Updates, Cosmetics
```

## Technology Stack

### Frontend
- **React 18** with TypeScript 5.9
- **Vite** for blazing-fast builds
- **TailwindCSS** for styling
- **Framer Motion** for animations
- **Zustand** for state management
- **React Router 7** for navigation
- **Lucide React** for icons

### Desktop Layer
- **Tauri 2.0** for native desktop wrapper
- **Rust** for IPC layer and system integration

### Mod Development
- **Fabric Mod Loader** for Minecraft modding
- **Stonecutter** for multi-version support
- **Gradle** with Kotlin DSL for builds
- **Loom** for Fabric development tooling

### Backend
- **Rust** with Axum web framework
- **Supabase** for database and authentication
- **JWT** for secure session management

## Features in Detail

### Mod Management
- Browse and install mods from CurseForge and Modrinth
- Automatic dependency resolution
- Mod compatibility checking
- Update notifications
- Per-profile mod configurations

### Profile System
- Multiple game profiles with independent configurations
- Cloud sync across devices
- Version-specific mod sets
- Custom launch parameters

### Social Features
- Friends list with online/offline status
- Friend requests and management
- Current server tracking
- Last seen timestamps

### Launcher Features
- Automatic Minecraft version downloads
- Game process management
- Crash detection and reporting
- Resource pack integration
- Settings persistence

## Development

### Prerequisites
- Node.js 20+
- Rust 1.75+
- Java 21+
- pnpm

### Setup
```bash
# Install dependencies
pnpm install

# Run launcher in dev mode
pnpm --filter launcher tauri dev

# Build mod
cd mod && ./gradlew build

# Run backend
cd backend && cargo run
```

## Stats

- **Launcher**: 42 TypeScript/React files + 15 Rust files
- **Mod**: 702 Java files with multi-version support
- **Backend**: 6 route modules in Rust
- **Total**: Multi-language full-stack application

## License

Proprietary - All Rights Reserved

---

*A modern take on Minecraft launchers, built for performance and user experience.*
