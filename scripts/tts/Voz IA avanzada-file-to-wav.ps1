param(
    [Parameter(Mandatory=$true)][string]$Python,
    [Parameter(Mandatory=$true)][string]$Wrapper,
    [Parameter(Mandatory=$true)][string]$ModelDir,
    [Parameter(Mandatory=$true)][string]$SpeakerWav,
    [Parameter(Mandatory=$true)][string]$Text,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$Language = "es",
    [string]$Device = ""
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

$forcedPython = Join-Path $root "tools\xtts-wrapper\.venv\Scripts\python.exe"
$forcedWrapper = Join-Path $root "tools\xtts-wrapper\synthesize_xtts.py"
$forcedModelDir = Join-Path $root "models\tts\xtts"
$forcedSpeaker = Join-Path $forcedModelDir "speakers\voz-por-defecto.wav"

function Test-XttsModelFolder([string]$Directory) {
    if ([string]::IsNullOrWhiteSpace($Directory)) {
        return $false
    }
    return (Test-Path -LiteralPath (Join-Path $Directory "model.pth") -PathType Leaf) `
        -and (Test-Path -LiteralPath (Join-Path $Directory "config.json") -PathType Leaf) `
        -and (Test-Path -LiteralPath (Join-Path $Directory "vocab.json") -PathType Leaf)
}

function Test-LegacyPath([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }
    $v = $Value.Replace('\','/').ToLowerInvariant()
    return $v.Contains('recursos locales ia avanzada') `
        -or $v.Contains('componentes locales ia avanzada-wrapper') `
        -or $v.Contains('/model.pth/model.pth')
}

function Write-ReceivedArg([string]$Label, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return
    }
    if (Test-LegacyPath $Value) {
        Write-Host "DOCUPODCAST_XTTS_FORCE: ignoredLegacyArg=$Label"
        return
    }
    Write-Host "DOCUPODCAST_XTTS_FORCE: ignoredArg=$Label"
}

function Get-NormalizedReceivedModelDir([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }
    $received = $Value
    $normalized = $received.Replace('\','/')
    if ($normalized.ToLowerInvariant().EndsWith('/model.pth')) {
        Write-Host "DOCUPODCAST_XTTS_FORCE: model-dir-termina-en-model-pth"
        Write-Host "DOCUPODCAST_XTTS_FORCE: model-dir-apunta-a-model-pth; evitar model.pth/model.pth"
        $received = Split-Path -Parent $received
    }
    if (-not (Test-XttsModelFolder $received)) {
        Write-Host "DOCUPODCAST_XTTS_FORCE: model-dir-legacy-o-incompleto"
    }
    return $received
}

Write-Host "DOCUPODCAST_XTTS_FORCE: script=$PSCommandPath"
Write-Host "DOCUPODCAST_XTTS_FORCE: appRoot=$root"
Write-Host "DOCUPODCAST_XTTS_FORCE: redirigiendo a xtts-file-to-wav.ps1"
Write-ReceivedArg "python" $Python
Write-ReceivedArg "wrapper" $Wrapper
Write-ReceivedArg "modelDir" $ModelDir
$null = Get-NormalizedReceivedModelDir $ModelDir

$Python = $forcedPython
$Wrapper = $forcedWrapper
$ModelDir = $forcedModelDir

if (-not (Test-Path -LiteralPath $Python -PathType Leaf)) {
    throw "No existe el Python local de Coqui/XTTS: $Python. Python no permitido para Coqui/XTTS fuera del runtime portable. Ejecuta scripts\20-preparar-python-portable-coqui.bat desde esta misma carpeta. No se usa Python global."
}
if (-not (Test-Path -LiteralPath $Wrapper -PathType Leaf)) {
    throw "No existe el wrapper administrado de Voz IA avanzada: $Wrapper. Revisa tools\xtts-wrapper."
}
if (-not (Test-XttsModelFolder $ModelDir)) {
    throw "No existe modelo XTTS portable valido en $ModelDir. Debe contener model.pth, config.json y vocab.json."
}
if (-not (Test-Path -LiteralPath $Text -PathType Leaf)) {
    throw "No existe el texto de entrada: $Text"
}

if ([string]::IsNullOrWhiteSpace($SpeakerWav) -or -not (Test-Path -LiteralPath $SpeakerWav -PathType Leaf)) {
    if (Test-Path -LiteralPath $forcedSpeaker -PathType Leaf) {
        Write-Host "DOCUPODCAST_XTTS_FORCE: speakerFallback=portable-default"
        $SpeakerWav = $forcedSpeaker
    }
}
if (-not (Test-Path -LiteralPath $SpeakerWav -PathType Leaf)) {
    throw "No existe la muestra de voz: $SpeakerWav. Tambien falta la muestra portable: $forcedSpeaker"
}

$legacyCheck = ($Python + ' ' + $Wrapper + ' ' + $ModelDir).Replace('\','/').ToLowerInvariant()
if ($legacyCheck.Contains('recursos locales ia avanzada') -or $legacyCheck.Contains('componentes locales ia avanzada-wrapper') -or $legacyCheck.Contains('/model.pth/model.pth')) {
    Write-Host "DOCUPODCAST_XTTS_FORCE: refusing legacy path=forced-runtime"
    throw "Guardarrail XTTS: despues de forzar runtime portable aun queda una ruta legacy en el comando."
}

Write-Host "DOCUPODCAST_XTTS_FORCE: python=$Python"
Write-Host "DOCUPODCAST_XTTS_FORCE: wrapper=$Wrapper"
Write-Host "DOCUPODCAST_XTTS_FORCE: modelDir=$ModelDir"
Write-Host "DOCUPODCAST_XTTS_FORCE: model-normalizado=$ModelDir"
Write-Host "DOCUPODCAST_XTTS_FORCE: speaker=$SpeakerWav"
Write-Host "DOCUPODCAST_XTTS_FORCE: device=$Device"

$outDir = Split-Path -Parent $Output
if ($outDir -and -not (Test-Path -LiteralPath $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

$argsList = @(
    $Wrapper,
    "--text-file", $Text,
    "--output", $Output,
    "--speaker-wav", $SpeakerWav,
    "--language", $Language,
    "--model-dir", $ModelDir
)

if ($Device -and $Device.Trim().Length -gt 0) {
    $argsList += @("--device", $Device)
}

$env:PYTHONUNBUFFERED = "1"
$env:PYTHONIOENCODING = "utf-8"

Write-Host "DOCUPODCAST_XTTS_FORCE: iniciando wrapper local"
& $Python @argsList

if ($LASTEXITCODE -ne 0) {
    throw "Voz IA avanzada devolvio codigo de salida $LASTEXITCODE"
}
