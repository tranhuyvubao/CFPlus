$ErrorActionPreference = "Continue"
Set-Location -LiteralPath "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP"
& {
node "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\scripts\cfplus-static-server.mjs" --root "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\web-order" --port 5173
} 1>> "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\.cfplus-runtime\logs\web-order.out.log" 2>> "C:\Users\Huy Vu\Desktop\GIT\CFPLUS_APP\.cfplus-runtime\logs\web-order.err.log"
