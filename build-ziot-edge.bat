@echo off
setlocal

cd /d "%~dp0"
set "PROJECT_DIR=%CD%"
set "LOG_FILE=%PROJECT_DIR%\build-ziot-edge.log"

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] Khong tim thay Java tai: %JAVA_HOME%
  echo Hay cai JDK 21 hoac sua bien JAVA_HOME trong file build-ziot-edge.bat
  exit /b 1
)

echo [ZIOT] JAVA_HOME=%JAVA_HOME%
if not exist "build" mkdir "build"
echo [ZIOT] Build log: %LOG_FILE%
echo [ZIOT] Build started at %DATE% %TIME% > "%LOG_FILE%"

echo [ZIOT] Stop Gradle daemon cu neu co...
call gradlew.bat --stop --console=plain >> "%LOG_FILE%" 2>&1

echo [ZIOT] Clean output cu co the gay loi bnd wrong-directory...
call gradlew.bat --no-daemon clean --console=plain --warn >> "%LOG_FILE%" 2>&1
if errorlevel 1 goto gradle_failed

echo [ZIOT] Build cac module can thiet cho ban ZIOT...
call gradlew.bat --no-daemon jar --console=plain --warn >> "%LOG_FILE%" 2>&1
if errorlevel 1 goto gradle_failed

echo [ZIOT] Export ZiotEdgeApp...
call gradlew.bat --no-daemon :io.openems.edge.application:export.ZiotEdgeApp -x compileJava --console=plain --warn >> "%LOG_FILE%" 2>&1
if errorlevel 1 goto gradle_failed

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
echo [ZIOT] Log chi tiet: %LOG_FILE%
exit /b 0

:gradle_failed
set "BUILD_ERROR=%errorlevel%"
echo [ERROR] Build failed. Xem log chi tiet: %LOG_FILE%
echo [ERROR] 80 dong cuoi:
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Content -Path '%LOG_FILE%' -Tail 80"
exit /b %BUILD_ERROR%
