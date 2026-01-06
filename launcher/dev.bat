@echo off
echo Killing existing node processes...
taskkill /F /IM node.exe 2>nul

echo Setting temp directories...
set TMPDIR=%USERPROFILE%\AppData\Local\Temp
set TMP=%USERPROFILE%\AppData\Local\Temp
set TEMP=%USERPROFILE%\AppData\Local\Temp

echo Starting Tauri dev server...
npm run tauri dev
