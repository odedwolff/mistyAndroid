@echo off
echo Android Studio Emulator Cleanup Script
echo ====================================
echo.

:: Request admin privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo This script requires administrator privileges.
    echo Please run as administrator.
    pause
    exit /b 1
)

echo Checking if Android Studio is running...
tasklist /FI "IMAGENAME eq studio64.exe" 2>NUL | find /I /N "studio64.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo Please close Android Studio before continuing.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

echo.
echo Starting cleanup process...
echo.

:: Create a backup folder with timestamp
set timestamp=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set timestamp=%timestamp: =0%
set "backup_dir=%USERPROFILE%\AndroidEmulator_Backup_%timestamp%"
mkdir "%backup_dir%"

echo Creating backup at: %backup_dir%
echo.

:: Backup and clean AVD directory
if exist "%USERPROFILE%\.android\avd" (
    echo Backing up AVD files...
    xcopy /E /I "%USERPROFILE%\.android\avd" "%backup_dir%\avd"
    echo Removing AVD files...
    rd /s /q "%USERPROFILE%\.android\avd"
) else (
    echo No AVD directory found.
)

:: Backup and delete avd.ini
if exist "%USERPROFILE%\.android\avd.ini" (
    echo Backing up avd.ini...
    copy "%USERPROFILE%\.android\avd.ini" "%backup_dir%"
    echo Removing avd.ini...
    del "%USERPROFILE%\.android\avd.ini"
)

:: Clean temporary files
echo Cleaning temporary AndroidEmulator files...
for /d %%G in ("%LOCALAPPDATA%\Temp\AndroidEmulator*") do rd /s /q "%%G"

:: Clean SDK cache
if exist "%USERPROFILE%\.android\cache" (
    echo Backing up Android cache...
    xcopy /E /I "%USERPROFILE%\.android\cache" "%backup_dir%\cache"
    echo Cleaning Android SDK cache...
    rd /s /q "%USERPROFILE%\.android\cache"
) else (
    echo No Android cache directory found.
)

echo.
echo Cleanup complete!
echo A backup of your files has been created at: %backup_dir%
echo.
echo You can now restart Android Studio and create new emulators.
echo.
pause