#Requires -Version 5.1
<#
.SYNOPSIS
Verifica o diagnóstico com ADB simulado, sem acessar aparelhos reais.
#>
[CmdletBinding()]
param([string]$FixtureRoot)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$diagnosticsScript = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\Collect-WatchDiagnostics.ps1'))
$temporaryPrefix = Join-Path ([IO.Path]::GetTempPath()) 'watch8-diagnostics-test-'
$markerName = '.watch8-diagnostics-fixture'

function Assert-FixturePath {
    param([string]$Path)
    $absolutePath = [IO.Path]::GetFullPath($Path)
    if (-not $absolutePath.StartsWith($temporaryPrefix, [StringComparison]::OrdinalIgnoreCase) -or
        (Split-Path $absolutePath -Leaf) -notmatch '^watch8-diagnostics-test-[a-f0-9]{32}$') {
        throw 'Caminho de teste fora do diretório temporário esperado.'
    }
    $rootItem = Get-Item -LiteralPath $absolutePath -Force
    if (-not $rootItem.PSIsContainer -or ($rootItem.Attributes -band [IO.FileAttributes]::ReparsePoint) -or
        -not (Test-Path -LiteralPath (Join-Path $absolutePath $markerName))) {
        throw 'Diretório de teste inválido; a limpeza automática foi bloqueada.'
    }
}

if ($FixtureRoot) {
    Assert-FixturePath $FixtureRoot
    $expectedLocalAppData = Join-Path $FixtureRoot 'LocalAppData'
    if ($env:LOCALAPPDATA -ne $expectedLocalAppData) {
        throw 'O subprocesso precisa de LOCALAPPDATA isolado.'
    }
    [Console]::OutputEncoding = New-Object Text.UTF8Encoding($false)
    $fakeAdb = Join-Path $FixtureRoot 'fake-adb.ps1'
    $caseFile = Join-Path $FixtureRoot 'case.txt'
    $callLog = Join-Path $FixtureRoot 'calls.txt'
    $fakeCode = @'
$global:LASTEXITCODE = 0
$fixtureFolder = Split-Path $PSCommandPath -Parent
Add-Content -LiteralPath (Join-Path $fixtureFolder 'calls.txt') -Value ($args -join ' ')
if ($args[0] -eq 'devices') {
    Write-Output 'List of devices attached'
    $fixtureCase = [IO.File]::ReadAllText((Join-Path $fixtureFolder 'case.txt'))
    if ($fixtureCase -eq 'none') { return }
    Write-Output "fixture-watch-a`tdevice"
    if ($fixtureCase -eq 'multiple') { Write-Output "fixture-watch-b`tdevice" }
    return
}
if ($args -contains 'getprop') {
    if ($args[-1] -eq 'ro.build.fingerprint') { 'FIXTURE-PRIVATE-FINGERPRINT' }
    else { 'fixture-version' }
    return
}
if ($args -contains 'resolve-activity') { 'fixture.settings/.Settings'; return }
if ($args -contains 'dumpsys') { 'FIXTURE-PRIVATE-ACTIVITIES'; return }
$global:LASTEXITCODE = 1
'@
    [IO.File]::WriteAllText($fakeAdb, $fakeCode)

    function Assert-ExpectedFailure {
        param([scriptblock]$Operation, [string]$MessagePrefix)
        try { & $Operation | Out-Null }
        catch {
            if ($_.Exception.Message.StartsWith($MessagePrefix, [StringComparison]::Ordinal)) { return }
            throw
        }
        throw ('Falha esperada não ocorreu: ' + $MessagePrefix)
    }

    [IO.File]::WriteAllText($caseFile, 'none')
    Assert-ExpectedFailure { & $diagnosticsScript -AdbPath $fakeAdb } 'Nenhum aparelho ADB'
    [IO.File]::WriteAllText($caseFile, 'multiple')
    Assert-ExpectedFailure { & $diagnosticsScript -AdbPath $fakeAdb } 'Há vários aparelhos'
    Write-Output 'PASS: nenhum aparelho e seleção explícita com múltiplos aparelhos.'

    [IO.File]::WriteAllText($caseFile, 'one')
    foreach ($privateMode in @($false, $true, $true)) {
        $result = @(& $diagnosticsScript -AdbPath $fakeAdb -IncludePrivateDetails:$privateMode -CaptureActivities:$privateMode)
        $reportPath = $result[0].Substring('Diagnóstico privado salvo em: '.Length)
        $expectedDirectory = Join-Path $expectedLocalAppData 'WatchReserveDiagnostics'
        if ((Split-Path $reportPath -Parent) -ne $expectedDirectory) {
            throw 'O relatório saiu do diretório temporário privado.'
        }
        $report = [IO.File]::ReadAllText($reportPath)
        if ($privateMode) {
            if ($report -notmatch 'FIXTURE-PRIVATE-FINGERPRINT' -or
                $report -notmatch 'FIXTURE-PRIVATE-ACTIVITIES' -or $report -notmatch 'fixture-watch-a') {
                throw 'O opt-in não incluiu os dados simulados esperados.'
            }
        } elseif ($report -match 'FIXTURE-PRIVATE|fixture-watch-a') {
            throw 'O relatório padrão expôs identificadores ou dumps simulados.'
        }
        $acl = Get-Acl -LiteralPath $expectedDirectory
        $expectedSids = @([Security.Principal.WindowsIdentity]::GetCurrent().User.Value, 'S-1-5-18') | Sort-Object
        $actualSids = @($acl.Access | ForEach-Object {
            $_.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
        }) | Sort-Object
        if (-not $acl.AreAccessRulesProtected -or $acl.Access.Count -ne 2 -or
            @(Compare-Object $expectedSids $actualSids).Count -ne 0) {
            throw 'A ACL não está restrita ao usuário atual e SYSTEM.'
        }
    }
    Write-Output 'PASS: padrão sanitizado, opt-in privado e ACL em execuções repetidas.'
    Assert-ExpectedFailure { & $diagnosticsScript -AdbPath $fakeAdb -CaptureActivities } 'Use -IncludePrivateDetails'
    Assert-ExpectedFailure { & $diagnosticsScript -AdbPath $fakeAdb -Serial 'fixture-disconnected' } 'O aparelho escolhido'
    if (@(Get-Content -LiteralPath $callLog | Where-Object {
        $_ -notmatch '^(devices|-s fixture-watch-a shell (getprop |cmd package resolve-activity |dumpsys activity activities$))'
    }).Count -gt 0) {
        throw 'O diagnóstico chamou algo fora das consultas permitidas.'
    }
    Write-Output 'PASS: rejeição de seleções inválidas e consultas ADB somente leitura.'
    exit 0
}

if ($env:OS -ne 'Windows_NT') { throw 'Execute estes testes no Windows.' }
$originalLocalAppData = $env:LOCALAPPDATA
$testRoot = $temporaryPrefix + [Guid]::NewGuid().ToString('N')
$null = New-Item -Path $testRoot -ItemType Directory
[IO.File]::WriteAllText((Join-Path $testRoot $markerName), 'fixture sem dados reais')
try {
    $startInfo = New-Object Diagnostics.ProcessStartInfo
    $startInfo.FileName = [Diagnostics.Process]::GetCurrentProcess().MainModule.FileName
    $startInfo.Arguments = '-NoProfile -ExecutionPolicy Bypass -File "' + $PSCommandPath + '" -FixtureRoot "' + $testRoot + '"'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = New-Object Text.UTF8Encoding($false)
    $startInfo.StandardErrorEncoding = New-Object Text.UTF8Encoding($false)
    $startInfo.EnvironmentVariables['LOCALAPPDATA'] = Join-Path $testRoot 'LocalAppData'
    $child = [Diagnostics.Process]::Start($startInfo)
    $stdoutTask = $child.StandardOutput.ReadToEndAsync()
    $stderrTask = $child.StandardError.ReadToEndAsync()
    if (-not $child.WaitForExit(60000)) { $child.Kill(); throw 'Os testes excederam 60 segundos.' }
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    Write-Output $stdout.Trim()
    if ($child.ExitCode -ne 0) { throw ('Testes falharam: ' + $stderr) }
    if ($env:LOCALAPPDATA -ne $originalLocalAppData) { throw 'O ambiente do processo pai foi alterado.' }
} finally {
    Assert-FixturePath $testRoot
    $redirectedItems = @(Get-ChildItem -LiteralPath $testRoot -Recurse -Force | Where-Object {
        $_.Attributes -band [IO.FileAttributes]::ReparsePoint
    })
    if ($redirectedItems.Count -gt 0) { throw 'Limpeza bloqueada: há redirecionamento no diretório temporário.' }
    Remove-Item -LiteralPath ([IO.Path]::GetFullPath($testRoot)) -Recurse -Force
}
