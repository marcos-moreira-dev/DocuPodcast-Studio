param([string]$Worker = 'target/qwen3-tts-worker-bin/llama-tts-worker.exe')
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$outputDir = Join-Path $root 'target/qwen-worker-smoke'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$start = [System.Diagnostics.ProcessStartInfo]::new()
$start.FileName = (Resolve-Path (Join-Path $root $Worker)).Path
$start.WorkingDirectory = Split-Path $start.FileName
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardInput = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
foreach ($argument in @('-m', (Join-Path $root 'models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf'),
    '-mm', (Join-Path $root 'models/tts/qwen3-tts/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf'),
    '-ngl', '99', '--no-mmproj-offload', '-t', '4', '--seed', '1234', '--temp', '0.75', '--top-k', '30', '--top-p', '0.90')) {
    $start.ArgumentList.Add($argument)
}
$workerProcess = [System.Diagnostics.Process]::Start($start)
$errors = $workerProcess.StandardError.ReadToEndAsync()
function Read-Reply {
    $pending = $workerProcess.StandardOutput.ReadLineAsync()
    if (-not $pending.Wait([TimeSpan]::FromMinutes(10))) { throw 'Worker response timeout' }
    if ($null -eq $pending.Result) { throw 'Worker exited before replying' }
    return $pending.Result
}
try {
    $timer = [System.Diagnostics.Stopwatch]::StartNew()
    $ready = Read-Reply
    if ($ready -ne 'READY') { throw "Unexpected startup: $ready" }
    Write-Output "READY PID=$($workerProcess.Id) loadSeconds=$([math]::Round($timer.Elapsed.TotalSeconds, 2))"
    $index = 0
    foreach ($phrase in @('El capitán saluda.', 'La señora abre la puerta.')) {
        $index++
        $promptPath = Join-Path $outputDir "prompt-$index.txt"
        $wavePath = Join-Path $outputDir "audio-$index.wav"
        $manifestPath = Join-Path $outputDir "request-$index.txt"
        [IO.File]::WriteAllText($promptPath, $phrase, [Text.UTF8Encoding]::new($false))
        [IO.File]::WriteAllLines($manifestPath, @($promptPath, $wavePath,
            (Join-Path $root 'samples/voices/advanced-presets/hombre_20_idealista_ecuador_dialogo/neutral.wav'),
            'es', '220'), [Text.UTF8Encoding]::new($false))
        $timer.Restart()
        $workerProcess.StandardInput.WriteLine($manifestPath)
        $workerProcess.StandardInput.Flush()
        $reply = Read-Reply
        if (-not $reply.StartsWith("OK`t")) { throw $reply }
        if ((Get-Item -LiteralPath $wavePath).Length -le 44) { throw 'Empty WAV' }
        Write-Output "UNIT=$index PID=$($workerProcess.Id) seconds=$([math]::Round($timer.Elapsed.TotalSeconds, 2)) $reply"
    }
    $workerProcess.StandardInput.WriteLine('QUIT')
    $workerProcess.StandardInput.Flush()
    if (-not $workerProcess.WaitForExit(10000)) { throw 'Worker did not release resources' }
    if ($workerProcess.ExitCode -ne 0) { throw "Worker exit $($workerProcess.ExitCode)" }
    Write-Output 'PASS: two units, one process, clean shutdown'
} finally {
    if (-not $workerProcess.HasExited) { $workerProcess.Kill($true); $workerProcess.WaitForExit() }
    [IO.File]::WriteAllText((Join-Path $outputDir 'native.log'), $errors.GetAwaiter().GetResult())
    $workerProcess.Dispose()
}
