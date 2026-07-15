param(
    [Parameter(Mandatory=$true)][string]$Piper,
    [Parameter(Mandatory=$true)][string]$Model,
    [Parameter(Mandatory=$true)][string]$Text,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$ComputePolicy = "AUTO",
    [string]$Device = "",
    [string]$GpuIndex = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Piper -PathType Leaf)) {
    throw "Piper executable not found: $Piper"
}
if (-not (Test-Path -LiteralPath $Model -PathType Leaf)) {
    throw "Piper voice model not found: $Model"
}
if (-not (Test-Path -LiteralPath $Text -PathType Leaf)) {
    throw "Input text file not found: $Text"
}

$outputDir = Split-Path -Parent $Output
if ($outputDir -and -not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$content = Get-Content -LiteralPath $Text -Raw -Encoding UTF8
if ([string]::IsNullOrWhiteSpace($content)) {
    throw "Input text file is empty: $Text"
}

# DocuPodcast forwards CPU/GPU intent to the local voice process. GPU-capable Piper builds may
# honor these variables; CPU-only builds may ignore them or fail with their own diagnostic.
if ($ComputePolicy) {
    $env:DOCUPODCAST_COMPUTE_POLICY = $ComputePolicy
}
if ($Device) {
    $env:DOCUPODCAST_COMPUTE_DEVICE = $Device
    $env:DOCUPODCAST_PIPER_DEVICE = $Device
}
if ($GpuIndex -and $GpuIndex -ne "-1") {
    $deviceLower = if ($Device) { $Device.ToLowerInvariant() } else { "" }
    if ($deviceLower.StartsWith("gpu-nvidia")) {
        $env:CUDA_VISIBLE_DEVICES = $GpuIndex
        $env:NVIDIA_VISIBLE_DEVICES = $GpuIndex
    } elseif ($deviceLower.StartsWith("gpu-amd")) {
        $env:HIP_VISIBLE_DEVICES = $GpuIndex
        $env:ROCR_VISIBLE_DEVICES = $GpuIndex
    } elseif ($deviceLower.StartsWith("gpu-intel")) {
        $env:ONEAPI_DEVICE_SELECTOR = "level_zero:$GpuIndex"
    }
}

Write-Host "DOCUPODCAST_PIPER_DEVICE: policy=$ComputePolicy device=$Device gpuIndex=$GpuIndex"

# Piper reads text from stdin and writes WAV using --output_file.
# The wrapper keeps DocuPodcast jobs resumable because each segment text remains persisted on disk.
$content | & $Piper --model $Model --output_file $Output

if ($LASTEXITCODE -ne 0) {
    throw "Piper exited with code $LASTEXITCODE"
}
if (-not (Test-Path -LiteralPath $Output -PathType Leaf)) {
    throw "Piper did not create output WAV: $Output"
}
if ((Get-Item -LiteralPath $Output).Length -le 44) {
    throw "Piper output WAV is empty or invalid: $Output"
}
