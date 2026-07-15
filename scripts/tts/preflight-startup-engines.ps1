param(
    [switch]$FailOnMissing
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$reportRoot = Join-Path $root "target\docupodcast-engine-startup-preflight"
$reportPath = Join-Path $reportRoot "T90E_STARTUP_ENGINE_PREFLIGHT_REPORT.md"
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null

$items = New-Object System.Collections.Generic.List[object]
function Add-Item([string]$Id, [string]$Name, [bool]$Required, [string]$PathText, [bool]$Ok, [string]$MissingMessage, [string]$Action) {
    $items.Add([pscustomobject]@{
        Id = $Id
        Name = $Name
        Required = $Required
        Path = $PathText
        Ok = $Ok
        Message = $(if ($Ok) { "$Name listo." } else { $MissingMessage })
        Action = $(if ($Ok) { "Sin accion pendiente." } else { $Action })
    }) | Out-Null
}

$python = Join-Path $root "tools\xtts-wrapper\.venv\Scripts\python.exe"
$wrapper = Join-Path $root "tools\xtts-wrapper\synthesize_xtts.py"
$bridge = Join-Path $root "scripts\tts\xtts-file-to-wav.ps1"
$model = Join-Path $root "models\tts\xtts"
$speaker = Join-Path $root "models\tts\xtts\speakers\voz-por-defecto.wav"
$piper = Join-Path $root "tools\piper\piper.exe"
$ffmpeg = Join-Path $root "tools\ffmpeg\bin\ffmpeg.exe"

Add-Item "coqui-python-local" "Python local Coqui/XTTS" $true $python (Test-Path -LiteralPath $python -PathType Leaf) "Falta Python local de Coqui/XTTS." "Ejecuta scripts\20-preparar-python-portable-coqui.bat. No se usa Python global."
Add-Item "coqui-wrapper" "Wrapper Coqui/XTTS" $true $wrapper (Test-Path -LiteralPath $wrapper -PathType Leaf) "Falta wrapper Coqui/XTTS." "Restaura tools\xtts-wrapper\synthesize_xtts.py."
Add-Item "coqui-script" "Script puente Coqui/XTTS" $true $bridge (Test-Path -LiteralPath $bridge -PathType Leaf) "Falta script puente Coqui/XTTS." "Restaura scripts\tts\xtts-file-to-wav.ps1."
Add-Item "coqui-model" "Modelo local Coqui/XTTS" $true $model (Test-Path -LiteralPath $model -PathType Container) "Falta carpeta del modelo Coqui/XTTS." "Coloca el modelo en models\tts\xtts."
Add-Item "coqui-speaker" "Voz por defecto" $true $speaker (Test-Path -LiteralPath $speaker -PathType Leaf) "Falta voz por defecto para Coqui/XTTS." "Coloca models\tts\xtts\speakers\voz-por-defecto.wav."
Add-Item "piper-exe" "Piper local" $false $piper (Test-Path -LiteralPath $piper -PathType Leaf) "Piper no esta disponible como fallback rapido." "Coloca tools\piper\piper.exe y una voz ONNX."
Add-Item "ffmpeg-exe" "FFmpeg local" $false $ffmpeg (Test-Path -LiteralPath $ffmpeg -PathType Leaf) "FFmpeg no esta disponible para preparar audio/video." "Coloca tools\ffmpeg\bin\ffmpeg.exe."

$missingRequired = @($items | Where-Object { $_.Required -and -not $_.Ok })
$overall = if ($missingRequired.Count -eq 0) { "OK" } else { "REQUIERE_PREPARACION" }

$content = @()
$content += "# T90E - Preflight de arranque de motores"
$content += ""
$content += "- Estado: $overall"
$content += "- Raiz de app: ``$root``"
$content += "- Politica Python: solo runtime local en ``tools\python`` / ``tools\xtts-wrapper\.venv``. No se usa Python global ni PATH."
$content += ""
$content += "## Elementos"
$content += ""
$content += "| Id | Elemento | Requerido | Estado | Ruta | Accion |"
$content += "|---|---|---:|---|---|---|"
foreach ($item in $items) {
    $state = if ($item.Ok) { "OK" } else { "FALTA" }
    $required = if ($item.Required) { "si" } else { "no" }
    $content += "| $($item.Id) | $($item.Name) | $required | $state | ``$($item.Path)`` | $($item.Action) |"
}
$content += ""
$content += "## Siguiente paso"
$content += ""
if ($missingRequired.Count -gt 0) {
    $content += "Ejecuta primero:"
    $content += ""
    $content += '```bat'
    $content += "scripts\20-preparar-python-portable-coqui.bat"
    $content += "scripts\22-verificar-coqui-xtts-local.bat"
    $content += '```'
} else {
    $content += "Coqui/XTTS tiene prerrequisitos locales minimos. Ejecuta:"
    $content += ""
    $content += '```bat'
    $content += "scripts\21-probar-coqui-xtts.bat"
    $content += "scripts\19-smoke-motores-reales.bat"
    $content += '```'
}
Set-Content -LiteralPath $reportPath -Value ($content -join "`r`n") -Encoding UTF8

Write-Host "[DocuPodcast Studio] Preflight de arranque: $overall"
Write-Host "Reporte: $reportPath"
if ($FailOnMissing -and $missingRequired.Count -gt 0) {
    exit 1
}
exit 0
