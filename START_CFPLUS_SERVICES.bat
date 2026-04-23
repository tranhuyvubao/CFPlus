@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-cfplus-demo.ps1" -OpenWeb %*
echo.
echo Backend va web order dang chay nen. Chay STOP_CFPLUS_DEMO.bat de tat.
pause
