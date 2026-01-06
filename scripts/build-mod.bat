@echo off
REM Build Fabric mod and copy to launcher resources
echo Building Fabric mod...
cd "%~dp0..\mod"
call gradlew build -x test

echo Copying mod JARs to launcher resources...
copy /Y "versions\1.21.11\build\libs\miracle-client-1.0.0.jar" "..\launcher\src-tauri\resources\miracle-client-1.21.11.jar"
copy /Y "versions\1.21.8\build\libs\miracle-client-1.0.0.jar" "..\launcher\src-tauri\resources\miracle-client-1.21.8.jar"
copy /Y "versions\1.21.5\build\libs\miracle-client-1.0.0.jar" "..\launcher\src-tauri\resources\miracle-client-1.21.5.jar"
copy /Y "versions\1.21.4\build\libs\miracle-client-1.0.0.jar" "..\launcher\src-tauri\resources\miracle-client-1.21.4.jar"

echo Done! Mod JARs copied to launcher resources.
pause
