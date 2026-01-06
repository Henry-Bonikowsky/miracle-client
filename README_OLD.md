# Miracle Client

A modern Minecraft 1.21+ client with custom launcher, built on Fabric with Sodium/Iris integration.

## Project Structure

```
miracle-client/
├── launcher/     # Tauri desktop application (Rust + React)
├── mod/          # Fabric mod for Minecraft 1.21+
├── backend/      # API server (Rust/Axum)
├── shared/       # Shared types and protocols
└── infra/        # Infrastructure configuration
```

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

## License
Proprietary - All Rights Reserved
