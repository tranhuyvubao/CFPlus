$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
$RuntimeDir = Join-Path $Root ".cfplus-runtime"
$PidsFile = Join-Path $RuntimeDir "pids.json"

function Write-Step {
    param([string]$Message)
    Write-Host "[CFPLUS] $Message" -ForegroundColor Cyan
}

function Stop-PortOwner {
    param([int]$Port)
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        foreach ($connection in $connections) {
            $pidValue = [int]$connection.OwningProcess
            $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
            if ($process) {
                Write-Step "Dang tat process giu cong $Port (PID $pidValue)..."
                Stop-Process -Id $pidValue -Force
            }
        }
    } catch {
    }
}

if (-not (Test-Path $PidsFile)) {
    Write-Step "Khong co service nao duoc ghi nhan dang chay."
    exit 0
}

try {
    $pids = Get-Content $PidsFile -Raw | ConvertFrom-Json
    foreach ($property in $pids.PSObject.Properties) {
        $name = $property.Name
        $pidValue = [int]$property.Value
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($process) {
            Write-Step "Dang tat $name (PID $pidValue)..."
            Stop-Process -Id $pidValue -Force
        } else {
            Write-Step "$name khong con chay."
        }
    }
    Remove-Item -LiteralPath $PidsFile -Force
    Stop-PortOwner -Port 3000
    Stop-PortOwner -Port 5173
    Write-Step "Da tat xong service CFPLUS."
} catch {
    Write-Host "[WARN] Khong doc duoc pids.json: $($_.Exception.Message)" -ForegroundColor Yellow
    Stop-PortOwner -Port 3000
    Stop-PortOwner -Port 5173
}
