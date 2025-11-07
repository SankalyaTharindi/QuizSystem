/@echo off
echo ====================================================
echo   GIT PUSH - NIO Chat Feature
echo ====================================================
echo.
echo This script will help you push the changes to remote
echo.
cd /d "%~dp0"

echo Step 1: Checking Git Status...
echo ====================================================
git status
echo.
echo ====================================================
echo.

echo Step 2: Adding all files...
echo ====================================================
git add .
echo ✓ Files staged
echo.

echo Step 3: Review what will be committed...
echo ====================================================
git status
echo.
pause
echo.

echo Step 4: Committing changes...
echo ====================================================
git commit -m "feat: Add NIO-based real-time chat system" -m "New Features: NIO chat server with Selector pattern (single thread, multiple connections), Real-time messaging between students and teachers, Split-pane UI with chat panel, System notifications for user join/leave events. Technical: ChatServer.java (NIO with SocketChannel and Selector), ChatMessage.java (message model), ChatClientPanel.java (chat UI), ServerLauncher.java (unified launcher). Documentation: Complete testing guides, NIO implementation guide, architecture diagrams, quick start guide. Utilities: Batch files for easy launching and automated testing."
echo.

if %errorlevel% neq 0 (
    echo ❌ Commit failed!
    pause
    exit /b 1
)

echo ✓ Commit successful
echo.
echo ====================================================
echo.

echo Step 5: Pushing to remote...
echo ====================================================
echo Which branch are you pushing to?
echo   1. main
echo   2. master
echo   3. Other (you'll need to type it)
echo.
set /p branch_choice="Enter choice (1-3): "

if "%branch_choice%"=="1" set branch=main
if "%branch_choice%"=="2" set branch=master
if "%branch_choice%"=="3" (
    set /p branch="Enter branch name: "
)

echo.
echo Pushing to origin/%branch%...
git push origin %branch%

if %errorlevel% neq 0 (
    echo.
    echo ❌ Push failed!
    echo.
    echo Common solutions:
    echo   1. Run: git pull origin %branch% --rebase
    echo   2. Then run: git push origin %branch%
    echo.
    pause
    exit /b 1
)

echo.
echo ====================================================
echo   ✓ Successfully pushed to remote!
echo ====================================================
echo.
echo Verifying...
git log --oneline -3
echo.
echo ====================================================
echo   All done! Your NIO chat feature is now on remote
echo ====================================================
pause

