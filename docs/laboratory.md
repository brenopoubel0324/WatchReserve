# Laboratório de controles diretos

O módulo `:lab` é uma prova técnica separada do WatchReserve v0.2.1. Pacote: `dev.brenopoubel.watchreserve.lab`. Não tem launcher, interface, serviços, rede, dependência do app principal ou variante release. A permissão `WRITE_SECURE_SETTINGS` existe somente neste APK de laboratório; o app publicado continua sem ela.

## Decisão para prosseguir

Priorizar controles com efeito verificável e restauração persistente. A revisão prévia do plano encontrou lacunas de identidade de escrita, recuperação após interrupção e isolamento de permissões; elas foram incorporadas antes da execução. Veja a [autoauditoria](auditorias/2026-10-19-laboratorio.md) e o [plano](planos/2026-10-19-watchreserve.md).

A imagem Wear OS 5/API 34 usada nesta investigação declara `config_batterySaverSupported=false`. A escrita de `low_power` foi aceita, mas `PowerManager.isPowerSaveMode` continuou falso; a política de localização do modo AOSP também não foi observada. Portanto, não há controle de economia AOSP aprovado nesta imagem. Isso não comprova impossibilidade no Samsung, que precisa de investigação própria.

A localização global é uma prova distinta: escrita de `Settings.Secure.location_mode` pelo UID do APK, observação em `LocationManager.isLocationEnabled` e restauração do estado anterior. Seu efeito no consumo de bateria não é medido por esse teste.

A v0.2.2 já comprovou a confirmação Samsung e os três acessos em uso normal no Watch8.
O próximo passo dos controles é observar a economia nativa ligada e a atividade residual,
conforme a [investigação aprofundada](referencias/wearos-profundo.md). O dump anterior foi
coletado com economia desligada e declara Low Power Standby sem suporte. As políticas
completas já incluem várias restrições; novos perfis precisam de ganho adicional demonstrado.

## Executar no AVD reservado

Use o ambiente do [guia de emulação](emulator.md): AVD `WatchReserve_Wear5_Small_Round`, tela circular, serial explícito e PowerShell 7.4+. O laboratório recusa relógio físico. A execução normal exige economia inicialmente desligada, nenhuma bateria simulada e permissão avançada inicialmente negada ao pacote lab.

Compile com o mesmo JDK/SDK do projeto:

```powershell
.\gradlew.bat --no-daemon `
    :lab:assembleDebug :lab:assembleDebugAndroidTest :lab:lintDebug
```

Execute apenas no emulador reservado:

```powershell
.\scripts\Test-PowerControlsLab.ps1 `
    -Serial emulator-5580 `
    -AdbPath "$env:ANDROID_HOME\platform-tools\adb.exe" `
    -OutputDirectory "$env:LOCALAPPDATA\WatchReserveLabReports\normal"
```

O roteiro verifica emulador, nome do AVD e configuração Wear circular antes de instalar. A instrumentação verifica hardware, pacote/UID e argumento reservado antes de escrever. Não adota identidade shell para as escritas: `ContentResolver` usa o UID comum do pacote após concessão explícita da permissão ao laboratório. Comandos shell são usados para grant/revogação e para preparar/resetar a bateria simulada.

O teste grava uma sessão atômica com sincronização e releitura, antes das alterações. Conserva o original e recusa nova aplicação com sessão pendente. Ao terminar, tenta restaurar cada ajuste, confirma chaves e APIs, remove a simulação e revoga a permissão. Se algo falhar, mantém o registro. A recuperação é uma fixture exclusiva desse AVD, não o futuro contrato de conflitos do app de usuário.

## Interromper e recuperar

Para testar a perda real do processo, use um diretório diferente e acrescente `-InterruptAfterApply`. Esse modo altera a fixture e encerra o próprio processo com o registro pendente; **não é uma execução aprovada do teste completo**. O relatório mostra `pendingRecovery=true` e `passed=false`.

Depois execute o mesmo roteiro com `-RecoverOnly`, usando outro diretório de saída. Essa execução apenas restaura a sessão encontrada. Uma execução normal enquanto há sessão pendente deve falhar antes de alterar os valores originais. Não desinstale o laboratório nem apague seus dados enquanto houver sessão pendente.

Se a recuperação não puder ler o registro, interrompa novos experimentos e preserve o arquivo `no_backup/power-session.json` do pacote lab. Os valores anteriores ficam nesse JSON; qualquer recuperação manual precisa respeitar cada namespace e valor registrado. O roteiro mantém o alvo reservado e não tenta recuperar um relógio físico.

## Interpretar os resultados

- `passed`: a sequência de prova/restauração terminou sem falhas de infraestrutura; não significa que todos os controles funcionaram.
- `powerSaveControlValidated` e `policyControlValidated`: exigem efeito observado por `PowerManager`, além de escrita aceita.
- `globalLocationControlValidated`: exige mudança observada pelo serviço de localização.
- `settingsRestored`, `runtimeStateRestored`, `batterySimulationReset` e `permissionRevoked`: confirmam a limpeza da fixture.
- `environment.json`: identifica o AVD e os hashes dos APKs do laboratório.
- `runId`: impede aceitar um relatório antigo como evidência da execução atual.

Os relatórios não contêm fingerprint, serial de relógio, rede pessoal ou dumps de sistema. Não publicar o arquivo de sessão bruto de um aparelho pessoal. A [evidência revisada](evidencias/2026-10-19-controles/README.md) distingue resultados observados de controles não validados.

A CI compila e executa lint do laboratório e compila sua instrumentação. A execução real acontece no AVD local. Não há publicação de APK lab como release do app de usuário.

Fontes das verificações independentes: [PowerManager](https://developer.android.com/reference/android/os/PowerManager#getLocationPowerSaveMode()), [LocationManager](https://developer.android.com/reference/android/location/LocationManager#isLocationEnabled()), [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile).
