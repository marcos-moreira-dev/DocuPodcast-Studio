param([Parameter(Mandatory=$true)][string]$ProjectFile, [switch]$Apply)
$ErrorActionPreference = 'Stop'
$file = (Resolve-Path -LiteralPath $ProjectFile).Path
$root = Split-Path -Parent $file
$project = Get-Content -Raw -LiteralPath $file | ConvertFrom-Json
$changes = @()
$renames = @{}
foreach ($asset in $project.assets.items) {
    if ($asset.kind -ne 'IMAGE' -or $asset.relativePath -notmatch '(^|/)personajes/') { continue }
    $candidate = [IO.Path]::ChangeExtension($asset.relativePath, '.png').Replace('\','/')
    $absolute = [IO.Path]::GetFullPath((Join-Path $root $candidate))
    if (-not $absolute.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Ruta fuera del proyecto' }
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { throw "Falta PNG para $($asset.id): $absolute" }
    $stream = [IO.File]::OpenRead($absolute)
    try { $signature = New-Object byte[] 8; [void]$stream.Read($signature,0,8) } finally { $stream.Dispose() }
    if ([BitConverter]::ToString($signature) -ne '89-50-4E-47-0D-0A-1A-0A') { throw "No es PNG: $absolute" }
    $hash = (Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash.ToLowerInvariant()
    $renames[[IO.Path]::GetFileName($asset.relativePath)] = [IO.Path]::GetFileName($candidate)
    $changes += [pscustomobject]@{id=$asset.id;before=$asset.relativePath;after=$candidate;checksum=$hash}
    $asset.relativePath=$candidate; $asset.displayName=[IO.Path]::GetFileName($candidate)
    $asset.mimeType='image/png'; $asset.checksum=$hash
}
if (-not $Apply) { $changes | ConvertTo-Json -Depth 5; return }
$backup = Join-Path (Split-Path -Parent $root) ('png-repair-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup | Out-Null
function Write-BackedUp([string]$Target,[string]$Content) {
    $relative = [IO.Path]::GetRelativePath($root,$Target)
    if ($relative.StartsWith('..')) { throw 'Destino fuera del proyecto' }
    $copy = Join-Path $backup $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $copy) | Out-Null
    Copy-Item -LiteralPath $Target -Destination $copy
    [IO.File]::WriteAllText($Target,$Content,[Text.UTF8Encoding]::new($false))
}
Write-BackedUp $file ($project | ConvertTo-Json -Depth 100)
foreach ($md in Get-ChildItem -LiteralPath $root -Recurse -File -Filter '*.md') {
    $text = [IO.File]::ReadAllText($md.FullName); $updated=$text
    foreach ($old in $renames.Keys) { if ($old -ne $renames[$old]) { $updated=$updated.Replace($old,$renames[$old]) } }
    if ($updated -ne $text) { Write-BackedUp $md.FullName $updated }
}
foreach ($manifest in Get-ChildItem -LiteralPath $root -Recurse -File -Filter 'docupodcast-theatre.json') {
    $data=Get-Content -Raw -LiteralPath $manifest.FullName | ConvertFrom-Json
    foreach ($asset in $data.assets) {
        $old=[IO.Path]::GetFileName($asset.path)
        if ($asset.path -match '(^|/)personajes/' -and $renames.ContainsKey($old)) {
            $asset.path=$asset.path.Replace($old,$renames[$old])
            $absolute=Join-Path $manifest.DirectoryName $asset.path
            $asset.sha256=(Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash.ToLowerInvariant()
            $asset.size=(Get-Item -LiteralPath $absolute).Length
        }
    }
    Write-BackedUp $manifest.FullName ($data | ConvertTo-Json -Depth 100)
}
[pscustomobject]@{project=$file;backup=$backup;checked=$changes.Count;extensionChanges=@($changes | Where-Object {$_.before -ne $_.after}).Count} | ConvertTo-Json
