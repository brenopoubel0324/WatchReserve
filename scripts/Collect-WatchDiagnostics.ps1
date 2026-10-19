#Requires -Version 5.1
<#
.SYNOPSIS
Coleta consultas ADB de somente leitura para investigação local do Watch8.
.DESCRIPTION
Não instala, não abre telas e não altera configurações do relógio. Grava somente
em LOCALAPPDATA\Watch8BatteryDiagnostics com acesso do usuário atual e SYSTEM.
Serial, fingerprint e dumps de atividades exigem opções explícitas. Nunca envie
o relatório bruto ao repositório público.
#>
[CmdletBinding()]
param(
    [string]$Serial,
    [string]$AdbPath = 'adb',
    [switch]$IncludePrivateDetails,
    [switch]$CaptureActivities
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($env:OS -ne 'Windows_NT' -or [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    throw 'Execute este script no Windows com LOCALAPPDATA disponível.'
}
if ($CaptureActivities -and -not $IncludePrivateDetails) {
    throw 'Use -IncludePrivateDetails junto com -CaptureActivities; a pilha pode conter dados pessoais.'
}
if ($Serial -and $Serial -notmatch '^[A-Za-z0-9._:\[\]-]+$') {
    throw 'Identificador ADB inválido. Use exatamente o identificador local exibido por adb devices.'
}

try {
    $adbExecutable = (Get-Command -Name $AdbPath -CommandType Application, ExternalScript -ErrorAction Stop).Source
} catch {
    throw 'ADB não encontrado. Instale Platform Tools ou use -AdbPath com o caminho de adb.exe.'
}

function Invoke-AdbRead {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    try {
        $lines = @(& $adbExecutable @Arguments 2>&1)
        $commandExitCode = $LASTEXITCODE
    } catch {
        throw 'A consulta ADB falhou. Confira localmente a conexão e a autorização do relógio.'
    }
    if ($commandExitCode -ne 0) {
        throw 'A consulta ADB falhou. Confira localmente a conexão e a autorização do relógio.'
    }
    return ($lines | Out-String).Trim()
}

$deviceList = Invoke-AdbRead -Arguments @('devices')
$connectedDevices = @(
    foreach ($line in ($deviceList -split '\r?\n')) {
        if ($line -match '^([^\s]+)\s+device(?:\s|$)') {
            $Matches[1]
        }
    }
)
if ($Serial) {
    if ($Serial -notin $connectedDevices) {
        throw 'O aparelho escolhido não está conectado e autorizado. Confira adb devices localmente.'
    }
    $selectedSerial = $Serial
} elseif ($connectedDevices.Count -eq 1) {
    $selectedSerial = $connectedDevices[0]
} elseif ($connectedDevices.Count -eq 0) {
    throw 'Nenhum aparelho ADB conectado e autorizado. Pareie e conecte o relógio antes da coleta.'
} else {
    throw 'Há vários aparelhos conectados. Confira adb devices localmente e execute com -Serial explícito.'
}

$diagnosticsRoot = Join-Path $env:LOCALAPPDATA 'Watch8BatteryDiagnostics'
if (Test-Path -LiteralPath $diagnosticsRoot) {
    $existingItem = Get-Item -LiteralPath $diagnosticsRoot -Force
    if (-not $existingItem.PSIsContainer -or ($existingItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'O diretório privado deve ser uma pasta local real, sem redirecionamento.'
    }
} else {
    $null = New-Item -Path $diagnosticsRoot -ItemType Directory
}

try {
    $userIdentity = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $systemIdentity = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
    $privateAcl = New-Object Security.AccessControl.DirectorySecurity
    $privateAcl.SetAccessRuleProtection($true, $false)
    foreach ($identity in @($userIdentity, $systemIdentity)) {
        $rule = New-Object Security.AccessControl.FileSystemAccessRule(
            $identity,
            [Security.AccessControl.FileSystemRights]::FullControl,
            [Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit',
            [Security.AccessControl.PropagationFlags]::None,
            [Security.AccessControl.AccessControlType]::Allow
        )
        $privateAcl.AddAccessRule($rule)
    }
    $diagnosticsDirectory = Get-Item -LiteralPath $diagnosticsRoot
    if ($PSVersionTable.PSEdition -eq 'Core') {
        [IO.FileSystemAclExtensions]::SetAccessControl($diagnosticsDirectory, $privateAcl)
    } else {
        $diagnosticsDirectory.SetAccessControl($privateAcl)
    }
} catch {
    throw 'Não foi possível restringir o diretório privado. Nenhum relatório foi gravado.'
}

$report = New-Object 'System.Collections.Generic.List[string]'
$report.Add('Watch8-Battery — diagnóstico privado; não publicar o conteúdo bruto.')
$report.Add('Coleta UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))
$report.Add('Consultas somente leitura; nenhum ajuste do relógio foi alterado.')

$propertyNames = @(
    'ro.product.manufacturer',
    'ro.product.model',
    'ro.build.version.release',
    'ro.build.version.sdk',
    'ro.build.version.security_patch',
    'ro.build.version.incremental'
)
if ($IncludePrivateDetails) {
    $report.Add('Serial ADB selecionado: ' + $selectedSerial)
    $propertyNames += 'ro.build.fingerprint'
}
foreach ($propertyName in $propertyNames) {
    $value = Invoke-AdbRead -Arguments @('-s', $selectedSerial, 'shell', 'getprop', $propertyName)
    $report.Add($propertyName + ': ' + $value)
}

foreach ($action in @('android.settings.BATTERY_SAVER_SETTINGS', 'android.settings.SETTINGS')) {
    $resolved = Invoke-AdbRead -Arguments @(
        '-s', $selectedSerial, 'shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', $action
    )
    $report.Add('Resolução de ' + $action + ': ' + $resolved)
}
if ($CaptureActivities) {
    $report.Add('--- PILHA DE ATIVIDADES: DADOS POTENCIALMENTE PESSOAIS ---')
    $activityDump = Invoke-AdbRead -Arguments @('-s', $selectedSerial, 'shell', 'dumpsys', 'activity', 'activities')
    $report.Add($activityDump)
}

$fileName = 'watch8-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.txt'
$reportPath = Join-Path $diagnosticsRoot $fileName
[IO.File]::WriteAllText($reportPath, ($report -join [Environment]::NewLine), (New-Object Text.UTF8Encoding($false)))
Write-Output ('Diagnóstico privado salvo em: ' + $reportPath)
Write-Output 'Não publique o relatório bruto. Nenhum ajuste do relógio foi alterado.'
