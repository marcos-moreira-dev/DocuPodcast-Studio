[CmdletBinding()]
param(
    [switch]$Apply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$rootPrefix = $root.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar

if (-not (Test-Path -LiteralPath (Join-Path $root "pom.xml") -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $root "src\main\java") -PathType Container)) {
    throw "Workspace root validation failed: $root"
}

function Resolve-SafeWorkspacePath {
    param([Parameter(Mandatory = $true)][string]$RelativePath)

    $candidate = [System.IO.Path]::GetFullPath((Join-Path $root $RelativePath))
    if (-not $candidate.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing path outside workspace: $candidate"
    }
    return $candidate
}

function Get-PathSize {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return [int64]0
    }
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        return [int64](Get-Item -LiteralPath $Path).Length
    }
    $sum = [int64]0
    Get-ChildItem -LiteralPath $Path -File -Force -Recurse -ErrorAction SilentlyContinue |
        ForEach-Object { $sum += [int64]$_.Length }
    return $sum
}

$relativeTargets = [System.Collections.Generic.List[string]]::new()
@(
    "target",
    "generated",
    "tools\python\pip-cache",
    "tools\ffmpeg\downloads",
    "tools\image\ComfyUI\input",
    "tools\image\ComfyUI\output",
    "tools\image\ComfyUI\temp",
    "runtime\tts\xtts-batch-smoke",
    "runtime\tts\xtts-smoke"
) | ForEach-Object { $relativeTargets.Add($_) }

Get-ChildItem -LiteralPath (Join-Path $root "tools") -Directory -Filter "__pycache__" -Recurse -Force -ErrorAction SilentlyContinue |
    ForEach-Object { $relativeTargets.Add($_.FullName.Substring($rootPrefix.Length)) }

Get-ChildItem -LiteralPath (Join-Path $root "tools\image") -File -Filter "*.log" -Force -ErrorAction SilentlyContinue |
    ForEach-Object { $relativeTargets.Add($_.FullName.Substring($rootPrefix.Length)) }

$totalBytes = [int64]0
$removedBytes = [int64]0
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($relativePath in ($relativeTargets | Sort-Object -Unique)) {
    $path = Resolve-SafeWorkspacePath $relativePath
    if (-not (Test-Path -LiteralPath $path)) {
        continue
    }

    $bytes = Get-PathSize $path
    $totalBytes += $bytes
    $sizeMb = [math]::Round($bytes / 1MB, 2)

    $showPath = $sizeMb -ge 1 -or -not $relativePath.EndsWith("__pycache__", [System.StringComparison]::OrdinalIgnoreCase)
    if (-not $Apply) {
        if ($showPath) {
            Write-Host "[dry-run] $relativePath ($sizeMb MB)"
        }
        continue
    }

    try {
        Remove-Item -LiteralPath $path -Force -Recurse
        $removedBytes += $bytes
        if ($showPath) {
            Write-Host "[removed] $relativePath ($sizeMb MB)"
        }
    } catch {
        $failures.Add("$relativePath - $($_.Exception.Message)")
        Write-Warning "Could not remove ${relativePath}: $($_.Exception.Message)"
    }
}

$reportedBytes = if ($Apply) { $removedBytes } else { $totalBytes }
$reportedMb = [math]::Round($reportedBytes / 1MB, 2)
$mode = if ($Apply) { "removed" } else { "recoverable" }
Write-Host "Workspace cleanup ${mode}: $reportedMb MB"

if ($failures.Count -gt 0) {
    throw "Workspace cleanup completed with $($failures.Count) failure(s)."
}
