@echo off
REM Start Miracle Client dev server
echo Killing existing node processes...
powershell -Command "Get-Process node -ErrorAction SilentlyContinue | Stop-Process -Force"

echo Starting dev server...
cd "%~dp0..\launcher"
npm run tauri dev
