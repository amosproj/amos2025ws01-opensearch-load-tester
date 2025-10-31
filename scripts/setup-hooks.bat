@echo off
REM setup-hooks.bat
REM Installs Git hooks for the repository (Windows)

setlocal enabledelayedexpansion

echo Setting up Git hooks...

REM Find git root
for /f "delims=" %%i in ('git rev-parse --show-toplevel 2^>nul') do set GIT_ROOT=%%i

if not defined GIT_ROOT (
    echo Error: Not in a Git repository
    exit /b 1
)

REM Convert forward slashes to backslashes for Windows
set "GIT_ROOT=%GIT_ROOT:/=\%"

set "HOOKS_SRC=%~dp0git-hooks"
set "HOOKS_DEST=%GIT_ROOT%\.git\hooks"

REM Check if hooks source directory exists
if not exist "%HOOKS_SRC%" (
    echo Error: Hooks directory not found: %HOOKS_SRC%
    exit /b 1
)

REM Create hooks directory if it doesn't exist
if not exist "%HOOKS_DEST%" mkdir "%HOOKS_DEST%"

REM Install each hook
set HOOKS_INSTALLED=0
for %%f in ("%HOOKS_SRC%\*") do (
    if exist "%%f" (
        set "hook_name=%%~nxf"
        set "dest_file=%HOOKS_DEST%\!hook_name!"
        
        REM Backup existing hook if present
        if exist "!dest_file!" (
            set "timestamp=%date:~-4%%date:~-7,2%%date:~-10,2%-%time:~0,2%%time:~3,2%%time:~6,2%"
            set "timestamp=!timestamp: =0!"
            set "backup_file=!dest_file!.backup-!timestamp!"
            echo Backing up existing hook: !hook_name! to !backup_file!
            move /y "!dest_file!" "!backup_file!" >nul
        )
        
        REM Copy hook
        copy /y "%%f" "!dest_file!" >nul
        echo Installed: !hook_name!
        set /a HOOKS_INSTALLED+=1
    )
)

if %HOOKS_INSTALLED% EQU 0 (
    echo Warning: No hooks found to install
    exit /b 1
)

echo.
echo Successfully installed %HOOKS_INSTALLED% hook(s)!
echo.
echo Usage:
echo    - Use @username in commit messages to add co-authors
echo    - Edit .coauthors file to add/update team members
echo    - Signed-off-by is automatically added to the commit message
echo.
echo Example commit:
echo    git commit -m 'Add new feature @maxmuster'
echo Example outcome:
echo    Add new feature
echo    Co-Authored-By: Max Mustermann <maxmuster@example.com>
echo    Signed-off-by: Your Name <your.email@example.com>
echo.

endlocal

