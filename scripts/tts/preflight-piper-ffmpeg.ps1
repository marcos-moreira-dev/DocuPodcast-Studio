param(
    [switch]$FailOnMissing
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$reportRoot = Join-Path $root "target\docupodcast-engine-setup"
$reportPath = Join-Path $reportRoot "T90F_PIPER_FFMPEG_PREFLIGHT_REPORT.md"
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null

$piper = Join-Path $root "tools\piper\piper.exe"
$piperVoices = Join-Path $root "models\tts\piper\voices"
$piperModel = $null
if (Test-Path -LiteralPath $piperVoices -PathType Container) {
    $candidate = Get-ChildItem -LiteralPath $piperVoices -Recurse -Filter *.onnx -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($candidate) { $piperModel = $candidate.FullName }
}
if (-not $piperModel) { $piperModel = Join-Path $piperVoices "es_ES-default-medium.onnx" }
$ffmpeg = Join-Path $root "tools\ffmpeg\bin\ffmpeg.exe"
$ffprobe = Join-Path $root "tools\ffmpeg\bin\ffprobe.exe"

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

Add-Item "piper-exe" "Piper local" $true $piper (Test-Path -LiteralPath $piper -PathType Leaf) "Falta Piper local para fallback rapido." "Coloca tools\piper\piper.exe. No se usa PATH."
Add-Item "piper-model" "Voz Piper ONNX" $true $piperModel (Test-Path -LiteralPath $piperModel -PathType Leaf) "Falta una voz Piper .onnx." "Coloca una voz .onnx en models\tts\piper\voices."
Add-Item "ffmpeg-exe" "FFmpeg local" $true $ffmpeg (Test-Path -LiteralPath $ffmpeg -PathType Leaf) "Falta FFmpeg local para audio/video." "Coloca tools\ffmpeg\bin\ffmpeg.exe. No se usa PATH."
Add-Item "ffprobe-exe" "FFprobe local" $true $ffprobe (Test-Path -LiteralPath $ffprobe -PathType Leaf) "Falta FFprobe local para diagnostico y validacion de video." "Coloca tools\ffmpeg\bin\ffprobe.exe. No se usa PATH."

$missingRequired = @($items | Where-Object { $_.Required -and -not $_.Ok })
$overall = if ($missingRequired.Count -eq 0) { "OK" } else { "REQUIERE_PREPARACION" }

$ffmpegVersion = ""
$ffprobeVersion = ""
$encoderSummary = "no verificado"
if (Test-Path -LiteralPath $ffmpeg -PathType Leaf) {
    try {
        $ffmpegVersion = (& $ffmpeg -version 2>&1 | Select-Object -First 1) -join " "
        $encodersText = (& $ffmpeg -hide_banner -encoders 2>&1) -join "`n"
        $found = @()
        foreach ($encoder in @("libx264", "h264_nvenc", "h264_qsv", "h264_amf")) {
            if ($encodersText -match [regex]::Escape($encoder)) { $found += $encoder }
        }
        if ($found.Count -gt 0) { $encoderSummary = ($found -join ", ") }
    } catch {
        $encoderSummary = "no se pudo ejecutar ffmpeg -encoders"
    }
}
if (Test-Path -LiteralPath $ffprobe -PathType Leaf) {
    try { $ffprobeVersion = (& $ffprobe -version 2>&1 | Select-Object -First 1) -join " " } catch { $ffprobeVersion = "no se pudo ejecutar ffprobe -version" }
}


$content = @()
$content += "# T90F - Preflight Piper/FFmpeg locales"
$content += ""
$content += "- Estado: $overall"
$content += "- Raiz de app: ``$root``"
$content += "- Politica: solo artefactos repo-locales. No se usa PATH ni instalaciones globales."
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
$content += "## Versiones y encoders"
$content += ""
$content += "- FFmpeg: $ffmpegVersion"
$content += "- FFprobe: $ffprobeVersion"
$content += "- Encoders detectados: $encoderSummary"
$content += ""
$content += "## Smoke sugerido"
$content += ""
if ($missingRequired.Count -eq 0) {
    $content += '```bat'
    $content += "scripts\26-smoke-piper.bat"
    $content += "scripts\27-smoke-ffmpeg.bat"
    $content += '```'
} else {
    $content += "Completa los artefactos faltantes antes de ejecutar el smoke modular de Piper/FFmpeg."
}
Set-Content -LiteralPath $reportPath -Value ($content -join "`r`n") -Encoding UTF8

Write-Host "[DocuPodcast Studio] Preflight Piper/FFmpeg: $overall"
Write-Host "Reporte: $reportPath"
if ($FailOnMissing -and $missingRequired.Count -gt 0) {
    exit 1
}
exit 0
