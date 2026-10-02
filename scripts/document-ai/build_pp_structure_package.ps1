param(
    [Parameter(Mandatory = $true)]
    [string]$Recipe,
    [Parameter(Mandatory = $true)]
    [string]$Destination
)

$ErrorActionPreference = "Stop"
$recipePath = (Resolve-Path -LiteralPath $Recipe).Path
$destinationPath = [System.IO.Path]::GetFullPath($Destination)
$recipeData = Get-Content -LiteralPath $recipePath -Raw -Encoding UTF8 | ConvertFrom-Json

if ($recipeData.pythonVersion -ne "3.12.11" -or
    $recipeData.paddleVersion -ne "3.3.0" -or
    $recipeData.paddleOcrVersion -ne "3.7.0") {
    throw "La receta debe fijar Python 3.12.11, PaddlePaddle 3.3.0 y PaddleOCR 3.7.0."
}
if ($recipeData.profile -notin @("cpu", "gpu-cu118")) {
    throw "El perfil debe ser cpu o gpu-cu118."
}

$parent = Split-Path -Parent $destinationPath
$staging = Join-Path $parent (".pp-structure-staging-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $staging -Force | Out-Null

function Get-Sha256([string]$Path) {
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

try {
    foreach ($artifact in $recipeData.artifacts) {
        if ([string]::IsNullOrWhiteSpace($artifact.url) -or
            [string]::IsNullOrWhiteSpace($artifact.sha256) -or
            [string]::IsNullOrWhiteSpace($artifact.destination)) {
            throw "Cada artefacto requiere url, sha256 y destination."
        }
        $relative = $artifact.destination.Replace("/", [System.IO.Path]::DirectorySeparatorChar)
        if ([System.IO.Path]::IsPathRooted($relative) -or $relative.Contains("..")) {
            throw "Ruta de artefacto no trasladable: $relative"
        }
        $download = Join-Path $staging ("download-" + [Guid]::NewGuid().ToString("N"))
        Invoke-WebRequest -Uri $artifact.url -OutFile $download -UseBasicParsing
        if ($artifact.size -gt 0 -and (Get-Item -LiteralPath $download).Length -ne [long]$artifact.size) {
            throw "Tamaño inesperado para $($artifact.url)"
        }
        if ((Get-Sha256 $download) -ne $artifact.sha256.ToLowerInvariant()) {
            throw "SHA-256 inesperado para $($artifact.url)"
        }
        $target = Join-Path $staging $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
        if ($artifact.archive -eq "zip") {
            New-Item -ItemType Directory -Path $target -Force | Out-Null
            Expand-Archive -LiteralPath $download -DestinationPath $target -Force
            Remove-Item -LiteralPath $download -Force
        } else {
            Move-Item -LiteralPath $download -Destination $target
        }
    }

    $required = @(
        "python/python.exe",
        "bridge/pp_structure_v3.py",
        "models/manifest.json"
    )
    foreach ($requiredFile in $required) {
        if (-not (Test-Path -LiteralPath (Join-Path $staging $requiredFile) -PathType Leaf)) {
            throw "El paquete construido no contiene $requiredFile."
        }
    }
    if (Test-Path -LiteralPath (Join-Path $staging ".venv")) {
        throw "No se permiten entornos virtuales trasladados."
    }

    $files = Get-ChildItem -LiteralPath $staging -Recurse -File |
        Where-Object { $_.Name -ne "package-manifest.json" } |
        ForEach-Object {
            [ordered]@{
                path = [System.IO.Path]::GetRelativePath($staging, $_.FullName).Replace("\", "/")
                size = $_.Length
                sha256 = Get-Sha256 $_.FullName
            }
        }
    $manifest = [ordered]@{
        schemaVersion = 1
        pythonVersion = $recipeData.pythonVersion
        paddleVersion = $recipeData.paddleVersion
        paddleOcrVersion = $recipeData.paddleOcrVersion
        profile = $recipeData.profile
        license = $recipeData.license
        sources = $recipeData.sources
        files = @($files)
    }
    $manifest | ConvertTo-Json -Depth 8 |
        Set-Content -LiteralPath (Join-Path $staging "package-manifest.json") -Encoding UTF8

    if (Test-Path -LiteralPath $destinationPath) {
        throw "El destino ya existe; no se sobrescribe: $destinationPath"
    }
    Move-Item -LiteralPath $staging -Destination $destinationPath
    Write-Output "Paquete PP-StructureV3 publicado en $destinationPath"
} finally {
    if (Test-Path -LiteralPath $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}
