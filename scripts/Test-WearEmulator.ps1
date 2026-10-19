#Requires -Version 7.4
<#
.SYNOPSIS
Executa a instrumentação somente no AVD reservado e coleta capturas para revisão.
.DESCRIPTION
Exige APKs debug e androidTest já compilados. Não seleciona aparelhos automaticamente.
Altera temporariamente fonte e tempo de tela somente no emulador confirmado; restaura
os valores ao terminar. Não usar para medir autonomia ou validar integração Samsung.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string]$AdbPath = 'adb',
    [string]$ExpectedAvd = 'WatchReserve_Wear5_Small_Round'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$adbExecutable = (Get-Command -Name $AdbPath -CommandType Application -ErrorAction Stop).Source
$packageName = 'dev.brenopoubel.watch8battery'

function Invoke-EmulatorAdb([string[]]$Arguments) {
    $lines = @(& $adbExecutable -s $Serial @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "ADB falhou: $($Arguments[0])" }
    return ($lines | Out-String).Trim()
}

if ((Invoke-EmulatorAdb @('get-state')) -ne 'device' -or
    (Invoke-EmulatorAdb @('shell', 'getprop', 'ro.kernel.qemu')) -ne '1') {
    throw 'O alvo precisa ser um emulador conectado e autorizado.'
}
$avdName = ((Invoke-EmulatorAdb @('emu', 'avd', 'name')) -split '\r?\n')[0].Trim()
if ($avdName -ne $ExpectedAvd -or $avdName -notmatch '^WatchReserve_') {
    throw 'O alvo não corresponde ao AVD de testes reservado para o projeto.'
}
$configuration = Invoke-EmulatorAdb @('shell', 'am', 'get-config')
if ($configuration -notmatch '-round-' -or $configuration -notmatch '-watch-') {
    throw 'Este roteiro exige uma tela circular Wear OS.'
}

$debugApk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
$testApk = Join-Path $projectRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
foreach ($apk in @($debugApk, $testApk)) {
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "Compile o APK antes do teste: $apk" }
}
Invoke-EmulatorAdb @('install', '-r', $debugApk) | Out-Null
Invoke-EmulatorAdb @('install', '-r', $testApk) | Out-Null
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$fontBefore = Invoke-EmulatorAdb @('shell', 'settings', 'get', 'system', 'font_scale')
$timeoutBefore = Invoke-EmulatorAdb @('shell', 'settings', 'get', 'system', 'screen_off_timeout')
$results = [Collections.Generic.List[object]]::new()

function Restore-Setting([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq 'null') {
        Invoke-EmulatorAdb @('shell', 'settings', 'delete', 'system', $Name) | Out-Null
    } else {
        Invoke-EmulatorAdb @('shell', 'settings', 'put', 'system', $Name, $Value) | Out-Null
    }
}

try {
    Invoke-EmulatorAdb @('shell', 'settings', 'put', 'system', 'screen_off_timeout', '600000') | Out-Null
    foreach ($scale in @('1.0', '1.3')) {
        Invoke-EmulatorAdb @('shell', 'input', 'keyevent', '224') | Out-Null
        Invoke-EmulatorAdb @('shell', 'settings', 'put', 'system', 'font_scale', $scale) | Out-Null
        $result = Invoke-EmulatorAdb @(
            'shell', 'am', 'instrument', '-w', '--no-window-animation',
            "$packageName.test/androidx.test.runner.AndroidJUnitRunner"
        )
        $suffix = $scale.Replace('.', '_')
        [IO.File]::WriteAllText((Join-Path $OutputDirectory "instrumentation-font-$suffix.txt"), $result, [Text.UTF8Encoding]::new($false))
        $testCount = [regex]::Match($result, 'OK \(([0-9]+) tests?\)')
        if (-not $testCount.Success -or $result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
            throw "A instrumentação falhou com fonte $scale. Consulte o relatório local."
        }
        foreach ($name in @('home', 'measurement', 'destination-error', 'rotary-scroll')) {
            $destination = Join-Path $OutputDirectory "$name-font-$suffix.png"
            # PowerShell 7.4+ preserva os bytes do stdout nativo nessa redireção direta.
            & $adbExecutable -s $Serial exec-out run-as $packageName cat "files/audit-screenshots/$name.png" > $destination
            if ($LASTEXITCODE -ne 0) { throw "Não foi possível obter a captura: $name" }
            $bytes = [IO.File]::ReadAllBytes($destination)
            if ($bytes.Length -lt 8 -or [Convert]::ToHexString($bytes[0..7]) -ne '89504E470D0A1A0A') {
                throw "Captura PNG inválida: $name"
            }
        }
        $results.Add([pscustomobject]@{fontScale=$scale; passedTests=[int]$testCount.Groups[1].Value})
        Write-Output "PASS: fonte $scale, $($testCount.Groups[1].Value) testes instrumentados e quatro capturas."
    }
} finally {
    $restoreErrors = [Collections.Generic.List[string]]::new()
    foreach ($setting in @(@('font_scale', $fontBefore), @('screen_off_timeout', $timeoutBefore))) {
        try { Restore-Setting $setting[0] $setting[1] }
        catch { $restoreErrors.Add($setting[0]) }
    }
    if ($restoreErrors.Count -gt 0) { throw "Falha ao restaurar configurações do emulador: $($restoreErrors -join ', ')." }
}

$summary = [pscustomobject]@{
    avd=$avdName
    configuration=$configuration
    debugApkSha256=(Get-FileHash -LiteralPath $debugApk -Algorithm SHA256).Hash.ToLowerInvariant()
    runs=$results.ToArray()
    samsungValidated=$false
    autonomyMeasured=$false
}
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'summary.json'), ($summary | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
Write-Output 'Fonte e tempo de tela restaurados. Revise as capturas antes de publicá-las.'
