@echo off
setlocal
cd /d "%~dp0"

if not exist ".env" (
  echo Missing .env file.
  echo Copy .env.example to .env and put your OPENROUTER_API_KEY there.
  pause
  exit /b 1
)

if not exist "node_modules" (
  echo Installing backend dependencies...
  call npm install
)

echo Starting CFPLUS backend on http://127.0.0.1:3000
echo Keep this window open while testing the Android chatbox.
node server.js
pause
