#Requires -Version 5.1
<#
.SYNOPSIS
Coleta consultas ADB de somente leitura para investigação local de Galaxy Watch com Wear OS.
.DESCRIPTION
Não instala, não abre telas e não altera configurações do relógio. Grava somente
em LOCALAPPDATA\WatchReserveDiagnostics com acesso do usuário atual e SYSTEM.
Serial, fingerprint e dumps de atividades/energia exigem opções explícitas. Nunca envie
o relatório bruto ao repositório público.
#>
[CmdletBinding()]
param(
    [string]$Serial,
    [string]$AdbPath = 'adb',
    [switch]$IncludePrivateDetails,
    [switch]$CaptureActivities,
    [switch]$CapturePower
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($env:OS -ne 'Windows_NT' -or [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    throw 'Execute este script no Windows com LOCALAPPDATA disponível.'
}
if ($CaptureActivities -and -not $IncludePrivateDetails) {
    throw 'Use -IncludePrivateDetails junto com -CaptureActivities; a pilha pode conter dados pessoais.'
}
if ($CapturePower -and -not $IncludePrivateDetails) {
    throw 'Use -IncludePrivateDetails junto com -CapturePower; os contadores podem conter apps e alarmes pessoais.'
}
if ($CapturePower -and [string]::IsNullOrWhiteSpace($Serial)) {
    throw 'Use -Serial explícito junto com -CapturePower para escolher o relógio.'
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
if ($CapturePower) {
    $isWatch = Invoke-AdbRead -Arguments @('-s', $selectedSerial, 'shell', 'pm', 'has-feature', 'android.hardware.type.watch')
    if ($isWatch -ne 'true') { throw 'A coleta de energia exige um aparelho com o recurso Android de relógio.' }
}

$diagnosticsRoot = Join-Path $env:LOCALAPPDATA 'WatchReserveDiagnostics'
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
$report.Add('WatchReserve — diagnóstico privado; não publicar o conteúdo bruto.')
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
if ($CapturePower) {
    $report.Add('--- ENERGIA: DADOS POTENCIALMENTE PESSOAIS; NÃO PUBLICAR ---')
    $report.Add('Coleta fora da medição de autonomia; consultas podem acordar CPU e serviços.')
    $powerQueries = @(
        @{ Label = 'PowerManager e política de economia'; Args = @('dumpsys', '-t', '10', 'power') },
        @{ Label = 'DeviceIdle e allowlists'; Args = @('dumpsys', '-t', '10', 'deviceidle') },
        # --checkin pode consumir o arquivo histórico de check-in; --charged não faz isso.
        @{ Label = 'Batterystats desde a carga'; Args = @('dumpsys', '-t', '10', 'batterystats', '--charged') },
        @{ Label = 'Alarmes agendados'; Args = @('dumpsys', '-t', '10', 'alarm') },
        @{ Label = 'Tarefas agendadas'; Args = @('dumpsys', '-t', '10', 'jobscheduler') },
        @{ Label = 'Processos em cache e freezer'; Args = @('dumpsys', '-t', '10', 'activity', 'settings') },
        @{ Label = 'Pacote Samsung de alarme'; Args = @('dumpsys', '-t', '10', 'package', 'com.samsung.android.watch.alarm') },
        @{ Label = 'Pacote Samsung de bateria'; Args = @('dumpsys', '-t', '10', 'package', 'com.samsung.android.batterysavingsettings') }
    )
    foreach ($powerQuery in $powerQueries) {
        $report.Add('--- ' + $powerQuery.Label + ' ---')
        $report.Add('Consulta UTC: ' + [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ'))
        try {
            $value = Invoke-AdbRead -Arguments (@('-s', $selectedSerial, 'shell') + $powerQuery.Args)
            if ([string]::IsNullOrWhiteSpace($value) -or
                $value -match '(?im)DUMP TIMEOUT|Permission Denial|Permission denied|Can.t find service|Unable to find package|Unknown (package|option|command)') {
                $report.Add('CONSULTA INCOMPLETA OU INDISPONÍVEL; não interpretar como ausência de consumo.')
            } else { $report.Add('Consulta concluída; interpretar somente os campos efetivamente presentes.') }
            $report.Add($value)
        } catch {
            $report.Add('CONSULTA FALHOU; dados indisponíveis, não interpretar como ausência de consumo.')
        }
    }
    foreach ($settingGroup in @(
        @{ Namespace = 'global'; SettingNames = @('low_power', 'battery_saver_constants', 'restricted_device_performance',
            'low_power_back_data_off', 'wifi_scan_always_enabled', 'ble_scan_always_enabled',
            'mobile_data_always_on', 'cached_apps_freezer') },
        @{ Namespace = 'secure'; SettingNames = @('sm_connectivity_disable', 'location_mode') }
    )) {
        foreach ($settingKey in $settingGroup.SettingNames) {
            $report.Add('--- ' + $settingGroup.Namespace + '.' + $settingKey + ' ---')
            try {
                $value = Invoke-AdbRead -Arguments @('-s', $selectedSerial, 'shell', 'settings', 'get',
                    $settingGroup.Namespace, $settingKey)
                $report.Add($value)
            } catch { $report.Add('CONSULTA FALHOU; valor desconhecido.') }
        }
    }
}

$fileName = 'watchreserve-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.txt'
$reportPath = Join-Path $diagnosticsRoot $fileName
[IO.File]::WriteAllText($reportPath, ($report -join [Environment]::NewLine), (New-Object Text.UTF8Encoding($false)))
Write-Output ('Diagnóstico privado salvo em: ' + $reportPath)
Write-Output 'Não publique o relatório bruto. Nenhum ajuste do relógio foi alterado.'
