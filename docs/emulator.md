# Testes autônomos em emulador Wear OS

O app pode ser testado no computador sem conectar o relógio. Um emulador Wear OS exercita interface, navegação, foco, armazenamento e abertura de configurações públicas. Ele não reproduz o firmware One UI Watch, o seletor Samsung de duplo clique em Home nem a autonomia física do Watch8.

## Ambiente desta revisão

| Item | Configuração |
|---|---|
| Imagem | `system-images;android-34;android-wear;x86_64`, Wear OS 5/API 34 |
| Perfil | `wearos_small_round`, 384×384 px, 320 dpi, área lógica de 192 dp |
| Forma | `round` confirmado em `adb shell am get-config` |
| Emulator | 37.2.12, WHPX utilizável, dois núcleos e 1.536 MB de RAM |
| Janela | Execução sem janela, com renderização por software |
| Alvo reservado | AVD `WatchReserve_Wear5_Small_Round`, serial `emulator-5580` |

As ferramentas e os dados do AVD ficam fora do repositório. Não houve alteração da BIOS, instalação de driver de virtualização, ativação de recurso do Windows ou reinício do PC. A checagem `emulator -accel-check` retornou código 0.

## Preparar e repetir

Requisitos: JDK 17, SDK Android com API/build-tools 36, Platform Tools, Emulator e a imagem acima. O roteiro PowerShell exige **7.4 ou superior** para preservar os bytes das capturas; os scripts anteriores de diagnóstico e assinatura mantêm a compatibilidade já documentada com Windows PowerShell 5.1.

1. Crie um AVD próprio com o nome reservado, a imagem e o perfil da tabela. O `avdmanager` usado nesta sessão gerou `hw.lcd.circular=false` apesar do nome do perfil; o arquivo `config.ini` do AVD de teste foi ajustado para `hw.lcd.circular=true` antes de iniciar. Confirme a forma no Android antes de aceitar resultados.
2. Inicie esse AVD em uma porta livre. Na execução desta revisão foram usados `-port 5580 -no-window -no-snapshot -no-audio -no-boot-anim -gpu software -memory 1536 -cores 2 -accel on -no-metrics`. Ao usar `Start-Process` no Windows, mantenha `-WindowStyle Hidden`.
3. Configure `JAVA_HOME` e `ANDROID_HOME` para o ambiente local e compile o app e o pacote de testes:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
```

4. Execute o roteiro com serial e diretório de saída explícitos:

```powershell
.\scripts\Test-WearEmulator.ps1 `
    -Serial emulator-5580 `
    -AdbPath "$env:ANDROID_HOME\platform-tools\adb.exe" `
    -OutputDirectory "$env:LOCALAPPDATA\WatchReserveEmulatorReports"
```

O roteiro confirma que o alvo é um emulador, confere o nome do AVD e exige tela circular Wear OS antes de instalar APKs ou alterar configurações. Um AVD divergente foi rejeitado na verificação negativa. Não usa seleção automática nem o comando Gradle que possa escolher todos os dispositivos conectados.

Fonte e tempo de tela são alterados apenas no AVD confirmado, com tentativa de restauração independente de ambos no `finally`. Falha na restauração interrompe o roteiro com informação explícita. Nenhum grant, ajuste ou coleta é executado no relógio pessoal.

## Cobertura e evidências

O pacote instrumentado contém seis testes, executados com fonte 1,0 e 1,3:

- Ação principal visível na abertura de uma tela circular e indicação de destino não validado.
- Entrada em Medição, recriação da atividade e retorno ao painel com Back.
- Componente inválido, repetição do mesmo erro e bloqueio do teste do destino.
- Evento Android de rolagem rotativa entregue ao painel em foco.
- Falha real de gravação de preferências sem confirmar o destino no cache.
- Falha real de gravação sem encerrar uma medição ativa.

Os dois últimos usam preferências temporárias com nome exclusivo. A permissão de escrita do diretório do próprio app é retirada durante a tentativa e restaurada em `finally`; as fixtures são removidas. Uma checagem de hardware recusa a execução dos testes em relógio físico. Não são testes de controles globais de economia.

As capturas sincronizam o desenho Compose e obtêm a tela do Android. Devem ser inspecionadas antes de publicação; o resultado automático não substitui a revisão de acabamento. Relatórios locais também registram o hash do APK debug, a configuração do AVD e a ausência de validação Samsung ou medição de autonomia.

Veja a [auditoria de execução](auditorias/2026-10-19-execucao.md) para os resultados finais e as limitações remanescentes. Dados de bateria simulados servem somente para verificar fluxo, sem representar ganho de autonomia.

## Encerrar

Depois dos testes, desligue apenas o emulador reservado:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5580 emu kill
```

Não é necessário manter ADB ou emulador ligados para usar o app no relógio. [Guia oficial de emulação Wear OS](https://developer.android.com/training/wearables/get-started/creating), [aceleração do Android Emulator](https://developer.android.com/studio/run/emulator-acceleration).
