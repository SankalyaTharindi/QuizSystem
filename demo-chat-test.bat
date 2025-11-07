@echo off
echo ============================================
echo   CHAT TEST DEMONSTRATION
echo ============================================
echo.
echo This script will guide you through testing the chat feature.
echo.
echo STEP 1: Start the Servers
echo --------------------------
echo Press any key to start both servers...
pause >nul
start "Quiz & Chat Servers" cmd /k "cd /d "%~dp0" && java src.ServerLauncher"
timeout /t 3 >nul
echo.
echo ✓ Servers started!
echo.
echo STEP 2: Start Client Windows
echo -----------------------------
echo.
echo Press any key to open Teacher client...
pause >nul
start "Teacher - admin" cmd /k "cd /d "%~dp0" && java src.QuizClient"
echo ✓ Teacher window opened
echo   → Login with: admin / 123 / Teacher
echo.
timeout /t 2 >nul
echo Press any key to open Student 1 (alice)...
pause >nul
start "Student - alice" cmd /k "cd /d "%~dp0" && java src.QuizClient"
echo ✓ Student 1 window opened
echo   → Login with: alice / student / Student
echo.
timeout /t 2 >nul
echo Press any key to open Student 2 (bob)...
pause >nul
start "Student - bob" cmd /k "cd /d "%~dp0" && java src.QuizClient"
echo ✓ Student 2 window opened
echo   → Login with: bob / student / Student
echo.
echo ============================================
echo   All windows opened!
echo ============================================
echo.
echo NOW YOU CAN TEST:
echo -----------------
echo 1. Login to all 3 windows with the credentials shown above
echo 2. In Teacher window, type: "Hello students!"
echo 3. In alice window, type: "Hi teacher!"
echo 4. In bob window, type: "Hello everyone!"
echo 5. Watch messages appear in all windows in real-time!
echo.
echo Look for:
echo   ✓ "[TIME] SYSTEM: alice joined the chat"
echo   ✓ "[TIME] SYSTEM: bob joined the chat"
echo   ✓ Messages appearing instantly in all windows
echo   ✓ Teacher labeled as "admin (Teacher)"
echo.
echo ============================================
echo Press any key to exit this script...
echo (The server and clients will keep running)
pause >nul

