@echo off
REM Build Tauri app for production
echo Building Miracle Client launcher...
cd "%~dp0..\launcher"
npm run tauri build

echo Done! Check launcher\src-tauri\target\release for the built app.
pause
