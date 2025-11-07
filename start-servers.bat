@echo off
echo ==========================================
echo   QUIZ SYSTEM - Starting Both Servers
echo ==========================================
echo.
cd /d "%~dp0"
javac src\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Compilation successful!
echo Starting servers...
echo.
java src.ServerLauncher
pause

