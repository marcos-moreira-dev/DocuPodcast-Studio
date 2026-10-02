param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Diagnose', 'RunApp', 'Test', 'Verify', 'RealSmoke', 'PackageAppImage', 'Diagnostics')]
    [string]$Task,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$TaskArguments
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$LauncherPom = Join-Path $ProjectRoot 'studio-launcher\pom.xml'
$Version = '0.0.1-onboarding'

function Show-TaskHelp {
    Write-Host "DocuPodcast: $Task"
    Write-Host 'Uso: scripts\<acceso>.bat [opciones]. Salida: 0 correcto, 1 fallo.'
    Write-Host 'Requisitos: Windows x64, Temurin 21, Maven 3.9+ y toolchain configurado.'
    switch ($Task) {
        'Diagnose' { Write-Host '--resolve-native: permite resolver el auxiliar de tinta con Maven. Sin opciones solo inspecciona; no compila ni descarga.' }
        'RunApp' { Write-Host '--smoke: compila el reactor necesario, abre el launcher y sale. Sin opciones abre la aplicacion. Salidas de compilacion: target.' }
        'Test' { Write-Host 'Sin opciones: suite determinista Maven, sin instalar motores. Informes: target de cada modulo.' }
        'Verify' { Write-Host '--reference-root=<ruta>: paridad historica opcional. Ejecuta verify, GUI headless y smoke del launcher. Informes: target.' }
        'RealSmoke' {
            Write-Host '--tier=short|soak --engine=<motor> --preset=<perfil> --required=<lista> --resume --timeout-hours=1..6'
            Write-Host '--prompt=<texto> --prompt-file=<archivo> --label=<nombre> --seed=<entero> --width=<pixeles> --height=<pixeles>'
            Write-Host 'OPTATIVO: genera medios reales; puede tardar horas. Soak exige motor y perfil. Evidencia: target/certification.'
        }
        'PackageAppImage' { Write-Host 'Sin opciones: compila y crea app-image ligera en dist/app-image; no incluye modelos ni entornos Python.' }
        'Diagnostics' { Write-Host '--resolve-native --reference-root=<ruta>: comprobaciones adicionales explicitas. Informe y ZIP local en target/diagnostics incluso si falla una comprobacion.' }
    }
    Write-Host '--help: muestra esta ayuda sin ejecutar la tarea. Nunca modifica PATH ni descarga modelos.'
}

function Get-TaskOption([string]$Name) {
    $prefix = "--$Name="
    foreach ($argument in $TaskArguments) {
        if ($argument.StartsWith($prefix, [StringComparison]::Ordinal)) { return $argument.Substring($prefix.Length) }
    }
    return ''
}

function Assert-TaskArguments {
    $allowed = switch ($Task) {
        'Diagnose' { @('--resolve-native') }
        'RunApp' { @('--smoke') }
        'Verify' { @('--reference-root=') }
        'Diagnostics' { @('--resolve-native', '--reference-root=') }
        'RealSmoke' { @('--resume', '--tier=', '--engine=', '--preset=', '--required=', '--timeout-hours=', '--prompt=', '--prompt-file=', '--label=', '--seed=', '--width=', '--height=') }
        default { @() }
    }
    $seen = @{}
    foreach ($argument in $TaskArguments) {
        $name = ($argument -split '=', 2)[0]
        if ($seen.ContainsKey($name)) { throw "Opcion repetida: $name" }
        $seen[$name] = $true
        $valid = $false
        foreach ($option in $allowed) {
            if ($option.EndsWith('=')) {
                if ($argument.StartsWith($option, [StringComparison]::Ordinal) -and -not [string]::IsNullOrWhiteSpace($argument.Substring($option.Length))) { $valid = $true }
            } elseif ($argument -eq $option) { $valid = $true }
        }
        if (-not $valid) { throw "Opcion desconocida o vacia para ${Task}: $name. Consulta --help." }
    }
    if ($Task -eq 'RealSmoke') {
        foreach ($numeric in @('width', 'height', 'timeout-hours')) {
            $value = Get-TaskOption $numeric
            $parsed = 0
            if ($value -and (-not [int]::TryParse($value, [ref]$parsed) -or $parsed -lt 1 -or ($numeric -eq 'timeout-hours' -and $parsed -gt 6))) {
                throw "Valor invalido para --$numeric. Debe ser positivo; timeout-hours admite 1..6."
            }
        }
        $seed = Get-TaskOption 'seed'
        $seedValue = [long]0
        if ($seed -and -not [long]::TryParse($seed, [ref]$seedValue)) { throw 'seed debe ser un entero.' }
        foreach ($name in @('engine', 'preset', 'label')) {
            $value = Get-TaskOption $name
            if ($value -and $value -notmatch '^[a-zA-Z0-9][a-zA-Z0-9_.+-]*$') { throw "Identificador invalido: --$name" }
        }
    }
}

function Invoke-OptionalParity {
    $reference = Get-TaskOption 'reference-root'
    if (-not $reference) { Write-Host 'NO EJECUTADA: paridad historica (requiere --reference-root).'; return }
    if (-not (Test-Path -LiteralPath $reference -PathType Container)) { throw "No existe la referencia solicitada: $reference" }
    & (Join-Path $ProjectRoot 'scripts\lib\Compare-ReferenceCapabilities.ps1') -ReferenceRoot $reference
    if ($LASTEXITCODE -ne 0) { throw 'La auditoria de paridad fallo.' }
}

function Invoke-CommandChecked {
    param([string]$Command, [string[]]$Arguments)
    Write-Host ("> {0} {1}" -f $Command, ($Arguments -join ' '))
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "El comando '$Command' termino con codigo $LASTEXITCODE."
    }
}

function Get-NativeVersion([string]$Command) {
    # Windows PowerShell 5.1 wraps native stderr (including java -version) as errors.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = (& $Command -version 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar la version de $Command." }
        return $output
    } finally { $ErrorActionPreference = $previousPreference }
}

function Set-DevelopmentRoots {
    if ([string]::IsNullOrWhiteSpace($env:DOCUPODCAST_APP_ROOT)) {
        $env:DOCUPODCAST_APP_ROOT = $ProjectRoot
    }
    if ([string]::IsNullOrWhiteSpace($env:DOCUPODCAST_RUNTIME_ROOT)) {
        $env:DOCUPODCAST_RUNTIME_ROOT = $ProjectRoot
    }
    Write-Host "Installation root solicitado: $env:DOCUPODCAST_APP_ROOT"
    Write-Host "Runtime root solicitado: $env:DOCUPODCAST_RUNTIME_ROOT"
    Write-Host 'Las propiedades JVM explicitas tienen precedencia sobre estas variables.'
}

function Invoke-Diagnose {
    Write-Host '[DocuPodcast Studio] Diagnostico de entorno y layout'
    Write-Host "Raiz del proyecto: $ProjectRoot"
    Set-DevelopmentRoots
    $failures = [System.Collections.Generic.List[string]]::new()
    function Check([string]$Name, [scriptblock]$Action) {
        try { & $Action; Write-Host "OK: $Name" }
        catch { $failures.Add($Name); Write-Host "FALLO: $Name - $($_.Exception.Message)" }
    }
    Check 'Java 21' {
        $java = Get-Command java -ErrorAction Stop
        $text = Get-NativeVersion $java.Source
        if ($LASTEXITCODE -ne 0 -or $text -notmatch 'version "21[.\"]') { throw 'Java en PATH no es JDK 21. Instala Temurin 21 y revisa JAVA_HOME/PATH.' }
    }
    Check 'Maven 3.9+' {
        $maven = Get-Command mvn -ErrorAction Stop
        $text = Get-NativeVersion $maven.Source
        if ($LASTEXITCODE -ne 0 -or $text -notmatch 'Apache Maven (\d+\.\d+\.\d+)' -or [version]$Matches[1] -lt [version]'3.9.0') { throw 'Maven 3.9+ no esta disponible en PATH.' }
    }
    Check 'Toolchain Temurin 21' {
        $toolchains = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2\toolchains.xml'
        if (-not (Test-Path -LiteralPath $toolchains)) { throw 'Falta .m2/toolchains.xml. Integra la plantilla .mvn/toolchains.xml.example sin sobrescribir otras entradas.' }
        [xml]$xml = Get-Content -LiteralPath $toolchains -Raw
        $entry = @($xml.toolchains.toolchain | Where-Object { $_.type -eq 'jdk' -and $_.provides.version -eq '21' -and $_.provides.vendor -eq 'temurin' })
        if ($entry.Count -eq 0) { throw 'Falta una entrada jdk/version=21/vendor=temurin.' }
        $valid = $false
        foreach ($jdk in $entry) {
            $jdkPath = [Environment]::ExpandEnvironmentVariables([string]$jdk.configuration.jdkHome)
            $release = Join-Path $jdkPath 'release'
            if ((Test-Path -LiteralPath (Join-Path $jdkPath 'bin\javac.exe')) -and (Test-Path -LiteralPath $release)) {
                $jdkRelease = Get-Content -LiteralPath $release -Raw
                if ($jdkRelease -match 'JAVA_VERSION="21[.\"]' -and $jdkRelease -match 'IMPLEMENTOR="Eclipse Adoptium"') { $valid = $true }
            }
        }
        if (-not $valid) { throw 'jdkHome no apunta a una instalacion Temurin 21 valida.' }
    }
    Check 'Reactor Maven' {
        [xml]$reactor = Get-Content -LiteralPath (Join-Path $ProjectRoot 'pom.xml') -Raw
        foreach ($module in $reactor.project.modules.module) {
            $modulePom = Join-Path $ProjectRoot "$module\pom.xml"
            if (-not (Test-Path -LiteralPath $modulePom -PathType Leaf)) { throw "Falta $module/pom.xml" }
            [xml]$moduleXml = Get-Content -LiteralPath $modulePom -Raw
        }
    }
    if ($TaskArguments -contains '--resolve-native') {
        Check 'Resolver auxiliar nativo' { Invoke-CommandChecked 'mvn' @('-f', $LauncherPom, '-DskipTests', 'process-resources') }
    } else { Write-Host 'NO EJECUTADA: resolucion Maven del auxiliar nativo (requiere --resolve-native).' }
    $stylusNative = Join-Path $ProjectRoot "studio-launcher\target\native\stylus-0.3.0-windows-x86_64.jar"
    if (Test-Path -LiteralPath $stylusNative) {
        Check 'Auxiliar nativo local' {
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $zip = [IO.Compression.ZipFile]::OpenRead($stylusNative)
            try { if ($zip.Entries.FullName -notcontains 'stylus.dll') { throw 'El JAR no contiene stylus.dll.' } }
            finally { $zip.Dispose() }
        }
    } else {
        Write-Host 'NO INSTALADO: auxiliar nativo regenerable; usa --resolve-native o compila antes de ejecutar.'
        if ($TaskArguments -contains '--resolve-native') { $failures.Add('Auxiliar nativo ausente tras resolucion solicitada') }
    }
    $required = @(
        'studio-launcher\pom.xml',
        'scripts\tts\piper-file-to-wav.ps1',
        'scripts\tts\xtts-file-to-wav.ps1',
        'scripts\tts\xtts-batch-to-wav.ps1'
    )
    foreach ($relative in $required) {
        $path = Join-Path $ProjectRoot $relative
        Check $relative { if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Falta recurso obligatorio: $relative" } }
    }
    if ($failures.Count) { throw "Diagnostico: $($failures.Count) comprobaciones fallidas: $($failures -join ', ')." }
}

function Invoke-RunApp {
    Set-DevelopmentRoots
    $smoke = $TaskArguments -contains '--smoke'
    Write-Host '[DocuPodcast Studio] Compilando launcher y dependencias...'
    Invoke-CommandChecked 'mvn' @('-pl', 'studio-launcher', '-am', '-DskipTests', 'install')
    Write-Host '[DocuPodcast Studio] Iniciando aplicacion productiva...'
    $run = @('-f', $LauncherPom)
    if ($smoke) { $run += '-Ddocupodcast.launch.smoke=true' }
    $run += 'javafx:run'
    Invoke-CommandChecked 'mvn' $run
}

function Invoke-Tests {
    Invoke-CommandChecked 'mvn' @('test')
}

function Invoke-Verify {
    $reference = Get-TaskOption 'reference-root'
    if ($reference -and -not (Test-Path -LiteralPath $reference -PathType Container)) { throw "No existe la referencia solicitada: $reference" }
    Invoke-CommandChecked 'mvn' @('verify', '-Pgui-e2e')
    Invoke-OptionalParity
    $script:TaskArguments = @('--smoke')
    Invoke-RunApp
}

function Invoke-RealSmoke {
    Set-DevelopmentRoots
    $runtimeRoot = [IO.Path]::GetFullPath($env:DOCUPODCAST_RUNTIME_ROOT)
    $installationRoot = [IO.Path]::GetFullPath($env:DOCUPODCAST_APP_ROOT)
    $tier = 'short'
    $engineFilter = ''
    $preset = ''
    $requiredOverride = ''
    $resume = $false
    $timeoutHours = 1
    $promptText = ''
    $promptSource = ''
    $imageLabel = 'image-smoke'
    $imageSeed = '424242'
    $imageWidth = '512'
    $imageHeight = '512'
    $comfyMemoryProfile = if ([string]::IsNullOrWhiteSpace($env:DOCUPODCAST_COMFY_MEMORY_PROFILE)) {
        'SAFE_LOW_VRAM'
    } else {
        $env:DOCUPODCAST_COMFY_MEMORY_PROFILE
    }
    foreach ($argument in $TaskArguments) {
        if ($argument -eq '--resume') { $resume = $true }
        elseif ($argument.StartsWith('--tier=')) { $tier = $argument.Substring('--tier='.Length).ToLowerInvariant() }
        elseif ($argument.StartsWith('--engine=')) { $engineFilter = $argument.Substring('--engine='.Length).ToLowerInvariant() }
        elseif ($argument.StartsWith('--preset=')) { $preset = $argument.Substring('--preset='.Length) }
        elseif ($argument.StartsWith('--required=')) { $requiredOverride = $argument.Substring('--required='.Length) }
        elseif ($argument.StartsWith('--prompt=')) { $promptText = $argument.Substring('--prompt='.Length) }
        elseif ($argument.StartsWith('--prompt-file=')) { $promptSource = $argument.Substring('--prompt-file='.Length) }
        elseif ($argument.StartsWith('--label=')) { $imageLabel = $argument.Substring('--label='.Length) }
        elseif ($argument.StartsWith('--seed=')) { $imageSeed = $argument.Substring('--seed='.Length) }
        elseif ($argument.StartsWith('--width=')) { $imageWidth = $argument.Substring('--width='.Length) }
        elseif ($argument.StartsWith('--height=')) { $imageHeight = $argument.Substring('--height='.Length) }
        elseif ($argument.StartsWith('--timeout-hours=')) {
            $timeoutHours = [Math]::Max(1, [Math]::Min(6, [int]$argument.Substring('--timeout-hours='.Length)))
        }
    }
    if ($tier -notin @('short', 'soak')) { throw "Tier no valido: $tier. Usa short o soak." }
    if ($tier -eq 'soak' -and ([string]::IsNullOrWhiteSpace($engineFilter) -or [string]::IsNullOrWhiteSpace($preset))) {
        throw 'La certificacion soak exige --engine y --preset para evitar iniciar accidentalmente varias horas de trabajo.'
    }
    if (-not [string]::IsNullOrWhiteSpace($promptSource)) {
        $resolvedPromptSource = [System.IO.Path]::GetFullPath($promptSource)
        if (-not (Test-Path -LiteralPath $resolvedPromptSource -PathType Leaf)) {
            throw "No existe el archivo de prompt: $resolvedPromptSource"
        }
        $promptText = [System.IO.File]::ReadAllText($resolvedPromptSource)
    }

    $certificationRoot = Join-Path $ProjectRoot 'target\certification'
    New-Item -ItemType Directory -Path $certificationRoot -Force | Out-Null
    $latestFile = Join-Path $certificationRoot 'latest.txt'
    if ($resume -and (Test-Path -LiteralPath $latestFile)) {
        $sessionRoot = (Get-Content -LiteralPath $latestFile -Raw).Trim()
        if (-not (Test-Path -LiteralPath $sessionRoot -PathType Container)) { $sessionRoot = $null }
    }
    if ([string]::IsNullOrWhiteSpace($sessionRoot)) {
        $sessionRoot = Join-Path $certificationRoot (Get-Date -Format 'yyyyMMdd-HHmmss')
        New-Item -ItemType Directory -Path $sessionRoot -Force | Out-Null
        [System.IO.File]::WriteAllText($latestFile, $sessionRoot, [System.Text.UTF8Encoding]::new($false))
    }

    $detected = [System.Collections.Generic.List[string]]::new()
    if ((Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\piper\piper.exe')) -and
        (Test-Path -LiteralPath (Join-Path $runtimeRoot 'models\tts\piper\voices\es_ES-default-medium.onnx'))) {
        $detected.Add('piper')
    }
    if ((Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\xtts-wrapper\.venv\Scripts\python.exe')) -and
        (Test-Path -LiteralPath (Join-Path $runtimeRoot 'models\tts\xtts'))) {
        $detected.Add('xtts')
    }
    if (Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\ffmpeg\bin\ffmpeg.exe')) {
        $detected.Add('ffmpeg')
    }
    if ((Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\image\ComfyUI\main.py')) -and
        (Test-Path -LiteralPath (Join-Path $runtimeRoot 'models\image\workflows\workflow-sd15-reference.json'))) {
        $detected.Add('image')
    }
    if ((Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\image\ComfyUI\main.py')) -and
        (Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\image\ComfyUI\models\frame_interpolation\rife_v4.25_lite.safetensors'))) {
        $detected.Add('rife')
    }
    if (Test-Path -LiteralPath (Join-Path $runtimeRoot 'models\video\workflows\workflow-wan22-ti2v-5b-api.json')) {
        $detected.Add('video')
    }
    if ((Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\tesseract\bin\tesseract.exe')) -and
        (Test-Path -LiteralPath (Join-Path $runtimeRoot 'tools\tesseract\tessdata\eng.traineddata'))) {
        $detected.Add('ocr')
    }
    $requested = if (-not [string]::IsNullOrWhiteSpace($requiredOverride)) {
        @($requiredOverride.Split(',') | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ })
    } elseif (-not [string]::IsNullOrWhiteSpace($engineFilter)) {
        @($engineFilter)
    } else { @($detected) }
    if ($requested.Count -eq 0) { throw 'No se detectaron runtimes reales para certificar.' }

    $session = [ordered]@{
        schemaVersion = 1; startedAt = (Get-Date).ToString('o'); tier = $tier
        requested = $requested; preset = $preset; timeoutHours = $timeoutHours
        referenceRoot = $null
        policy = 'Only audit-owned managed processes may be stopped.'
    }
    $session | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $sessionRoot 'session.json') -Encoding utf8

    $declared = @(
        @{ capability='voice'; engine='piper'; preset='default'; resource='installed' },
        @{ capability='voice'; engine='xtts'; preset='unit+batch'; resource='installed' },
        @{ capability='image'; engine='comfyui'; preset='draft'; resource='installed' },
        @{ capability='image'; engine='comfyui'; preset='sd15-dreamshaper'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\image\DreamShaper_8_pruned.safetensors')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='image'; engine='comfyui'; preset='sdxl-reference'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\image\sd_xl_base_1.0.safetensors')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='image'; engine='comfyui'; preset='flux-kontext'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\image\flux1-dev.safetensors')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='interpolation'; engine='rife'; preset='rife-v4.25-lite'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'tools\image\ComfyUI\models\frame_interpolation\rife_v4.25_lite.safetensors')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='video-generation'; engine='comfyui-video'; preset='wan22-ti2v-5b-balanced'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\video\workflows\workflow-wan22-ti2v-5b-api.json')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='video-generation'; engine='comfyui-video'; preset='wan22-i2v-14b-quality'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\video\workflows\workflow-wan22-i2v-14b-api.json')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='video-generation'; engine='comfyui-video'; preset='ltx23-i2v-portrait'; resource=$(if (Test-Path (Join-Path $runtimeRoot 'models\video\workflows\workflow-ltx23-i2v-portrait-api.json')) {'installed'} else {'RESOURCE_MISSING'}) },
        @{ capability='video-render'; engine='ffmpeg'; preset='short-timeline'; resource='installed' },
        @{ capability='ocr'; engine='tesseract'; preset='spa+eng'; resource='installed' }
    )
    $declared | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $sessionRoot 'capability-matrix.json') -Encoding utf8

    $failures = [System.Collections.Generic.List[string]]::new()
    foreach ($engine in $requested) {
        $normalized = switch ($engine) {
            'comfyui' { 'image' }
            'comfyui-video' { 'video' }
            'tesseract' { 'ocr' }
            default { $engine }
        }
        $selectedPreset = $preset
        if ([string]::IsNullOrWhiteSpace($selectedPreset)) {
            $selectedPreset = switch ($normalized) {
                'image' { 'draft' }; 'rife' { 'rife-v4.25-lite' }; 'video' { 'wan22-ti2v-5b-balanced' }
                'xtts' { 'unit+batch' }; 'ffmpeg' { 'short-timeline' }; default { 'default' }
            }
        }
        $engineDirName = switch ($normalized) { 'image' {'comfyui'}; 'video' {'comfyui-video'}; default {$normalized} }
        $evidenceDir = Join-Path (Join-Path $sessionRoot $engineDirName) $selectedPreset
        $manifest = Join-Path $evidenceDir 'manifest.json'
        $signatureResources = switch ($normalized) {
            'piper' { @('tools\piper\piper.exe', 'models\tts\piper\voices\es_ES-default-medium.onnx', 'scripts\tts\piper-file-to-wav.ps1') }
            'xtts' { @('models\tts\xtts\config.json', 'models\tts\xtts\speakers\voz-por-defecto.wav',
                       'tools\xtts-wrapper\synthesize_xtts.py', 'tools\xtts-wrapper\synthesize_xtts_batch.py') }
            'ffmpeg' { @('tools\ffmpeg\bin\ffmpeg.exe') }
            'rife' { @('tools\image\ComfyUI\models\frame_interpolation\rife_v4.25_lite.safetensors') }
            'ocr' { @('tools\tesseract\bin\tesseract.exe', 'tools\tesseract\tessdata\spa.traineddata',
                      'tools\tesseract\tessdata\eng.traineddata') }
            'image' {
                if ($selectedPreset -like 'flux*') {
                    @('models\image\flux1-dev.safetensors', 'models\image\text_encoders\clip_l.safetensors',
                      'models\image\text_encoders\t5xxl_bf16.safetensors', 'models\image\vae\ae.safetensors')
                } else { @('models\image\workflows\workflow-sd15-reference.json', 'models\image\v1-5-pruned-emaonly-fp16.safetensors') }
            }
            'video' { @('models\video\workflows\workflow-wan22-ti2v-5b-api.json',
                        'models\video\workflows\workflow-wan22-i2v-14b-api.json',
                        'models\video\workflows\workflow-ltx23-i2v-portrait-api.json') }
            default { @() }
        }
        $resourceFingerprints = foreach ($relative in $signatureResources) {
            $resource = Join-Path $runtimeRoot $relative
            if (Test-Path -LiteralPath $resource -PathType Leaf) {
                $item = Get-Item -LiteralPath $resource
                "$relative|$($item.Length)|$($item.LastWriteTimeUtc.Ticks)"
            } else { "$relative|MISSING" }
        }
        $signatureSource = "$tier|$normalized|$selectedPreset|$timeoutHours|$env:DOCUPODCAST_COMFY_MEMORY_PROFILE|$imageLabel|$imageSeed|$imageWidth|$imageHeight|$promptText|$($resourceFingerprints -join ';')"
        $signatureBytes = [System.Text.Encoding]::UTF8.GetBytes($signatureSource)
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        try {
            $signature = -join ($sha256.ComputeHash($signatureBytes) | ForEach-Object { $_.ToString('x2') })
        } finally {
            $sha256.Dispose()
        }
        $signatureFile = Join-Path $evidenceDir 'signature.txt'
        if ($resume -and (Test-Path $manifest) -and (Test-Path $signatureFile) -and
            ((Get-Content $signatureFile -Raw).Trim() -eq $signature) -and
            ((Get-Content $manifest -Raw) -match '"state"\s*:\s*"PASS"')) {
            Write-Host "RESUME PASS: $normalized/$selectedPreset"
            continue
        }
        New-Item -ItemType Directory -Path $evidenceDir -Force | Out-Null
        [System.IO.File]::WriteAllText($signatureFile, $signature, [System.Text.UTF8Encoding]::new($false))
        $certificationPromptFile = ''
        if ($normalized -eq 'image' -and -not [string]::IsNullOrWhiteSpace($promptText)) {
            $certificationPromptFile = Join-Path $evidenceDir 'prompt.txt'
            [System.IO.File]::WriteAllText($certificationPromptFile, $promptText.Trim(), [System.Text.UTF8Encoding]::new($false))
        }

        if ($normalized -eq 'video') {
            $workflow = switch -Wildcard ($selectedPreset) {
                'wan22-ti2v*' { 'models\video\workflows\workflow-wan22-ti2v-5b-api.json' }
                'wan22-i2v*' { 'models\video\workflows\workflow-wan22-i2v-14b-api.json' }
                'ltx23*' { 'models\video\workflows\workflow-ltx23-i2v-portrait-api.json' }
                default { '' }
            }
            if ([string]::IsNullOrWhiteSpace($workflow) -or -not (Test-Path -LiteralPath (Join-Path $runtimeRoot $workflow))) {
                @{ state='RESOURCE_MISSING'; engineId='comfyui-video'; presetId=$selectedPreset; durationMs=0;
                   message='El workflow WAN/LTX no esta instalado; no se afirma generacion real.'; artifacts=@() } |
                    ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifest -Encoding utf8
                Write-Host "RESOURCE_MISSING: video/$selectedPreset"
                if ($requiredOverride.Split(',') -contains $engine -or -not [string]::IsNullOrWhiteSpace($engineFilter)) {
                    $failures.Add("video/${selectedPreset}: RESOURCE_MISSING")
                }
                continue
            }
        }

        if ($normalized -eq 'ocr') {
            $log = Join-Path $evidenceDir 'maven.log'
            & mvn '-pl' 'studio-desktop' '-am' '-Dtest=RealTesseractDocumentSmokeTest' `
                '-Dsurefire.failIfNoSpecifiedTests=false' '-Ddocupodcast.realTesseractSmoke.enabled=true' `
                "-Ddocupodcast.app.root=$installationRoot" 'test' 2>&1 | Tee-Object -FilePath $log
            $ok = $LASTEXITCODE -eq 0
            @{ state=$(if ($ok) {'PASS'} else {'FAIL'}); engineId='tesseract'; presetId=$selectedPreset;
               message=$(if ($ok) {'PDF nativo y escaneado certificados.'} else {'Fallo el smoke OCR; revisa maven.log.'}); artifacts=@() } |
                ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifest -Encoding utf8
        } elseif ($normalized -eq 'ink') {
            & mvn '-pl' 'studio-ink' '-am' '-Dtest=InkEditorSessionTest,InkRealtimeStrokeEnginePressureTest,InkInputProviderFactoryTest' `
                '-Dsurefire.failIfNoSpecifiedTests=false' 'test' 2>&1 | Tee-Object -FilePath (Join-Path $evidenceDir 'maven.log')
            $ok = $LASTEXITCODE -eq 0
            @{ state=$(if ($ok) {'PASS'} else {'FAIL'}); engineId='studio-ink'; presetId='regression';
               message='Regresion corta de tinta; no es un motor seleccionable.'; artifacts=@() } |
                ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifest -Encoding utf8
        } else {
            $requiredName = $normalized
            & mvn '-pl' 'studio-local-media-adapters' '-am' '-Dtest=LocalMediaRealCapabilitiesSmokeTest' `
                '-Dsurefire.failIfNoSpecifiedTests=false' '-Ddocupodcast.realCapabilitiesSmoke.enabled=true' `
                "-Ddocupodcast.realCapabilitiesSmoke.required=$requiredName" `
                "-Ddocupodcast.realCapabilitiesSmoke.preset=$selectedPreset" `
                "-Ddocupodcast.realCapabilitiesSmoke.timeoutHours=$timeoutHours" `
                "-Ddocupodcast.comfy.memoryProfile=$comfyMemoryProfile" `
                "-Ddocupodcast.certification.root=$sessionRoot" `
                "-Ddocupodcast.realCapabilitiesSmoke.promptFile=$certificationPromptFile" `
                "-Ddocupodcast.realCapabilitiesSmoke.label=$imageLabel" `
                "-Ddocupodcast.realCapabilitiesSmoke.seed=$imageSeed" `
                "-Ddocupodcast.realCapabilitiesSmoke.width=$imageWidth" `
                "-Ddocupodcast.realCapabilitiesSmoke.height=$imageHeight" `
                "-Ddocupodcast.app.root=$installationRoot" "-Ddocupodcast.runtime.root=$runtimeRoot" 'test' `
                2>&1 | Tee-Object -FilePath (Join-Path $evidenceDir 'maven.log')
            $ok = $LASTEXITCODE -eq 0
        }
        if (-not $ok) { $failures.Add("$normalized/$selectedPreset") }
    }

    Write-Host 'NO EJECUTADA: paridad historica. Usa 03-verificar-completo.bat --reference-root=<ruta> si la necesitas.'
    $manifests = @(Get-ChildItem -LiteralPath $sessionRoot -Filter manifest.json -Recurse -File | ForEach-Object {
        Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    })
    $report = [System.Collections.Generic.List[string]]::new()
    $report.Add('# Certificacion integral de motores')
    $report.Add('')
    $report.Add("- Fecha: ``$((Get-Date).ToString('o'))``")
    $report.Add("- Tier: ``$tier``")
    $report.Add("- Evidencia: ``$sessionRoot``")
    $report.Add('')
    $report.Add('| Estado | Motor | Preset | Duracion ms | Artefactos |')
    $report.Add('|---|---|---|---:|---|')
    foreach ($item in $manifests) {
        $report.Add("| $($item.state) | $($item.engineId) | $($item.presetId) | $($item.durationMs) | $(@($item.artifacts).Count) |")
    }
    [System.IO.File]::WriteAllLines((Join-Path $sessionRoot 'report.md'), $report,
        [System.Text.UTF8Encoding]::new($false))
    Write-Host "Reporte de certificacion: $(Join-Path $sessionRoot 'report.md')"
    if ($failures.Count -gt 0) { throw "Fallaron certificaciones: $($failures -join ', ')" }
}

function Invoke-PackageAppImage {
    Set-DevelopmentRoots
    Invoke-CommandChecked 'mvn' @('-pl', 'studio-launcher', '-am', '-DskipTests', 'install')
    $stage = Join-Path $ProjectRoot 'target\jpackage-launcher'
    $modules = Join-Path $stage 'modules'
    $destination = Join-Path $ProjectRoot 'dist\app-image'
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
    New-Item -ItemType Directory -Path $modules -Force | Out-Null
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Invoke-CommandChecked 'mvn' @(
        '-f', $LauncherPom, 'dependency:copy-dependencies',
        '-DincludeScope=runtime', "-DoutputDirectory=$modules"
    )
    Copy-Item -LiteralPath (Join-Path $ProjectRoot "studio-launcher\target\studio-launcher-$Version.jar") -Destination $modules
    $image = Join-Path $destination 'DocuPodcastStudio'
    if (Test-Path -LiteralPath $image) { Remove-Item -LiteralPath $image -Recurse -Force }
    Invoke-CommandChecked 'jpackage' @(
        '--type', 'app-image', '--name', 'DocuPodcastStudio', '--app-version', '0.0.1',
        '--vendor', 'DocuPodcast Studio', '--description', 'DocuPodcast Studio',
        '--dest', $destination, '--input', $modules,
        '--main-jar', "studio-launcher-$Version.jar",
        '--main-class', 'com.marcosmoreiradev.docupodcaststudio.launcher.StudioMain',
        '--java-options', '-Xmx2048m',
        '--java-options', '-Dfile.encoding=UTF-8',
        '--add-launcher', "DocuPodcastStudioSmoke=$(Join-Path $ProjectRoot 'scripts\lib\app-image-smoke.properties')",
        '--icon', (Join-Path $ProjectRoot 'packaging\windows\docupodcast-icon.ico')
    )
    $helpers = Join-Path $image 'app\scripts\tts'
    New-Item -ItemType Directory -Path $helpers -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $ProjectRoot 'scripts\tts\piper-file-to-wav.ps1') -Destination $helpers
    Copy-Item -LiteralPath (Join-Path $ProjectRoot 'scripts\tts\xtts-file-to-wav.ps1') -Destination $helpers
    Copy-Item -LiteralPath (Join-Path $ProjectRoot 'scripts\tts\xtts-batch-to-wav.ps1') -Destination $helpers
    $officialVoiceSamples = Join-Path $ProjectRoot 'samples\voices'
    if (-not (Test-Path -LiteralPath $officialVoiceSamples -PathType Container)) {
        throw "Faltan las muestras oficiales de voz para empaquetar: $officialVoiceSamples"
    }
    $packagedSamples = Join-Path $image 'app\samples\voices'
    New-Item -ItemType Directory -Path $packagedSamples -Force | Out-Null
    Copy-Item -Path (Join-Path $officialVoiceSamples '*') -Destination $packagedSamples -Recurse -Force
    $previousAppRoot = $env:DOCUPODCAST_APP_ROOT
    $previousRuntimeRoot = $env:DOCUPODCAST_RUNTIME_ROOT
    try {
        $env:DOCUPODCAST_APP_ROOT = Join-Path $image 'app'
        $env:DOCUPODCAST_RUNTIME_ROOT = Join-Path $ProjectRoot 'target\app-image-smoke-runtime'
        $smoke = Start-Process -FilePath (Join-Path $image 'DocuPodcastStudioSmoke.exe') `
            -WorkingDirectory $image -Wait -PassThru
        if ($smoke.ExitCode -ne 0) { throw "El smoke de la app-image termino con codigo $($smoke.ExitCode)." }
    } finally {
        $env:DOCUPODCAST_APP_ROOT = $previousAppRoot
        $env:DOCUPODCAST_RUNTIME_ROOT = $previousRuntimeRoot
    }
    Write-Host "App-image ligera generada en $image"
    Write-Host 'Los modelos y herramientas se administran despues desde Configuracion.'
}

function Invoke-Diagnostics {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $output = Join-Path $ProjectRoot "target\diagnostics\$stamp"
    New-Item -ItemType Directory -Path $output -Force | Out-Null
    $results = [System.Collections.Generic.List[object]]::new()
    foreach ($check in @('Environment', 'Parity')) {
        $capture = & {
            try {
                if ($check -eq 'Environment') { Invoke-Diagnose } else { Invoke-OptionalParity }
                $state = if ($check -eq 'Parity' -and -not (Get-TaskOption 'reference-root')) { 'NOT_RUN' } else { 'PASS' }
                $results.Add([pscustomobject]@{check=$check;state=$state})
            } catch {
                Write-Output "FALLO: $($_.Exception.Message)"
                $results.Add([pscustomobject]@{check=$check;state='FAIL'})
            }
        } *>&1 | Out-String
        # Do not collect environment/configuration dumps. Redact accidental credential-shaped output.
        $capture = $capture -replace '(?i)(token|password|secret|api[_-]?key|authorization)(\s*[=:]\s*)\S+', '$1$2[REDACTED]'
        $capture = $capture -replace '(?i)https?://[^\s/@]+:[^\s/@]+@', 'https://[REDACTED]@'
        $capture = $capture.Replace([Environment]::GetFolderPath('UserProfile'), '%USERPROFILE%')
        $capture | Set-Content -LiteralPath (Join-Path $output "$check.log") -Encoding utf8
    }
    $results | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'summary.json') -Encoding utf8
    Compress-Archive -LiteralPath $output -DestinationPath "$output.zip" -Force
    Write-Host "Diagnostico: $output.zip"
    if (@($results | Where-Object state -eq 'FAIL').Count) { throw 'Diagnostico exportado con comprobaciones fallidas; consulta summary.json.' }
}

Push-Location $ProjectRoot
try {
    if ($TaskArguments -contains '--help') {
        if (@($TaskArguments).Count -ne 1) { throw '--help debe usarse sin otras opciones.' }
        Show-TaskHelp
        exit 0
    }
    Assert-TaskArguments
    switch ($Task) {
        'Diagnose' { Invoke-Diagnose }
        'RunApp' { Invoke-RunApp }
        'Test' { Invoke-Tests }
        'Verify' { Invoke-Verify }
        'RealSmoke' { Invoke-RealSmoke }
        'PackageAppImage' { Invoke-PackageAppImage }
        'Diagnostics' { Invoke-Diagnostics }
    }
    exit 0
} catch {
    Write-Error $_
    exit 1
} finally {
    Pop-Location
}
