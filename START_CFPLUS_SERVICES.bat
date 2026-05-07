@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-cfplus-demo.ps1" -DeployHosting -DeployFirestoreRules -OpenWeb %*
if errorlevel 1 (
echo.
echo Khoi dong demo that bai. Kiem tra log loi o tren.
pause
exit /b 1
)
echo.
echo Hosting + Firestore rules da duoc cap nhat, backend va web order local dang chay nen.
echo Chay STOP_CFPLUS_DEMO.bat de tat service local.
pause
