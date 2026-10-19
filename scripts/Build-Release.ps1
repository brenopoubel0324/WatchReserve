[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdkRoot = $env:ANDROID_HOME,
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'Watch8BatterySigning'),
    [string]$PrivateBackupDirectory = (Join-Path $env:USERPROFILE '.watch8-battery-signing-backup'),
    [string]$BuildToolsVersion = '36.0.0'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$watchProjectRoot = Split-Path -Parent $PSScriptRoot

function Protect-PrivateDirectory([string]$Path) {
    if (Test-Path -LiteralPath $Path) {
        $watchExisting = Get-Item -LiteralPath $Path -Force
        if (-not $watchExisting.PSIsContainer -or ($watchExisting.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'A assinatura deve usar diretórios locais reais, sem redirecionamento.'
        }
    }
    $watchDirectory = New-Item -ItemType Directory -Path $Path -Force
    $watchAcl = New-Object System.Security.AccessControl.DirectorySecurity
    $watchAcl.SetAccessRuleProtection($true, $false)
    $watchIdentity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($watchPrincipal in @($watchIdentity, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $watchRule = [System.Security.AccessControl.FileSystemAccessRule]::new(
            $watchPrincipal, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $watchAcl.AddAccessRule($watchRule)
    }
    if ($PSVersionTable.PSEdition -eq 'Core') {
        [IO.FileSystemAclExtensions]::SetAccessControl($watchDirectory, $watchAcl)
    } else {
        $watchDirectory.SetAccessControl($watchAcl)
    }
}

function Protect-PrivateFile([string]$Path) {
    $watchFile = Get-Item -LiteralPath $Path -Force
    if ($watchFile.PSIsContainer -or ($watchFile.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'Os arquivos privados devem ser arquivos locais reais.'
    }
    $watchFileAcl = New-Object System.Security.AccessControl.FileSecurity
    $watchFileAcl.SetAccessRuleProtection($true, $false)
    $watchFileIdentity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($watchPrincipal in @($watchFileIdentity, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $watchFileAcl.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new(
            $watchPrincipal, 'FullControl', 'Allow'))
    }
    if ($PSVersionTable.PSEdition -eq 'Core') {
        [IO.FileSystemAclExtensions]::SetAccessControl($watchFile, $watchFileAcl)
    } else {
        $watchFile.SetAccessControl($watchFileAcl)
    }
}

if ([string]::IsNullOrWhiteSpace($JavaHome) -or [string]::IsNullOrWhiteSpace($AndroidSdkRoot)) {
    throw 'Informe JAVA_HOME e ANDROID_HOME, ou os parâmetros JavaHome e AndroidSdkRoot.'
}
$JavaHome = [IO.Path]::GetFullPath($JavaHome)
$AndroidSdkRoot = [IO.Path]::GetFullPath($AndroidSdkRoot)
$SigningDirectory = [IO.Path]::GetFullPath($SigningDirectory)
$PrivateBackupDirectory = [IO.Path]::GetFullPath($PrivateBackupDirectory)
$watchJava = Join-Path $JavaHome 'bin\keytool.exe'
$watchBuildTools = Join-Path $AndroidSdkRoot "build-tools\$BuildToolsVersion"
foreach ($watchRequired in @($watchJava, (Join-Path $watchBuildTools 'zipalign.exe'), (Join-Path $watchBuildTools 'apksigner.bat'))) {
    if (-not (Test-Path -LiteralPath $watchRequired)) { throw "Ferramenta ausente: $watchRequired" }
}

# Nunca permitir que a chave ou seu backup sejam gravados dentro do projeto.
$watchProjectPrefix = [IO.Path]::GetFullPath($watchProjectRoot).TrimEnd('\') + '\'
foreach ($watchPrivatePath in @($SigningDirectory, $PrivateBackupDirectory)) {
    $watchAbsolute = [IO.Path]::GetFullPath($watchPrivatePath).TrimEnd('\') + '\'
    if ($watchAbsolute.StartsWith($watchProjectPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'A assinatura e o backup devem ficar fora do repositório.'
    }
}
if ([IO.Path]::GetFullPath($SigningDirectory).TrimEnd('\') -eq [IO.Path]::GetFullPath($PrivateBackupDirectory).TrimEnd('\')) {
    throw 'Escolha um diretório separado para o backup privado.'
}
Protect-PrivateDirectory $SigningDirectory
Protect-PrivateDirectory $PrivateBackupDirectory
$watchKeystore = Join-Path $SigningDirectory 'release.p12'
$watchPasswordFile = Join-Path $SigningDirectory 'store-password.txt'
$watchCertificatePin = Join-Path $watchProjectRoot 'signing-certificate.sha256'
$watchHasKey = Test-Path -LiteralPath $watchKeystore
$watchHasPassword = Test-Path -LiteralPath $watchPasswordFile
if ($watchHasKey -ne $watchHasPassword) {
    throw 'Assinatura incompleta. Recupere a chave e a senha do backup; não gere outra chave.'
}
if (-not $watchHasKey) {
    if (Test-Path -LiteralPath $watchCertificatePin) {
        throw 'Este projeto já possui identidade de release. Recupere a assinatura privada do backup.'
    }
    if ((Test-Path -LiteralPath (Join-Path $PrivateBackupDirectory 'release.p12')) -or
        (Test-Path -LiteralPath (Join-Path $PrivateBackupDirectory 'store-password.txt'))) {
        throw 'Já existe backup de assinatura. Recupere-o para preservar a identidade do aplicativo.'
    }
    $watchRandom = New-Object byte[] 32
    $watchGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $watchGenerator.GetBytes($watchRandom) } finally { $watchGenerator.Dispose() }
    [IO.File]::WriteAllText($watchPasswordFile, [Convert]::ToBase64String($watchRandom), [Text.Encoding]::ASCII)
    & $watchJava -genkeypair -noprompt -storetype PKCS12 -keystore $watchKeystore `
        -storepass:file $watchPasswordFile -keypass:file $watchPasswordFile `
        -alias watch8-battery -keyalg RSA -keysize 3072 -validity 10000 `
        -dname 'CN=Watch8 Battery Release'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar a chave de assinatura.' }
}
foreach ($watchPrivateFile in @('release.p12', 'store-password.txt')) {
    $watchSource = Join-Path $SigningDirectory $watchPrivateFile
    $watchBackup = Join-Path $PrivateBackupDirectory $watchPrivateFile
    Protect-PrivateFile $watchSource
    if (Test-Path -LiteralPath $watchBackup) {
        Protect-PrivateFile $watchBackup
        if ((Get-FileHash -LiteralPath $watchSource -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $watchBackup -Algorithm SHA256).Hash) {
            throw 'O backup existente não corresponde à assinatura. Não será sobrescrito.'
        }
    } else {
        Copy-Item -LiteralPath $watchSource -Destination $watchBackup
        Protect-PrivateFile $watchBackup
    }
}

# O hash público impede assinar atualizações com uma chave nova por engano.
$watchCertificate = Join-Path $SigningDirectory 'release-cert.der'
& $watchJava -exportcert -alias watch8-battery -keystore $watchKeystore `
    -storepass:file $watchPasswordFile -file $watchCertificate
if ($LASTEXITCODE -ne 0) { throw 'Falha ao verificar o certificado de assinatura.' }
$watchCertificateHash = (Get-FileHash -LiteralPath $watchCertificate -Algorithm SHA256).Hash.ToLowerInvariant()
if (Test-Path -LiteralPath $watchCertificatePin) {
    $watchExpectedCertificate = ([IO.File]::ReadAllText($watchCertificatePin)).Trim().ToLowerInvariant()
    if ($watchExpectedCertificate -notmatch '^[a-f0-9]{64}$' -or $watchExpectedCertificate -ne $watchCertificateHash) {
        throw 'A chave privada não corresponde ao certificado público fixado no projeto.'
    }
} else {
    [IO.File]::WriteAllText($watchCertificatePin, "$watchCertificateHash`n", [Text.Encoding]::ASCII)
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdkRoot
$watchVersionMatch = [regex]::Match((Get-Content -LiteralPath (Join-Path $watchProjectRoot 'app\build.gradle.kts') -Raw), 'versionName\s*=\s*"([0-9A-Za-z.+-]+)"')
if (-not $watchVersionMatch.Success) { throw 'Não foi possível identificar a versão do aplicativo.' }
Push-Location $watchProjectRoot
try {
    & .\gradlew.bat --no-daemon :app:assembleRelease
    if ($LASTEXITCODE -ne 0) { throw 'O build de release falhou.' }
    $watchOutput = Join-Path $watchProjectRoot 'app\build\outputs\apk\release'
    $watchUnsigned = Join-Path $watchOutput 'app-release-unsigned.apk'
    $watchAligned = Join-Path $watchOutput 'app-release-aligned.apk'
    $watchSigned = Join-Path $watchOutput "Watch8-Battery-$($watchVersionMatch.Groups[1].Value).apk"
    & (Join-Path $watchBuildTools 'zipalign.exe') -P 16 -f 4 $watchUnsigned $watchAligned
    if ($LASTEXITCODE -ne 0) { throw 'O alinhamento do APK falhou.' }
    & (Join-Path $watchBuildTools 'apksigner.bat') sign --ks $watchKeystore `
        --ks-key-alias watch8-battery --ks-pass "file:$watchPasswordFile" `
        --out $watchSigned $watchAligned
    if ($LASTEXITCODE -ne 0) { throw 'A assinatura do APK falhou.' }
    & (Join-Path $watchBuildTools 'apksigner.bat') verify --verbose $watchSigned
    if ($LASTEXITCODE -ne 0) { throw 'A verificação da assinatura falhou.' }
    & (Join-Path $watchBuildTools 'zipalign.exe') -c -P 16 4 $watchSigned
    if ($LASTEXITCODE -ne 0) { throw 'A verificação do alinhamento do APK final falhou.' }
    $watchApkHash = (Get-FileHash -LiteralPath $watchSigned -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$watchSigned.sha256", "$watchApkHash  $([IO.Path]::GetFileName($watchSigned))`n", [Text.Encoding]::ASCII)
    Write-Output "APK assinado: $watchSigned"
    Write-Output 'Assinatura e backup privados preservados fora do repositório. Mantenha também uma cópia offline segura.'
} finally {
    Pop-Location
}
