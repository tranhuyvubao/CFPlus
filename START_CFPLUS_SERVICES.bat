@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-cfplus-demo.ps1" -DeployHosting -OpenWeb %*
echo.
echo Hosting da duoc cap nhat, backend va web order local dang chay nen.
echo Chay STOP_CFPLUS_DEMO.bat de tat service local.
pause
