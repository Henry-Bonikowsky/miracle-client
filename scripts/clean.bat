@echo off
REM Clean build artifacts
echo Cleaning build artifacts...

echo Cleaning mod builds...
cd "%~dp0..\mod"
call gradlew clean

echo Cleaning launcher builds...
cd "%~dp0..\launcher"
if exist "dist" rd /s /q "dist"
if exist "src-tauri\target" rd /s /q "src-tauri\target"

echo Done!
pause
