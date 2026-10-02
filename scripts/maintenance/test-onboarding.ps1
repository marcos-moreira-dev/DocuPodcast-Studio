[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$fixture = Join-Path $root ('target\onboarding-tests\checkout with spaces-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $fixture 'scripts\lib') -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'pom.xml') -Destination $fixture
Copy-Item -Path (Join-Path $root 'scripts\0*.bat') -Destination (Join-Path $fixture 'scripts')
foreach ($file in @('DocuPodcast.Tasks.ps1', 'Compare-ReferenceCapabilities.ps1')) {
    Copy-Item -LiteralPath (Join-Path $root "scripts\lib\$file") -Destination (Join-Path $fixture 'scripts\lib')
}
[xml]$pom = Get-Content -LiteralPath (Join-Path $root 'pom.xml') -Raw
foreach ($module in $pom.project.modules.module) {
    New-Item -ItemType Directory -Path (Join-Path $fixture $module) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $root "$module\pom.xml") -Destination (Join-Path $fixture $module)
}
New-Item -ItemType Directory -Path (Join-Path $fixture 'scripts\tts') -Force | Out-Null
Copy-Item -Path (Join-Path $root 'scripts\tts\*.ps1') -Destination (Join-Path $fixture 'scripts\tts')
$powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$results = [Collections.Generic.List[object]]::new()
function Run-Case([string]$Name, [string]$Task, [string[]]$Options, [int]$Expected) {
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $powershell -NoProfile -File (Join-Path $fixture 'scripts\lib\DocuPodcast.Tasks.ps1') -Task $Task @Options 2>&1 | Out-String
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    $output | Set-Content -LiteralPath (Join-Path $fixture "$Name.log") -Encoding UTF8
    $results.Add([pscustomobject]@{Name=$Name;ExitCode=$code;Passed=($code -eq $Expected)})
    if ($code -ne $Expected) { throw "$Name devolvio $code; esperado $Expected. $output" }
}
foreach ($task in @('Diagnose','RunApp','Test','Verify','RealSmoke','PackageAppImage','Diagnostics')) {
    Run-Case "help-$task" $task @('--help') 0
    Run-Case "invalid-$task" $task @('--unknown') 1
}
if (Test-Path -LiteralPath (Join-Path $fixture 'target')) { throw 'Ayuda o argumento invalido genero salidas.' }
Run-Case 'missing-reference' 'Verify' @("--reference-root=$(Join-Path $fixture 'missing')") 1
Run-Case 'diagnose-without-models' 'Diagnose' @() 0
if (Test-Path -LiteralPath (Join-Path $fixture 'target')) { throw 'Diagnostico ordinario genero salidas.' }
$savedPath = $env:PATH
try {
    $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
    Run-Case 'missing-java-maven' 'Diagnose' @() 1
    Run-Case 'diagnostics-on-failure' 'Diagnostics' @() 1
} finally { $env:PATH = $savedPath }
$archive = @(Get-ChildItem -LiteralPath (Join-Path $fixture 'target\diagnostics') -Filter '*.zip')
if ($archive.Count -ne 1) { throw 'Falta ZIP despues del fallo.' }
$summaryFile = Get-ChildItem -LiteralPath (Join-Path $fixture 'target\diagnostics') -Filter summary.json -Recurse | Select-Object -First 1
$summary = Get-Content -LiteralPath $summaryFile.FullName -Raw | ConvertFrom-Json
if (@($summary | Where-Object { $_.check -eq 'Parity' -and $_.state -eq 'NOT_RUN' }).Count -ne 1) { throw 'Paridad no solicitada no se registro como NOT_RUN.' }
Run-Case 'diagnostics-parity-failure' 'Diagnostics' @("--reference-root=$(Join-Path $fixture 'missing')") 1
$results | Export-Csv -LiteralPath (Join-Path $fixture 'results.csv') -NoTypeInformation -Encoding UTF8
Write-Host "$($results.Count) casos correctos. Fixture aislado sin modelos ni copia hermana: $fixture"
