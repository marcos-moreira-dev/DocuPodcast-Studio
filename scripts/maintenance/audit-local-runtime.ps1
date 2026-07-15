[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))
$manifestPath = Join-Path $root "runtime\local-runtime-manifest.json"
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$missingRequired = [System.Collections.Generic.List[string]]::new()

foreach ($asset in $manifest.assets) {
    $path = [System.IO.Path]::GetFullPath((Join-Path $root $asset.path))
    $exists = Test-Path -LiteralPath $path -PathType Leaf
    $status = if ($exists) { "ready" } else { "missing" }
    $size = if ($exists) { [math]::Round((Get-Item -LiteralPath $path).Length / 1MB, 2) } else { 0 }
    Write-Host ("[{0}] {1} ({2} MB)" -f $status, $asset.path, $size)
    if (-not $exists -and $asset.required) {
        $missingRequired.Add($asset.path)
    }
}

if ($missingRequired.Count -gt 0) {
    Write-Error ("Missing required local runtime assets:`n - " + ($missingRequired -join "`n - "))
    exit 1
}

Write-Host "Local runtime audit passed."

