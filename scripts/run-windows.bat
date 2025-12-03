@echo off
setlocal enabledelayedexpansion
:: Configuration
set "JDK_VERSION=11.0.19+7"
set "JDK_DIR=%USERPROFILE%\.runelite\vitalite\jdk"
set "JAR_NAME=VitaLite.jar"

:: Create directories if needed
if not exist "%JDK_DIR%" mkdir "%JDK_DIR%"
:: Check if JDK already exists
if not exist "%JDK_DIR%\bin\java.exe" (
    echo JDK not found. Downloading JDK %JDK_VERSION%...
    
    :: Download using PowerShell
    powershell -Command "$ProgressPreference = 'SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://api.adoptium.net/v3/binary/version/jdk-11.0.19+7/windows/x64/jdk/hotspot/normal/eclipse', '%JDK_DIR%\jdk-temp.zip')"
    
    if not exist "%JDK_DIR%\jdk-temp.zip" (
        echo Download failed!
        pause
        exit /b 1
    )
    
    :: Extract JDK using PowerShell
    echo Extracting JDK...
    powershell -Command "Expand-Archive -Path '%JDK_DIR%\jdk-temp.zip' -DestinationPath '%JDK_DIR%\temp' -Force"
    
    :: Move contents from the nested folder to JDK_DIR
    for /d %%i in ("%JDK_DIR%\temp\*") do (
        xcopy "%%i\*" "%JDK_DIR%\" /E /H /Y >nul
        rmdir "%%i" /S /Q
    )
    rmdir "%JDK_DIR%\temp" /S /Q 2>nul
    
    :: Clean up
    del "%JDK_DIR%\jdk-temp.zip"
    
    echo JDK installed successfully!
) else (
    echo JDK found at %JDK_DIR%
)
:: Launch the application
echo Launching application...
"%JDK_DIR%\bin\java.exe" -jar "%JAR_NAME%" %*
pause