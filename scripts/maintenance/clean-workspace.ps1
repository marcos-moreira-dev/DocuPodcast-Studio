[CmdletBinding()]
param(
    [switch]$Apply,
    [switch]$ReferencesReviewed,
    [switch]$Moderate
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if (-not (Test-Path -LiteralPath (Join-Path $root 'pom.xml'))) { throw 'No es la raiz del reactor.' }
if ($Apply -and -not $ReferencesReviewed) { throw 'Revisa referencias y usos antes de aplicar; requiere -ReferencesReviewed.' }

# El modo predeterminado solo limpia derivados del compilador. -Moderate añade
# resultados regenerables revisados; nunca incluye modelos, runtimes, ejemplos,
# configuraciones, medios de usuario ni carpetas mixtas completas.
$modules = @('', 'studio-media-api', 'studio-ink', 'studio-desktop', 'studio-local-media-adapters', 'studio-launcher')
$leaves = @('classes', 'test-classes', 'generated-sources', 'generated-test-sources', 'maven-status', 'maven-archiver')
$items = [Collections.Generic.List[object]]::new()
function Assert-NoLinks([string]$Path) {
    $target = Get-Item -LiteralPath $Path -Force
    if ($target.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Enlace o junction: $($target.FullName)" }
    $cursor = if ($target.PSIsContainer) { $target } else { $target.Directory }
    while ($null -ne $cursor) {
        if ($cursor.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Enlace o junction: $($cursor.FullName)" }
        if ($cursor.FullName -eq $root) { break }
        $cursor = $cursor.Parent
    }
    if ($target.PSIsContainer) {
        $pending = [Collections.Generic.Stack[string]]::new()
        $pending.Push($Path)
        while ($pending.Count) {
            foreach ($entry in Get-ChildItem -LiteralPath $pending.Pop() -Force) {
                if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Enlace en destino: $($entry.FullName)" }
                if ($entry.PSIsContainer) { $pending.Push($entry.FullName) }
            }
        }
    }
}
function Add-CleanupCandidate([string]$Relative, [string]$Reason) {
    $path = [IO.Path]::GetFullPath((Join-Path $root $Relative))
    if (-not $path.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Destino fuera del repositorio.' }
    if (-not (Test-Path -LiteralPath $path)) { return }
    if (@($items | Where-Object { $_.Path -eq $path }).Count -gt 0) { return }
    Assert-NoLinks $path
    $tracked = @(& git -C $root ls-files -- $Relative)
    if ($LASTEXITCODE -ne 0 -or $tracked.Count) { throw "Destino contiene archivos versionados o no se pudo comprobar: $Relative" }
    $item = Get-Item -LiteralPath $path -Force
    $bytes = if ($item.PSIsContainer) {
        [long]$sum = 0
        Get-ChildItem -LiteralPath $path -Recurse -File -Force | ForEach-Object { $sum += $_.Length }
        $sum
    } else {
        [long]$item.Length
    }
    $items.Add([pscustomobject]@{Path=$path; Bytes=$bytes; Reason=$Reason; Removed=$false})
}
foreach ($module in $modules) {
    foreach ($leaf in $leaves) {
        $relative = if ($module) { "$module\target\$leaf" } else { "target\$leaf" }
        Add-CleanupCandidate $relative 'Derivado regenerable del compilador Maven'
    }
}
if ($Moderate) {
    $explicit = @(
        '.staging\clip-range-smoke.bin',
        '.staging\comfyui',
        '.staging\comfyui-controlnet-tile',
        '.staging\comfyui-real-esrgan',
        'dist\app-image',
        'generated\image-engine',
        'generated\image-engine-tests',
        'tmp\xtts-unicode-smoke.wav',
        'tmp\docupodcast-ui-run.out.log',
        'tmp\docupodcast-ui-run.err.log',
        'tmp\mvn-test-20260801.err.log',
        'tmp\mvn-test-20260801.out.log'
    )
    foreach ($relative in $explicit) { Add-CleanupCandidate $relative 'Resultado regenerable de compilación o experimento' }
    foreach ($container in @('diagnostics', 'logs')) {
        $containerPath = Join-Path $root $container
        if (Test-Path -LiteralPath $containerPath -PathType Container) {
            foreach ($entry in Get-ChildItem -LiteralPath $containerPath -Force) {
                Add-CleanupCandidate "$container\$($entry.Name)" 'Diagnóstico o log regenerable'
            }
        }
    }
    $targetPath = Join-Path $root 'target'
    if (Test-Path -LiteralPath $targetPath -PathType Container) {
        foreach ($entry in Get-ChildItem -LiteralPath $targetPath -Force | Where-Object Name -ne 'cleanup') {
            Add-CleanupCandidate "target\$($entry.Name)" 'Resultado regenerable de compilación o prueba'
        }
    }
}
if ($Apply) {
    # Rechazo conservador: no se interrumpe ningun proceso para poder limpiar.
    $active = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^(javaw?|DocuPodcastStudio|ffmpeg|jpackage)\.exe$' -or
        ($_.CommandLine -and $_.CommandLine.Contains($root) -and $_.CommandLine -match 'mvn|MavenWrapperMain')
    })
    if ($active.Count) { throw 'Hay procesos de compilacion, aplicacion o multimedia activos; no se borro nada.' }
    $reportRoot = Join-Path $root 'target\cleanup'
    New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
    $report = Join-Path $reportRoot ("cleanup-" + (Get-Date -Format 'yyyyMMdd-HHmmssfff') + '.csv')
    $items | Export-Csv -LiteralPath $report -NoTypeInformation -Encoding UTF8
    foreach ($item in $items) {
        Assert-NoLinks $item.Path
        Remove-Item -LiteralPath $item.Path -Recurse -Force
        $item.Removed = $true
        $items | Export-Csv -LiteralPath $report -NoTypeInformation -Encoding UTF8
    }
    Write-Host "Registro: $report"
}
$items | Format-Table Path, Bytes, Reason, Removed -AutoSize
$total = [long]0
foreach ($item in $items) { $total += $item.Bytes }
Write-Host ("Bytes " + $(if ($Apply) {'retirados'} else {'candidatos; dry-run'}) + ": $total")
