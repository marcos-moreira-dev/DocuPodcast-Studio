param(
    [string]$Python = "",
    [string]$Wrapper = "",
    [string]$ModelDir = "",
    [Parameter(Mandatory = $true)][string]$Manifest,
    [string]$Language = "es",
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

if ([string]::IsNullOrWhiteSpace($Python)) {
    $Python = Join-Path $root "tools\xtts-wrapper\.venv\Scripts\python.exe"
}
if ([string]::IsNullOrWhiteSpace($Wrapper)) {
    $Wrapper = Join-Path $root "tools\xtts-wrapper\synthesize_xtts_batch.py"
}
if ([string]::IsNullOrWhiteSpace($ModelDir)) {
    $ModelDir = Join-Path $root "models\tts\xtts"
}

$Python = [System.IO.Path]::GetFullPath($Python)
$Wrapper = [System.IO.Path]::GetFullPath($Wrapper)
$ModelDir = [System.IO.Path]::GetFullPath($ModelDir)
$Manifest = [System.IO.Path]::GetFullPath($Manifest)

Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: script=$PSCommandPath"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: appRoot=$root"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: python=$Python"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: wrapper=$Wrapper"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: modelDir=$ModelDir"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: manifest=$Manifest"
Write-Host "DOCUPODCAST_XTTS_FORCE_BATCH: device=$Device"

if (-not (Test-Path -LiteralPath $Python -PathType Leaf)) {
    throw "Python local de Voz IA avanzada no existe: $Python"
}
if (-not (Test-Path -LiteralPath $Wrapper -PathType Leaf)) {
    throw "Wrapper batch de Voz IA avanzada no existe: $Wrapper"
}
if (-not (Test-Path -LiteralPath $Manifest -PathType Leaf)) {
    throw "Manifest batch de Voz IA avanzada no existe: $Manifest"
}

$required = @("model.pth", "config.json", "vocab.json")
foreach ($name in $required) {
    $candidate = Join-Path $ModelDir $name
    if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        throw "Modelo XTTS incompleto. Falta: $candidate"
    }
}

$arguments = @(
    $Wrapper,
    "--manifest", $Manifest,
    "--model-dir", $ModelDir,
    "--language", $Language
)
if (-not [string]::IsNullOrWhiteSpace($Device)) {
    $arguments += @("--device", $Device)
}

& $Python @arguments

if ($LASTEXITCODE -ne 0) {
    throw "Voz IA avanzada batch devolvio codigo de salida $LASTEXITCODE"
}
