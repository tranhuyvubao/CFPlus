@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-cfplus-demo.ps1" -OpenWeb %*
echo.
echo Backend/web order da chay. Hay run app bang Android Studio.
echo Neu muon tat backend/web order, chay STOP_CFPLUS_DEMO.bat
pause
