@echo off
setlocal EnableExtensions

title Album Tags - Galaxy Installer

for %%F in ("%~dp0album-tags-v*.apk") do set "APK_PATH=%%~fF"
if not defined APK_PATH (
    echo [ERROR] APK file was not found next to this installer.
    pause
    exit /b 1
)

set "ADB_EXE=adb"
where adb >nul 2>nul
if errorlevel 1 (
    set "TOOLS_DIR=%TEMP%\album-tags-platform-tools"
    set "TOOLS_ZIP=%TEMP%\album-tags-platform-tools.zip"
    if not exist "%TOOLS_DIR%\platform-tools\adb.exe" (
        echo Downloading Android Platform Tools from Google...
        powershell -NoProfile -ExecutionPolicy Bypass -Command ^
          "Invoke-WebRequest -UseBasicParsing 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile '%TOOLS_ZIP%'; Expand-Archive -LiteralPath '%TOOLS_ZIP%' -DestinationPath '%TOOLS_DIR%' -Force"
        if errorlevel 1 (
            echo [ERROR] Android Platform Tools download failed.
            pause
            exit /b 1
        )
    )
    set "ADB_EXE=%TOOLS_DIR%\platform-tools\adb.exe"
)

echo.
echo 1. Enable Developer options and USB debugging on your Galaxy.
echo 2. Connect the phone by USB and approve the authorization prompt.
echo.
"%ADB_EXE%" start-server >nul
"%ADB_EXE%" devices
echo.
echo Installing Album Tags...
"%ADB_EXE%" install -r "%APK_PATH%"
if errorlevel 1 (
    echo.
    echo [ERROR] Installation failed. Check the USB connection and authorization.
    pause
    exit /b 1
)

echo.
echo [OK] Album Tags was installed successfully.
pause
