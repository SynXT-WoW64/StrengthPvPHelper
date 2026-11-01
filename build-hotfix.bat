@echo off
setlocal
cd /d "%~dp0"
echo StrengthPvPHelper UI UPDATE - Minecraft 1.17.1 / Fabric / Java 17
echo.
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found in PATH. Set JAVA_HOME to your JDK 17 folder first.
  pause
  exit /b 1
)
java -version 2>&1 | findstr /C:"17." >nul
if errorlevel 1 (
  echo WARNING: PATH does not appear to point to Java 17. Fabric 1.17.1 build uses Java 17.
  echo Set JAVA_HOME and PATH to Eclipse Adoptium JDK 17 before continuing.
  pause
  exit /b 1
)
call gradlew.bat --no-daemon clean build
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Save and send the complete console output.
  pause
  exit /b 1
)
echo.
echo BUILD SUCCESSFUL. The fresh mod JAR is in build\libs.
echo For a development client, use the Gradle task runClient from Eclipse Buildship.
pause
