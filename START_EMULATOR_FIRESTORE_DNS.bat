@echo off
setlocal

set "SDK_DIR=C:\Users\Huy Vu\AppData\Local\Android\Sdk"
set "EMULATOR_EXE=%SDK_DIR%\emulator\emulator.exe"
set "AVD_NAME=%~1"

if "%AVD_NAME%"=="" set "AVD_NAME=Pixel_5"

if not exist "%EMULATOR_EXE%" (
    echo [CFPLUS] Khong tim thay emulator.exe tai:
    echo %EMULATOR_EXE%
    pause
    exit /b 1
)

echo [CFPLUS] Neu emulator dang mo va bi loi DNS, hay dong emulator truoc.
echo [CFPLUS] Khoi dong AVD "%AVD_NAME%" voi DNS 8.8.8.8,1.1.1.1...
echo [CFPLUS] Sau khi emulator len xong, hay bam Run app trong Android Studio nhu binh thuong.

start "CFPLUS Emulator DNS" "%EMULATOR_EXE%" -avd "%AVD_NAME%" -dns-server 8.8.8.8,1.1.1.1

endlocal
