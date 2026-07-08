@echo off
setlocal

cd /d "%~dp0"
set "PROJECT_DIR=%CD%"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] Khong tim thay Java tai: %JAVA_HOME%
  echo Hay cai JDK 21 hoac sua bien JAVA_HOME trong file build-ziot-edge.bat
  exit /b 1
)

echo [ZIOT] JAVA_HOME=%JAVA_HOME%
echo [ZIOT] Stop Gradle daemon cu neu co...
call gradlew.bat --stop --console=plain

echo [ZIOT] Build cac module can thiet cho ban ZIOT...
call gradlew.bat --no-daemon :io.openems.common:jar :io.openems.edge.controller.api.common:jar :io.openems.edge.controller.api.backend:jar :io.openems.edge.ziot.generic:clean :io.openems.edge.ziot.generic:jar --console=plain --warn
if errorlevel 1 exit /b %errorlevel%

echo [ZIOT] Export ZiotEdgeApp...
call gradlew.bat --no-daemon :io.openems.edge.application:export.ZiotEdgeApp -x compileJava --console=plain --warn
if errorlevel 1 exit /b %errorlevel%

if not exist "build" mkdir "build"
copy /Y "io.openems.edge.application\generated\distributions\executable\ZiotEdgeApp.jar" "build\ziot-edge.jar"
if errorlevel 1 exit /b %errorlevel%

if exist "%TEMP%\ziot-edge-update" rmdir /S /Q "%TEMP%\ziot-edge-update"
mkdir "%TEMP%\ziot-edge-update\jar"
copy /Y "io.openems.edge.ziot.generic\generated\io.openems.edge.ziot.generic.jar" "%TEMP%\ziot-edge-update\jar\io.openems.edge.ziot.generic.jar"
if errorlevel 1 exit /b %errorlevel%
pushd "%TEMP%\ziot-edge-update"
jar uf "%PROJECT_DIR%\build\ziot-edge.jar" jar/io.openems.edge.ziot.generic.jar
if errorlevel 1 exit /b %errorlevel%
popd

echo [ZIOT] Build xong:
dir "build\ziot-edge.jar"
