param(
    [int]$CpuColumn = 9
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$informesDir = Split-Path -Parent $scriptDir
$repoRoot = Split-Path -Parent $informesDir
$workspaceRoot = Split-Path -Parent $repoRoot

$localAdb = Join-Path $workspaceRoot ".android-sdk\platform-tools\adb.exe"
if (Test-Path -LiteralPath $localAdb) {
    $adb = $localAdb
} else {
    $adb = "adb"
}

$manualApk = Join-Path $repoRoot "Android\Proyecto_sin_Vibecoding\app\build\outputs\apk\debug\app-debug.apk"
$vibeApk = Join-Path $repoRoot "Android\Proyecto_con_Vibecoding\app\build\outputs\apk\debug\app-debug.apk"
$metricScript = Join-Path $scriptDir "get_cpu_mem_smarthpone.sh"
$remoteScript = "/data/local/tmp/get_cpu_mem_smarthpone.sh"
$outFile = Join-Path $informesDir ("metricas_resultados_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".txt")

function Run-Adb {
    param([string[]]$Args)
    & $adb @Args
}

function Wait-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host $Message -ForegroundColor Cyan
    Read-Host "Presiona ENTER para continuar"
}

function Measure-Case {
    param(
        [string]$Title,
        [string]$PackageName,
        [string]$Instruction
    )
    Wait-Step "$Title - $Instruction"
    Add-Content -LiteralPath $outFile -Value ""
    Add-Content -LiteralPath $outFile -Value "=============================="
    Add-Content -LiteralPath $outFile -Value $Title
    Add-Content -LiteralPath $outFile -Value "Package: $PackageName"
    Add-Content -LiteralPath $outFile -Value "CPU column: $CpuColumn"
    Add-Content -LiteralPath $outFile -Value "=============================="
    Run-Adb @("shell", "sh", $remoteScript, $PackageName, "$CpuColumn") | Tee-Object -FilePath $outFile -Append
}

Write-Host "Usando adb: $adb"
Run-Adb @("devices")

if (!(Test-Path -LiteralPath $manualApk)) {
    throw "No existe APK manual. Compilar con: .\gradlew.bat :app:assembleDebug en Android\Proyecto_sin_Vibecoding"
}
if (!(Test-Path -LiteralPath $vibeApk)) {
    throw "No existe APK vibecoding. Compilar con: .\gradlew.bat :app:assembleDebug en Android\Proyecto_con_Vibecoding"
}

Wait-Step "Verifica que el telefono aparezca como 'device' en adb devices y que aceptaste el permiso RSA."

Write-Host "Instalando APK manual..."
Run-Adb @("install", "-r", $manualApk)

Write-Host "Instalando APK vibecoding..."
Run-Adb @("install", "-r", $vibeApk)

Write-Host "Subiendo script de metricas..."
Run-Adb @("push", $metricScript, $remoteScript)
Run-Adb @("shell", "chmod", "755", $remoteScript)

Write-Host ""
Write-Host "Si la columna de CPU de 'adb shell top' no es 9, relanza este script con -CpuColumn N." -ForegroundColor Yellow
Write-Host "Ejemplo: .\run_metricas_android.ps1 -CpuColumn 10"

Measure-Case "1. Manual en reposo" "dev.mnovoa.SOA" "Abrir la app manual y no interactuar durante la medicion."
Measure-Case "2. Manual con accion de sensor" "dev.mnovoa.SOA" "Abrir SensorActivity manual e inclinar/mover el telefono durante la medicion."
Measure-Case "3. Vibecoding en reposo" "dev.mnovoa.SOA.vibecoding" "Abrir la app vibecoding y no interactuar durante la medicion."
Measure-Case "4. Vibecoding con accion de sensor" "dev.mnovoa.SOA.vibecoding" "Abrir SensorActivity vibecoding e inclinar/mover el telefono durante la medicion."

Write-Host ""
Write-Host "Resultados guardados en:" -ForegroundColor Green
Write-Host $outFile
