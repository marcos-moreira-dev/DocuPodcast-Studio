param(
    [string]$PythonVersion = "3.10.11",
    [string]$TtsRequirements = "tools\xtts-wrapper\requirements-xtts.txt",
    [string]$LocalTtsRepo = "",
    [switch]$SkipDependencyInstall
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$setupRoot = Join-Path $root "target\docupodcast-engine-setup"
$downloadRoot = Join-Path $setupRoot "downloads"
$logRoot = Join-Path $setupRoot "logs"
$pythonRoot = Join-Path $root "tools\python"
$pythonPackageRoot = Join-Path $pythonRoot ("python-nuget-" + $PythonVersion)
$pythonExe = Join-Path $pythonPackageRoot "tools\python.exe"
$venvDir = Join-Path $root "tools\xtts-wrapper\.venv"
$venvPython = Join-Path $venvDir "Scripts\python.exe"
$requirementsPath = Join-Path $root $TtsRequirements
$reportPath = Join-Path $setupRoot "T90B_COQUI_PYTHON_SETUP_REPORT.md"

New-Item -ItemType Directory -Force -Path $setupRoot, $downloadRoot, $logRoot, $pythonRoot | Out-Null
$steps = New-Object System.Collections.Generic.List[string]

function Add-Step([string]$name, [string]$status, [string]$detail) {
    $safe = $detail -replace "\r?\n", " "
    $steps.Add("| $name | $status | $safe |") | Out-Null
}

function Write-Report([string]$overall) {
    $content = @()
    $content += "# T90B - Preparacion Python local para Coqui/XTTS"
    $content += ""
    $content += "- Estado: $overall"
    $content += "- Raiz de app: ``$root``"
    $content += "- Python NuGet: ``$PythonVersion``"
    $content += "- Python repo-local: ``$pythonExe``"
    $content += "- Venv XTTS: ``$venvDir``"
    $content += "- Requirements: ``$requirementsPath``"
    $content += "- Repositorio TTS local: ``$LocalTtsRepo``"
    $content += "- Politica: no se usa Python global ni PATH para Coqui/XTTS."
    $content += ""
    $content += "## Pasos"
    $content += ""
    $content += "| Paso | Estado | Detalle |"
    $content += "|---|---|---|"
    $content += $steps
    $content += ""
    $content += "## Comandos siguientes"
    $content += ""
    $content += '```bat'
    $content += "scripts\22-verificar-coqui-xtts-local.bat"
    $content += "scripts\21-probar-coqui-xtts.bat"
    $content += "scripts\19-smoke-motores-reales.bat"
    $content += '```'
    Set-Content -LiteralPath $reportPath -Value ($content -join "`r`n") -Encoding UTF8
}

try {
    if (-not (Test-Path -LiteralPath $pythonExe)) {
        $packageUrl = "https://www.nuget.org/api/v2/package/python/$PythonVersion"
        $packageFile = Join-Path $downloadRoot ("python." + $PythonVersion + ".nupkg")
        $packageZip = Join-Path $downloadRoot ("python." + $PythonVersion + ".zip")
        Add-Step "Descarga Python" "RUNNING" $packageUrl
        Invoke-WebRequest -Uri $packageUrl -OutFile $packageFile -UseBasicParsing
        Copy-Item -LiteralPath $packageFile -Destination $packageZip -Force
        if (Test-Path -LiteralPath $pythonPackageRoot) {
            Remove-Item -LiteralPath $pythonPackageRoot -Recurse -Force
        }
        New-Item -ItemType Directory -Force -Path $pythonPackageRoot | Out-Null
        Expand-Archive -LiteralPath $packageZip -DestinationPath $pythonPackageRoot -Force
        if (-not (Test-Path -LiteralPath $pythonExe)) {
            throw "No se encontro python.exe luego de extraer el paquete NuGet: $pythonExe"
        }
        Add-Step "Descarga Python" "OK" "Python repo-local instalado en tools\\python."
    } else {
        Add-Step "Descarga Python" "OK" "Python repo-local ya existe."
    }

    $versionOutput = & $pythonExe -V 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo ejecutar Python repo-local: $versionOutput"
    }
    Add-Step "Verificar Python" "OK" ($versionOutput | Out-String).Trim()

    if (-not (Test-Path -LiteralPath $venvPython)) {
        if (Test-Path -LiteralPath $venvDir) {
            Remove-Item -LiteralPath $venvDir -Recurse -Force
        }
        & $pythonExe -m venv $venvDir 1> (Join-Path $logRoot "venv-stdout.log") 2> (Join-Path $logRoot "venv-stderr.log")
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $venvPython)) {
            throw "No se pudo crear el venv XTTS. Revisa target\\docupodcast-engine-setup\\logs\\venv-stderr.log"
        }
        Add-Step "Crear venv" "OK" "Venv creado en tools\\xtts-wrapper\\.venv."
    } else {
        Add-Step "Crear venv" "OK" "Venv XTTS ya existe."
    }

    & $venvPython -m pip install --upgrade pip setuptools wheel 1> (Join-Path $logRoot "pip-bootstrap-stdout.log") 2> (Join-Path $logRoot "pip-bootstrap-stderr.log")
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo actualizar pip/setuptools/wheel. Revisa pip-bootstrap-stderr.log"
    }
    Add-Step "Preparar pip" "OK" "pip/setuptools/wheel actualizados dentro del venv."

    if (-not $SkipDependencyInstall) {
        $env:PIP_CACHE_DIR = Join-Path $pythonRoot "pip-cache"
        New-Item -ItemType Directory -Force -Path $env:PIP_CACHE_DIR | Out-Null
        if ($LocalTtsRepo -and $LocalTtsRepo.Trim().Length -gt 0) {
            $repoPath = $LocalTtsRepo
            if (-not [System.IO.Path]::IsPathRooted($repoPath)) {
                $repoPath = Join-Path $root $repoPath
            }
            if (-not (Test-Path -LiteralPath $repoPath)) {
                throw "No existe el repositorio TTS local indicado: $repoPath"
            }
            if (-not ((Test-Path -LiteralPath (Join-Path $repoPath "pyproject.toml")) -or (Test-Path -LiteralPath (Join-Path $repoPath "setup.py")))) {
                throw "La carpeta indicada no parece un repositorio instalable de TTS: $repoPath"
            }
            & $venvPython -m pip install $repoPath 1> (Join-Path $logRoot "pip-xtts-stdout.log") 2> (Join-Path $logRoot "pip-xtts-stderr.log")
            if ($LASTEXITCODE -ne 0) {
                throw "No se pudo instalar TTS desde el repositorio local. Revisa target\docupodcast-engine-setup\logs\pip-xtts-stderr.log"
            }
            Add-Step "Instalar XTTS" "OK" "Dependencias instaladas desde repositorio TTS local: $repoPath."
        } else {
            if (-not (Test-Path -LiteralPath $requirementsPath)) {
                throw "No existe el archivo de requirements XTTS: $requirementsPath"
            }
            & $venvPython -m pip install -r $requirementsPath 1> (Join-Path $logRoot "pip-xtts-stdout.log") 2> (Join-Path $logRoot "pip-xtts-stderr.log")
            if ($LASTEXITCODE -ne 0) {
                throw "No se pudieron instalar dependencias XTTS. Revisa target\docupodcast-engine-setup\logs\pip-xtts-stderr.log"
            }
            Add-Step "Instalar XTTS" "OK" "Dependencias instaladas desde $TtsRequirements."
        }
    } else {
        Add-Step "Instalar XTTS" "OMITIDO" "SkipDependencyInstall activo."
    }

    & $venvPython (Join-Path $root "tools\xtts-wrapper\check_xtts_runtime.py") --skip-model-check 1> (Join-Path $logRoot "check-runtime-stdout.log") 2> (Join-Path $logRoot "check-runtime-stderr.log")
    if ($LASTEXITCODE -ne 0) {
        throw "El runtime Python existe, pero Coqui TTS no esta importable. Revisa check-runtime-stderr.log"
    }
    Add-Step "Verificar import TTS" "OK" "El paquete TTS es importable desde el venv."

    Write-Report "OK"
    Write-Host "OK: Python local y entorno Coqui/XTTS preparados. No se uso Python global."
    Write-Host "Reporte: $reportPath"
    exit 0
} catch {
    Add-Step "Error" "FALLO" $_.Exception.Message
    Write-Report "FALLO"
    Write-Error $_.Exception.Message
    exit 1
}
