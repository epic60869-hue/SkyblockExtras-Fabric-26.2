@echo off
setlocal

rem Skyblock Extras Gradle launcher for Windows
rem Downloads Gradle 9.5.1 once, then reuses the local copy.

set "GRADLE_VERSION=9.5.1"
set "GRADLE_DIR=%~dp0.gradle\local-gradle\gradle-%GRADLE_VERSION%"
set "GRADLE_EXE=%GRADLE_DIR%\bin\gradle.bat"
set "ZIP=%~dp0.gradle\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_EXE%" (
    echo Gradle %GRADLE_VERSION% is not installed for this project.
    echo Downloading it now...

    if not exist "%~dp0.gradle\local-gradle" mkdir "%~dp0.gradle\local-gradle"

    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.5.1-bin.zip' -OutFile '%ZIP%'"
    if errorlevel 1 (
        echo Failed to download Gradle.
        exit /b 1
    )

    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%~dp0.gradle\local-gradle' -Force"
    if errorlevel 1 (
        echo Failed to extract Gradle.
        exit /b 1
    )

    del /q "%ZIP%" >nul 2>&1
)

call "%GRADLE_EXE%" %*
exit /b %ERRORLEVEL%
