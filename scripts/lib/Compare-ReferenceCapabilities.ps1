param([string]$ReferenceRoot = '')

$ErrorActionPreference = 'Stop'
$ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if ([string]::IsNullOrWhiteSpace($ReferenceRoot)) {
    throw 'Indica -ReferenceRoot explicitamente. No se buscan copias de emergencia ni referencias hermanas.'
}
$ReferenceRoot = [System.IO.Path]::GetFullPath($ReferenceRoot)
if (-not (Test-Path -LiteralPath $ReferenceRoot -PathType Container)) {
    Write-Error "No existe la referencia de solo lectura: $ReferenceRoot"
    exit 1
}
if ($ProjectRoot.Equals($ReferenceRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Error 'La referencia no puede ser el mismo arbol que el proyecto actual.'
    exit 1
}

$reportDirectory = Join-Path $ProjectRoot 'target\parity'
New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
$report = Join-Path $reportDirectory 'reference-capabilities.md'
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Auditoria de paridad contra referencia de solo lectura')
$lines.Add('')
$lines.Add("- Actual: ``$ProjectRoot``")
$lines.Add("- Referencia: ``$ReferenceRoot``")
$lines.Add("- Fecha: ``$(Get-Date -Format o)``")
$lines.Add('- Politica: la referencia solo se enumera y lee; toda evidencia se escribe bajo `target/parity`.')
$lines.Add('')
$lines.Add('## Activos pesados')
$lines.Add('')
$lines.Add('| Familia | Actual | Referencia | Bytes actual | Bytes referencia | Faltantes | Tamano distinto |')
$lines.Add('|---|---:|---:|---:|---:|---:|---:|')
$failed = $false
foreach ($family in @('models', 'tools', 'runtime', 'voice-library')) {
    $currentBase = Join-Path $ProjectRoot $family
    $referenceBase = Join-Path $ReferenceRoot $family
    $currentFiles = @(Get-ChildItem -LiteralPath $currentBase -Recurse -File -ErrorAction SilentlyContinue)
    $referenceFiles = @(Get-ChildItem -LiteralPath $referenceBase -Recurse -File -ErrorAction SilentlyContinue)
    $current = @{}
    foreach ($file in $currentFiles) {
        $current[$file.FullName.Substring($currentBase.Length).TrimStart('\')] = $file.Length
    }
    $missing = 0
    $changed = 0
    foreach ($file in $referenceFiles) {
        $relative = $file.FullName.Substring($referenceBase.Length).TrimStart('\')
        if (-not $current.ContainsKey($relative)) { $missing++ }
        elseif ($current[$relative] -ne $file.Length) { $changed++ }
    }
    $currentBytes = ($currentFiles | Measure-Object Length -Sum).Sum
    $referenceBytes = ($referenceFiles | Measure-Object Length -Sum).Sum
    $lines.Add("| $family | $($currentFiles.Count) | $($referenceFiles.Count) | $currentBytes | $referenceBytes | $missing | $changed |")
    # A newer managed runtime may legitimately contain patched or additional assets. Missing
    # reference resources are blocking; different sizes require the behavioral evidence below.
    if ($missing -gt 0) { $failed = $true }
}

$critical = @(
    'scripts\tts\piper-file-to-wav.ps1',
    'scripts\tts\xtts-file-to-wav.ps1',
    'scripts\tts\xtts-batch-to-wav.ps1',
    'tools\piper\piper.exe',
    'tools\ffmpeg\bin\ffmpeg.exe',
    'models\image\workflows\workflow-sd15-reference.json',
    'models\video\workflows\workflow-wan22-ti2v-5b-api.json',
    'models\video\workflows\workflow-wan22-i2v-14b-api.json',
    'models\video\workflows\workflow-ltx23-i2v-portrait-api.json'
)
$lines.Add('')
$lines.Add('## Integridad critica')
$lines.Add('')
$lines.Add('| Recurso | Estado SHA-256 |')
$lines.Add('|---|---|')
foreach ($relative in $critical) {
    $current = Join-Path $ProjectRoot $relative
    $reference = Join-Path $ReferenceRoot $relative
    if (-not (Test-Path -LiteralPath $reference -PathType Leaf)) {
        $lines.Add("| ``$relative`` | No estaba en referencia |")
        continue
    }
    if (-not (Test-Path -LiteralPath $current -PathType Leaf)) {
        $lines.Add("| ``$relative`` | FALTA |")
        $failed = $true
        continue
    }
    $same = (Get-FileHash -LiteralPath $current -Algorithm SHA256).Hash -eq
            (Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash
    $lines.Add("| ``$relative`` | $(if ($same) { 'Coincide' } else { 'Distinto; requiere evidencia conductual' }) |")
}

$lines.Add('')
$lines.Add('## Contratos visibles de Estudio, video y exportacion')
$lines.Add('')
$lines.Add('| Contrato | Actual | Referencia | Estado |')
$lines.Add('|---|---:|---:|---|')

function Read-Matches([string]$Root, [string]$Relative, [string]$Pattern) {
    $path = Join-Path $Root $Relative
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { return @() }
    $text = [System.IO.File]::ReadAllText($path)
    return @([regex]::Matches($text, $Pattern) | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
}

function Add-SetContract([string]$Name, [string[]]$Current, [string[]]$Reference) {
    $currentSet = @($Current | Sort-Object -Unique)
    $referenceSet = @($Reference | Sort-Object -Unique)
    $missing = @($referenceSet | Where-Object { $_ -notin $currentSet })
    $status = if ($missing.Count -eq 0 -and $currentSet.Count -eq $referenceSet.Count) {
        'Conservado'
    } elseif ($missing.Count -eq 0) {
        'Conservado y ampliado'
    } else {
        'REGRESION: faltan ' + ($missing -join ', ')
    }
    $lines.Add("| $Name | $($currentSet.Count) | $($referenceSet.Count) | $status |")
    if ($missing.Count -gt 0) { $script:failed = $true }
}

$commandEnum = 'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\command\AppCommandId.java'
$shellView = 'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\shell\DocuPodcastShellView.java'
$dockEnum = 'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\sidedock\SideDockModuleId.java'
Add-SetContract 'AppCommandId publicados' `
    (Read-Matches $ProjectRoot $commandEnum '\b([A-Z][A-Z0-9_]+)\s*(?:\(|,|;)') `
    (Read-Matches $ReferenceRoot $commandEnum '\b([A-Z][A-Z0-9_]+)\s*(?:\(|,|;)')
Add-SetContract 'Handlers registrados en el shell' `
    (Read-Matches $ProjectRoot $shellView '\.register\(\s*AppCommandId\.([A-Z0-9_]+)') `
    (Read-Matches $ReferenceRoot $shellView '\.register\(\s*AppCommandId\.([A-Z0-9_]+)')
Add-SetContract 'Modulos laterales publicados' `
    (Read-Matches $ProjectRoot $dockEnum '\b([A-Z][A-Z0-9_]+)\s*(?:\(|,|;)') `
    (Read-Matches $ReferenceRoot $dockEnum '\b([A-Z][A-Z0-9_]+)\s*(?:\(|,|;)')

$requiredTokens = [ordered]@{
    'Contenido y ajustes de video documental' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\document\DocumentStudyVideoPanel.java',
        'document-study-video-edit-mode', 'Ajustes del video')
    'Superficie global de Video final' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\settings\SettingsDialog.java',
        'videoFinalPage', 'inspectFinalVideoRuntimeStatus', 'runtimeStatus.rendererName')
    'Runtime FFmpeg/FFprobe verificable' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\application\video\InspectFinalVideoRuntimeStatusUseCase.java',
        'report.ffmpegExecutable()', 'report.ffprobeExecutable()', 'report.effectiveEncoder(requested)')
    'Contrato de seis exportaciones creativas' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\export\ExportCenterCoordinator.java',
        'DOCUMENT_STUDY_VIDEO', 'THEATRE_SPATIAL_MAP', 'THEATRE_PORTION')
    'Cinco modulos teatrales' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\presentation\theatre\TheatreSideDock.java',
        'THEATRE_FRAGMENT_IMAGES', 'THEATRE_CHARACTERS', 'THEATRE_TEXTUAL_MAP',
        'THEATRE_SPATIAL_MAP', 'THEATRE_OBJECTS')
    'Roots de muestras de voz inyectados' = @(
        'src\main\java\com\marcosmoreiradev\docupodcaststudio\application\voice\VoiceReferenceSamplePathResolver.java',
        'installationPaths', 'runtimePaths')
}
$lines.Add('')
$lines.Add('| Capacidad funcional | Estado |')
$lines.Add('|---|---|')
foreach ($entry in $requiredTokens.GetEnumerator()) {
    $path = Join-Path $ProjectRoot $entry.Value[0]
    $text = if (Test-Path -LiteralPath $path -PathType Leaf) {
        [System.IO.File]::ReadAllText($path)
    } else { '' }
    $missingTokens = @($entry.Value[1..($entry.Value.Count - 1)] | Where-Object { -not $text.Contains($_) })
    $status = if ($missingTokens.Count -eq 0) { 'Conservada / ejecutable' }
        else { 'REGRESION: ' + ($missingTokens -join ', ') }
    $lines.Add("| $($entry.Key) | $status |")
    if ($missingTokens.Count -gt 0) { $failed = $true }
}

$lines.Add('')
$lines.Add('## Clases ausentes frente a la referencia')
$lines.Add('')
$lines.Add('| Clasificacion | Cantidad | Ejemplos |')
$lines.Add('|---|---:|---|')
$currentJavaRoot = Join-Path $ProjectRoot 'src\main\java'
$referenceJavaRoot = Join-Path $ReferenceRoot 'src\main\java'
$currentJava = @{}
Get-ChildItem -LiteralPath $currentJavaRoot -Recurse -Filter *.java -File | ForEach-Object {
    $currentJava[$_.FullName.Substring($currentJavaRoot.Length).TrimStart('\')] = $true
}
$classified = @{}
$unclassified = [System.Collections.Generic.List[string]]::new()
Get-ChildItem -LiteralPath $referenceJavaRoot -Recurse -Filter *.java -File | ForEach-Object {
    $relative = $_.FullName.Substring($referenceJavaRoot.Length).TrimStart('\')
    if ($currentJava.ContainsKey($relative)) { return }
    $category = switch -Regex ($relative) {
        '\\application\\document\\.*(?:Pdf|SourceCrop|DocumentOutlineHint)|\\infrastructure\\document\\Pdf' {
            'Retirada intencional: PDF V1'; break
        }
        '\\(?:application|presentation)\\ink\\|StudyProblemCanvasExport' {
            'Traslado de modulo: studio-ink'; break
        }
        '\\presentation\\settings\\|VoiceEngineSettingsControls' {
            'Reemplazo arquitectonico: administracion consolidada'; break
        }
        '\\presentation\\shell\\workflow\\' {
            'Reemplazo arquitectonico: controladores y casos de uso'; break
        }
        'ApplicationServices|ApplicationServicesFactory' {
            'Reemplazo arquitectonico: servicios por workspace'; break
        }
        'LocalTtsProcessAudioGenerationGateway|SettingsAware.*Gateway' {
            'Reemplazo arquitectonico: motores neutrales'; break
        }
        'ComfyUiConnectionSettings' {
            'Reemplazo arquitectonico: administracion ComfyUI'; break
        }
        default { '' }
    }
    if ([string]::IsNullOrWhiteSpace($category)) {
        $unclassified.Add($relative)
    } else {
        if (-not $classified.ContainsKey($category)) {
            $classified[$category] = [System.Collections.Generic.List[string]]::new()
        }
        $classified[$category].Add($relative)
    }
}
foreach ($category in ($classified.Keys | Sort-Object)) {
    $examples = @($classified[$category] | Select-Object -First 3 | ForEach-Object {
        [System.IO.Path]::GetFileName($_)
    }) -join ', '
    $lines.Add("| $category | $($classified[$category].Count) | $examples |")
}
$lines.Add("| Sin clasificar | $($unclassified.Count) | $(@($unclassified | Select-Object -First 5) -join ', ') |")
if ($unclassified.Count -gt 0) { $failed = $true }

$lines.Add('')
$lines.Add('## Cobertura conductual')
$lines.Add('')
$lines.Add('La matriz versionada esta en `docs/quality/capability-parity.md`. Los contratos y smokes son la autoridad para implementaciones refactorizadas que deliberadamente no coinciden byte a byte.')
[System.IO.File]::WriteAllLines($report, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "Auditoria de paridad: $report"
if ($failed) { exit 1 }
exit 0
