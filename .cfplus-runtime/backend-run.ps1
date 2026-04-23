$ErrorActionPreference = "Continue"
Set-Location -LiteralPath "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\cfplus-backend"
& {
if (!(Test-Path -LiteralPath "node_modules")) {
    npm install
}
$env:PORT = "3000"
npm start
} 1>> "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\.cfplus-runtime\logs\backend.out.log" 2>> "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\.cfplus-runtime\logs\backend.err.log"
