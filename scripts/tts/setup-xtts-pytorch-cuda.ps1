param(
    [string]$TorchIndexUrl = "https://download.pytorch.org/whl/cu121",
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$python = Join-Path $root "tools\xtts-wrapper\.venv\Scripts\python.exe"
$reportDir = Join-Path $root "runtime\tts\xtts-smoke"
$report = Join-Path $reportDir "xtts-pytorch-cuda-install.log"

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
"DocuPodcast Studio - Preparacion PyTorch CUDA para Voz IA avanzada" | Set-Content -Path $report -Encoding UTF8
"Root: $root" | Add-Content -Path $report -Encoding UTF8
"Python local: $python" | Add-Content -Path $report -Encoding UTF8
"Torch index: $TorchIndexUrl" | Add-Content -Path $report -Encoding UTF8

function Invoke-LocalPython {
    param(
        [string[]]$Arguments,
        [string]$FailureMessage
    )
    $previousErrorActionPreference = $ErrorActionPreference
    $global:LASTEXITCODE = 0
    try {
        $ErrorActionPreference = "Continue"
        & $python @Arguments 2>&1 | ForEach-Object { $_.ToString() } | Tee-Object -FilePath $report -Append
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($exitCode -ne 0) {
        throw "$FailureMessage Codigo de salida: $exitCode. Revisa $report"
    }
}

if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
    throw "No existe el Python local de Voz IA avanzada: $python. Ejecuta primero scripts\20-preparar-python-portable-coqui.bat. No se usa Python global."
}

Write-Host "[DocuPodcast Studio] Preparando PyTorch CUDA dentro del Python local de Voz IA avanzada."
Write-Host "[DocuPodcast Studio] Python local: $python"
Write-Host "[DocuPodcast Studio] Index URL: $TorchIndexUrl"
Write-Host "[DocuPodcast Studio] Esta descarga puede ser grande. No toca Python global."

Invoke-LocalPython -Arguments @("-m", "pip", "--version") -FailureMessage "No se pudo ejecutar pip dentro del Python local de Voz IA avanzada."

$installArgs = @("-m", "pip", "install", "--upgrade", "--no-deps")
if ($Force) {
    $installArgs += "--force-reinstall"
}
$installArgs += @("torch", "torchvision", "torchaudio", "--index-url", $TorchIndexUrl)

Write-Host "[DocuPodcast Studio] Ejecutando instalacion local de PyTorch CUDA..."
Invoke-LocalPython -Arguments $installArgs -FailureMessage "La instalacion de PyTorch CUDA fallo."

Write-Host "[DocuPodcast Studio] Restaurando dependencias compatibles con Coqui TTS..."
Invoke-LocalPython -Arguments @("-m", "pip", "install", "numpy==1.22.0", "networkx==2.8.8") -FailureMessage "No se pudieron restaurar dependencias compatibles con Coqui TTS."

Write-Host "[DocuPodcast Studio] Verificando torch.cuda dentro del Python local..."
$probe = "import torch; print('torch=' + str(torch.__version__)); print('torch_cuda=' + str(torch.version.cuda)); print('cuda_available=' + str(torch.cuda.is_available())); print('device_count=' + str(torch.cuda.device_count())); print('device0=' + (torch.cuda.get_device_name(0) if torch.cuda.is_available() and torch.cuda.device_count() else ''))"
Invoke-LocalPython -Arguments @("-c", $probe) -FailureMessage "PyTorch se instalo, pero la verificacion CUDA fallo."

Write-Host "[DocuPodcast Studio] Verificando Coqui TTS y dependencias locales..."
$coquiProbe = "import TTS, numpy, networkx; print('TTS=' + str(getattr(TTS, '__version__', 'imported'))); print('numpy=' + str(numpy.__version__)); print('networkx=' + str(networkx.__version__))"
Invoke-LocalPython -Arguments @("-c", $coquiProbe) -FailureMessage "CUDA quedo instalada, pero Coqui TTS no pudo importarse."
Invoke-LocalPython -Arguments @("-m", "pip", "check") -FailureMessage "El Python local quedo con dependencias incompatibles."

Write-Host "[DocuPodcast Studio] Preparacion PyTorch CUDA finalizada. Ejecuta scripts\37-smoke-cuda-xtts.bat o el boton Probar GPU para Voz IA avanzada."
