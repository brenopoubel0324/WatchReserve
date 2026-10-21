#Requires -Version 7.4
<#
.SYNOPSIS
Prova controles e restauração exclusivamente no AVD de laboratório.
.DESCRIPTION
Instala somente os pacotes lab já compilados. A instrumentação concede e revoga
WRITE_SECURE_SETTINGS nesse APK e simula a bateria no AVD, com registro de recuperação.
Não executa no relógio pessoal, não valida Samsung e não mede autonomia.
#>
[CmdletBinding(DefaultParameterSetName = 'Probe')]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string]$AdbPath = 'adb',
    [Parameter(ParameterSetName = 'Recovery')][switch]$RecoverOnly,
    [Parameter(ParameterSetName = 'Interrupt')][switch]$InterruptAfterApply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$adbExecutable = (Get-Command -Name $AdbPath -CommandType Application -ErrorAction Stop).Source
$packageName = 'dev.brenopoubel.watchreserve.lab'
$expectedAvd = 'WatchReserve_Wear5_Small_Round'

function Invoke-LabAdb([string[]]$Arguments) {
    $lines = @(& $adbExecutable -s $Serial @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "ADB do laboratório falhou: $($Arguments[0])" }
    return ($lines | Out-String).Trim()
}

if ((Invoke-LabAdb @('get-state')) -ne 'device' -or
    (Invoke-LabAdb @('shell', 'getprop', 'ro.kernel.qemu')) -ne '1') {
    throw 'O alvo deve ser um emulador autorizado.'
}
$avdName = ((Invoke-LabAdb @('emu', 'avd', 'name')) -split '\r?\n')[0].Trim()
if ($avdName -ne $expectedAvd) { throw 'O alvo não é o AVD reservado do WatchReserve.' }
$configuration = Invoke-LabAdb @('shell', 'am', 'get-config')
if ($configuration -notmatch '-round-' -or $configuration -notmatch '-watch-') {
    throw 'A prova exige um AVD Wear OS circular.'
}

$debugApk = Join-Path $projectRoot 'lab\build\outputs\apk\debug\lab-debug.apk'
$testApk = Join-Path $projectRoot 'lab\build\outputs\apk\androidTest\debug\lab-debug-androidTest.apk'
foreach ($apk in @($debugApk, $testApk)) {
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "Compile o laboratório: $apk" }
}
Invoke-LabAdb @('install', '-r', $debugApk) | Out-Null
Invoke-LabAdb @('install', '-r', $testApk) | Out-Null
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$runId = [guid]::NewGuid().ToString()
$arguments = @('shell', 'am', 'instrument', '-w', '-e', 'reservedAvd', $expectedAvd, '-e', 'runId', $runId)
if ($RecoverOnly) { $arguments += @('-e', 'recoverOnly', 'true') }
if ($InterruptAfterApply) { $arguments += @('-e', 'interruptAfterApply', 'true') }
$arguments += "$packageName.test/androidx.test.runner.AndroidJUnitRunner"
$result = Invoke-LabAdb $arguments
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'instrumentation.txt'), $result, [Text.UTF8Encoding]::new($false))
$rawReport = Invoke-LabAdb @('exec-out', 'run-as', $packageName, 'cat', 'no_backup/power-report.json')
$report = $rawReport | ConvertFrom-Json
if ($report.runId -ne $runId) { throw 'Relatório ausente ou de outra execução; inspecione o AVD e a sessão pendente.' }
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'report.json'), $rawReport, [Text.UTF8Encoding]::new($false))
if ($InterruptAfterApply) {
    if (-not $report.pendingRecovery -or $report.passed) { throw 'Interrupção não confirmada.' }
    Invoke-LabAdb @('shell', 'run-as', $packageName, 'test', '-f', 'no_backup/power-session.json') | Out-Null
    Write-Output 'INTERRUPÇÃO CONFIRMADA: sessão pendente no AVD. Execute este roteiro com -RecoverOnly antes de outra prova.'
} else {
    if ($result -notmatch 'OK \(1 test\)' -or $result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' -or -not $report.passed) {
        throw 'A prova falhou. Inspecione o relatório e preserve a sessão para recuperação.'
    }
    Write-Output 'PASS: prova concluída, estado restaurado, simulação removida e permissão revogada.'
    if (-not $RecoverOnly) {
        Write-Output "Controles observados: economia=$($report.powerSaveControlValidated); política=$($report.policyControlValidated); localização global=$($report.globalLocationControlValidated)."
    }
}
$environment = [pscustomobject]@{
    avd=$avdName
    configuration=$configuration
    labApkSha256=(Get-FileHash -LiteralPath $debugApk -Algorithm SHA256).Hash.ToLowerInvariant()
    testApkSha256=(Get-FileHash -LiteralPath $testApk -Algorithm SHA256).Hash.ToLowerInvariant()
}
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'environment.json'), ($environment | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
