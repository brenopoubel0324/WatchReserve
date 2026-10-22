#Requires -Version 7.4
<#
.SYNOPSIS
Testa o cliente de leitura com uma ponte já iniciada e autorizada no AVD reservado.
.DESCRIPTION
Instala somente os APKs advancedLab e executa a instrumentação em fontes 1,0 e 1,3.
Não instala nem inicia a ponte, não concede permissões e não altera controles de energia.
Restaura a escala de fonte ao terminar. Capturas ficam fora do repositório.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^emulator-[0-9]+$')][string]$Serial,
    [Parameter(Mandatory = $true)][string]$OutputDirectory,
    [string]$AdbPath = 'adb'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$adbExecutable = (Get-Command -Name $AdbPath -CommandType Application -ErrorAction Stop).Source
$packageName = 'dev.brenopoubel.watchreserve.advancedlab'
$expectedAvd = 'WatchReserve_Wear5_Small_Round'

function Invoke-LabAdb([string[]]$Arguments) {
    $lines = @(& $adbExecutable -s $Serial @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "ADB do laboratório falhou: $($Arguments[0])" }
    return ($lines | Out-String).Trim()
}

if ((Invoke-LabAdb @('get-state')) -ne 'device' -or
    (Invoke-LabAdb @('shell', 'getprop', 'ro.kernel.qemu')) -ne '1') {
    throw 'O alvo deve ser um emulador.'
}
$avdName = ((Invoke-LabAdb @('emu', 'avd', 'name')) -split '\r?\n')[0].Trim()
$configuration = Invoke-LabAdb @('shell', 'am', 'get-config')
if ($avdName -ne $expectedAvd -or $configuration -notmatch '-round-' -or $configuration -notmatch '-watch-') {
    throw 'Use o AVD Wear OS circular reservado do WatchReserve.'
}
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
if ($OutputDirectory.Equals($projectRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $OutputDirectory.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Escolha uma pasta de resultados fora do repositório.'
}
$debugApk = Join-Path $projectRoot 'advancedLab/build/outputs/apk/debug/advancedLab-debug.apk'
$testApk = Join-Path $projectRoot 'advancedLab/build/outputs/apk/androidTest/debug/advancedLab-debug-androidTest.apk'
foreach ($apk in @($debugApk, $testApk)) {
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "Compile o laboratório: $apk" }
    Invoke-LabAdb @('install', '-r', $apk) | Out-Null
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$originalFont = Invoke-LabAdb @('shell', 'settings', 'get', 'system', 'font_scale')
if ($originalFont -ne 'null' -and $originalFont -notmatch '^[0-9]+(\.[0-9]+)?$') { throw 'Escala original inválida.' }
try {
    foreach ($scale in @('1.0', '1.3')) {
        $runDirectory = Join-Path $OutputDirectory "font-$scale"
        New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
        Invoke-LabAdb @('shell', 'settings', 'put', 'system', 'font_scale', $scale) | Out-Null
        $result = Invoke-LabAdb @('shell', 'am', 'instrument', '-w', '-e', 'reservedAvd', $expectedAvd,
            "$packageName.test/androidx.test.runner.AndroidJUnitRunner")
        [IO.File]::WriteAllText((Join-Path $runDirectory 'instrumentation.txt'), $result, [Text.UTF8Encoding]::new($false))
        if ($result -notmatch 'OK \([1-9][0-9]* tests?\)' -or
            $result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
            throw "Instrumentação falhou na escala $scale. Confira o relatório privado."
        }
        foreach ($name in @('home', 'report')) {
            # O PowerShell 7.4+ preserva bytes no redirecionamento de comandos nativos.
            $capture = Join-Path $runDirectory "$name.png"
            & $adbExecutable -s $Serial exec-out run-as $packageName cat "files/review-screenshots/$name.png" > $capture
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao copiar captura do laboratório.' }
        }
        Write-Output "PASS: instrumentação, coleta shell e encerramento do helper; fonte $scale."
    }
} finally {
    if ($originalFont -eq 'null') {
        Invoke-LabAdb @('shell', 'settings', 'delete', 'system', 'font_scale') | Out-Null
    } else {
        Invoke-LabAdb @('shell', 'settings', 'put', 'system', 'font_scale', $originalFont) | Out-Null
    }
}
