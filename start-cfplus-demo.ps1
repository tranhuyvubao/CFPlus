param(
    [switch]$InstallApp,
    [switch]$LaunchApp,
    [switch]$OpenWeb,
    [switch]$DeployHosting,
    [switch]$DeployFirestoreRules,
    [switch]$VisibleLogs,
    [int]$BackendPort = 3000,
    [int]$WebPort = 5173
)

$ErrorActionPreference = "Stop"

$Root = $PSScriptRoot
$BackendDir = Join-Path $Root "cfplus-backend"
$WebDir = Join-Path $Root "web-order"
$RuntimeDir = Join-Path $Root ".cfplus-runtime"
$LogsDir = Join-Path $RuntimeDir "logs"
$PidsFile = Join-Path $RuntimeDir "pids.json"

New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogsDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host "[CFPLUS] $Message" -ForegroundColor Cyan
}

function Write-Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Test-CommandExists {
    param([string]$Command)
    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function Test-PortOpen {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect("127.0.0.1", $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Invoke-HostingDeploy {
    if (-not (Test-CommandExists "firebase")) {
        throw "Khong tim thay Firebase CLI trong PATH. Hay cai firebase-tools va dang nhap `firebase login` truoc."
    }

    Write-Step "Dang deploy web-order len Firebase Hosting..."
    Push-Location $Root
    try {
        & firebase deploy --only hosting
        if ($LASTEXITCODE -ne 0) {
            throw "Firebase Hosting deploy that bai voi ma $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
    Write-Step "Deploy Hosting thanh cong."
}

function Invoke-FirestoreRulesDeploy {
    if (-not (Test-CommandExists "firebase")) {
        throw "Khong tim thay Firebase CLI trong PATH. Hay cai firebase-tools va dang nhap `firebase login` truoc."
    }

    Write-Step "Dang deploy Firestore rules..."
    Push-Location $Root
    try {
        & firebase deploy --only firestore:rules
        if ($LASTEXITCODE -ne 0) {
            throw "Firestore rules deploy that bai voi ma $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
    Write-Step "Deploy Firestore rules thanh cong."
}

function Save-Pid {
    param(
        [string]$Name,
        [int]$ProcessId
    )
    $data = @{}
    if (Test-Path $PidsFile) {
        try {
            $existing = Get-Content $PidsFile -Raw | ConvertFrom-Json
            foreach ($property in $existing.PSObject.Properties) {
                $data[$property.Name] = $property.Value
            }
        } catch {
            $data = @{}
        }
    }
    $data[$Name] = $ProcessId
    $data | ConvertTo-Json | Set-Content -Path $PidsFile -Encoding UTF8
}

function Start-BackgroundService {
    param(
        [string]$Name,
        [int]$Port,
        [string]$WorkingDirectory,
        [string]$Body
    )

    if (Test-PortOpen $Port) {
        Write-Step "$Name da dang chay tren cong $Port."
        return
    }

    $runner = Join-Path $RuntimeDir "$Name-run.ps1"
    $outLog = Join-Path $LogsDir "$Name.out.log"
    $errLog = Join-Path $LogsDir "$Name.err.log"

    $script = @"
`$ErrorActionPreference = "Continue"
Set-Location -LiteralPath "$WorkingDirectory"
& {
$Body
} 1>> "$outLog" 2>> "$errLog"
"@
    Set-Content -Path $runner -Value $script -Encoding UTF8

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = "powershell.exe"
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = -not $VisibleLogs
    $startInfo.Arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$runner`""

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()

    Save-Pid -Name $Name -ProcessId $process.Id
    Start-Sleep -Milliseconds 900

    if (Test-PortOpen $Port) {
        Write-Step "$Name da san sang tren cong $Port."
    } else {
        Write-Warn "$Name chua mo cong $Port. Xem log: $outLog va $errLog"
    }
}

function Convert-GradlePropertyPath {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }

    $decoded = $Value.Trim()
    $decoded = $decoded -replace "\\:", ":"
    $decoded = $decoded -replace "\\\\", "\"
    $decoded = $decoded -replace "\\ ", " "
    return $decoded
}

function Get-AdbPath {
    $localProperties = Join-Path $Root "local.properties"
    if (Test-Path $localProperties) {
        $sdkLine = Get-Content $localProperties | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($sdkLine) {
            $sdkDir = Convert-GradlePropertyPath ($sdkLine -replace "^sdk\.dir=", "")
            if ($sdkDir -and (Test-Path $sdkDir)) {
                $candidate = Join-Path $sdkDir "platform-tools\adb.exe"
                if (Test-Path $candidate) {
                    return $candidate
                }
            }
        }
    }

    $default = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $default) {
        return $default
    }
    return "adb"
}

function Test-AdbDevice {
    param([string]$Adb)
    try {
        $devices = & $Adb devices
        return ($devices | Select-String -Pattern "`tdevice").Count -gt 0
    } catch {
        return $false
    }
}

Write-Step "Khoi dong bo demo CFPLUS..."

if (-not (Test-CommandExists "node")) {
    throw "Chua cai Node.js hoac node khong co trong PATH. Can Node de chay backend chat va web-order local."
}

if ($DeployHosting) {
    Invoke-HostingDeploy
}

if ($DeployFirestoreRules) {
    Invoke-FirestoreRulesDeploy
}

$envPath = Join-Path $BackendDir ".env"
if (-not (Test-Path $envPath)) {
    Write-Warn "Chua co cfplus-backend\.env. Chat AI se khong goi duoc OpenRouter neu thieu key."
} else {
    $envContent = Get-Content $envPath -Raw
    if ($envContent -match "sk-or-your-openrouter-api-key|sk-your-openai-api-key") {
        Write-Warn "File .env van co key mau. Hay dien OPENROUTER_API_KEY that de chat AI thong minh."
    }
}

$backendBody = @"
if (!(Test-Path -LiteralPath "node_modules")) {
    npm install
}
`$env:PORT = "$BackendPort"
npm start
"@
Start-BackgroundService -Name "backend" -Port $BackendPort -WorkingDirectory $BackendDir -Body $backendBody

$staticServer = Join-Path $Root "scripts\cfplus-static-server.mjs"
$webBody = @"
node "$staticServer" --root "$WebDir" --port $WebPort
"@
Start-BackgroundService -Name "web-order" -Port $WebPort -WorkingDirectory $Root -Body $webBody

if ($InstallApp) {
    Write-Step "Build va cai app debug len thiet bi/emulator..."
    & (Join-Path $Root "gradlew.bat") app:installDebug --no-daemon
}

if ($LaunchApp) {
    $adb = Get-AdbPath
    if (Test-AdbDevice $adb) {
        Write-Step "Mo app tren emulator/thiet bi..."
        & $adb shell am start -n "com.example.do_an_hk1_androidstudio/.SplashActivity" | Out-Null
    } else {
        Write-Warn "Khong thay emulator/thiet bi Android dang ket noi. Hay mo emulator roi chay lai voi -LaunchApp."
    }
}

if ($OpenWeb) {
    Start-Process "http://127.0.0.1:$WebPort/menu.html?table=BAN01"
}

Write-Host ""
Write-Host "San sang demo:" -ForegroundColor Green
Write-Host "- Backend chat: http://127.0.0.1:$BackendPort/health"
Write-Host "- Web order:    http://127.0.0.1:$WebPort/menu.html?table=BAN01"
if ($DeployHosting) {
    Write-Host "- Hosting live: https://cafeplus-1fd32.web.app/menu.html?table=BAN01"
}
Write-Host "- Logs:         $LogsDir"
Write-Host "- Tat service:  .\STOP_CFPLUS_DEMO.bat"
