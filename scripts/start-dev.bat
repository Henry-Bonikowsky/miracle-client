@echo off
echo Killing existing node processes...
powershell -Command "Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force"

echo Starting Miracle Client dev server...
cd launcher
npm run tauri dev
