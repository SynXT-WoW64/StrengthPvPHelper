@echo off
setlocal
cd /d "%~dp0"

rem Force the exact JDK 17 installation found on this PC.
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JDK 17 was not found at:
  echo "%JAVA_HOME%"
  echo Edit JAVA_HOME in this BAT file to match your JDK 17 folder.
  pause
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ================================================
echo StrengthPvPHelper - forced JDK 17 build
echo ================================================
echo JAVA_HOME=%JAVA_HOME%
echo.
echo Java runtime:
"%JAVA_HOME%\bin\java.exe" -version
echo.
echo Java compiler:
"%JAVA_HOME%\bin\javac.exe" -version
echo.
echo Gradle runtime actually selected:
call gradlew.bat "-Dorg.gradle.java.home=%JAVA_HOME%" --version
if errorlevel 1 (
  echo ERROR: Gradle wrapper could not start. Check the full output above.
  pause
  exit /b 1
)
echo.
echo Building project...
call gradlew.bat "-Dorg.gradle.java.home=%JAVA_HOME%" --no-daemon clean build
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Copy the complete console output and send it.
  pause
  exit /b 1
)
echo.
echo BUILD SUCCESSFUL. Check build\libs for the mod JAR.
pause
