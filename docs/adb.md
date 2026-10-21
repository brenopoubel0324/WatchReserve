# Instalação e investigação por ADB

O app funciona desconectado. ADB é usado para instalar e investigar o comportamento do firmware, não para manter a economia funcionando. A depuração deve ser desligada durante as medições de autonomia.

Em 21/10/2026, o Watch8 foi conectado pelo PC e a [coleta física](evidencias/2026-10-21-watch8/README.md) identificou a candidata Samsung. Depois, com autorização do usuário para instalar e testar, a v0.2.2 abriu a confirmação pelo UID comum do app. Os testes dos três acessos continuam separados.

## Opção A: instalar pelo celular com Bugjaeger

Baixe o APK de [Releases](https://github.com/brenopoubel0324/WatchReserve/releases) no celular e deixe celular e relógio na mesma rede Wi-Fi. No relógio, habilite as opções do desenvolvedor e a depuração sem fio. No Bugjaeger, use o pareamento com o código, IP e porta mostrados pelo relógio; depois conecte à porta da tela principal de depuração, que pode ser diferente da porta de pareamento.

Com o relógio escolhido no Bugjaeger, abra a aba de pacotes e use a ação de instalar APK para selecionar o arquivo baixado. Os nomes e posições podem variar conforme a versão do app. Confirme o sucesso e procure “Só relógio” no launcher do relógio; o nome do app é WatchReserve. Este fluxo é baseado na documentação do desenvolvedor do Bugjaeger; ainda não foi validado fisicamente nos modelos-alvo. [Pareamento de relógio](https://sisik.eu/blog/android/bugjaeger/connect-watch), [instalação de APK](https://sisik.eu/blog/android/bugjaeger/sideload-apk-files)

Ao terminar, desconecte o Bugjaeger e desligue a depuração sem fio no relógio. Não são necessários comandos para conceder permissões ou modificar configurações globais. A coleta por PowerShell abaixo é uma alternativa para investigação futura pelo PC.

## Opção B: preparar e parear pelo PC

Instale Android SDK Platform Tools. Se `adb` não estiver no `PATH`, use seu caminho completo ou adicione a pasta `platform-tools` somente à sessão atual do PowerShell.

No relógio, habilite as opções do desenvolvedor pelo número da versão do software e abra a depuração sem fio. Os rótulos e o caminho podem mudar conforme o firmware. Use a opção de parear novo dispositivo. Execute este bloco e informe o endereço completo de pareamento mostrado no relógio:

```powershell
$watchPairEndpoint = Read-Host 'IP e porta de pareamento, no formato endereço:porta'
adb pair $watchPairEndpoint
```

O endereço contém quatro números separados por pontos, seguidos de dois-pontos e uma porta numérica. Não escreva a palavra `IP` nem o nome de uma variável no lugar do endereço. No prompt `Enter pairing code`, digite somente o código numérico atual e pressione Enter; aguarde o resultado antes de executar outro comando.

Depois de confirmar o pareamento, volte à tela principal de depuração sem fio do relógio e execute este bloco com o endereço de conexão mostrado nela:

```powershell
$watchConnectEndpoint = Read-Host 'IP e porta de conexão, no formato endereço:porta'
adb connect $watchConnectEndpoint
adb devices
```

A porta da conexão é a mostrada na tela principal de depuração sem fio; ela pode ser diferente da porta de pareamento. PC e relógio devem estar na mesma rede compatível. IP, portas, códigos e a saída de `adb devices` são dados locais; não os publique. [Guia oficial de depuração por Wi-Fi no Wear OS](https://developer.android.com/training/wearables/get-started/debug-wifi)

## Instalar um APK

Escolha explicitamente o identificador exibido em `adb devices`. Não use os valores ilustrativos abaixo literalmente.

```powershell
$watchSerial = 'IDENTIFICADOR_LOCAL_DO_RELOGIO'
adb -s $watchSerial install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

Para uma release, use o caminho do APK baixado em Releases, como `WatchReserve-0.2.0.apk`. O nome novo preserva application ID e assinatura para atualizar a v0.1.0. Atualizações com `install -r` exigem assinatura compatível; debug e release podem ter chaves diferentes. Não desinstale apenas para contornar um erro sem considerar a perda dos dados locais. [Referência oficial de instalação por ADB](https://developer.android.com/tools/adb#move)

Abra “Só relógio” no launcher do relógio. O seletor Samsung pode identificá-lo como WatchReserve. Depois configure Home, tile e complicação conforme o README. Na v0.2.2, os três acessos à confirmação Samsung foram comprovados no Watch8 documentado em [resultados](results.md). Outros modelos e firmwares precisam de validação própria; sem destino validado, o app usa as configurações de economia ou gerais. O painel do app fica em tile WatchReserve → Economia.

## Coleta privada e somente leitura

O script consulta somente propriedades de versão e resolução de intents públicos. Não instala apps, não abre telas, não escreve configurações, não concede permissões e não executa `root`. Ele pode iniciar o servidor ADB local se este ainda não estiver rodando.

```powershell
.\scripts\Collect-WatchDiagnostics.ps1 -Serial $watchSerial
```

Se houver um único aparelho conectado, `-Serial` pode ser omitido. Com mais de um aparelho, a seleção explícita é obrigatória. Para indicar um SDK sem alterar o `PATH`:

```powershell
.\scripts\Collect-WatchDiagnostics.ps1 `
    -AdbPath 'C:\CAMINHO_LOCAL\platform-tools\adb.exe' `
    -Serial $watchSerial
```

O resultado fica em `%LOCALAPPDATA%\WatchReserveDiagnostics\`, em arquivos com prefixo `watchreserve-`, fora do Git. O diretório é restrito ao usuário atual e ao SYSTEM; a coleta aborta se essa proteção não puder ser aplicada. O console informa somente o caminho, nunca o relatório bruto. Relatórios da v0.1.0 permanecem no diretório antigo `Watch8BatteryDiagnostics`; não são movidos nem publicados automaticamente.

O padrão não inclui serial, fingerprint nem dumps de atividades. Os dados completos de firmware exigem escolha explícita:

```powershell
.\scripts\Collect-WatchDiagnostics.ps1 `
    -Serial $watchSerial -IncludePrivateDetails
```

Para investigar a tela Samsung, abra-a manualmente no relógio e solicite uma captura local da pilha de atividades:

```powershell
.\scripts\Collect-WatchDiagnostics.ps1 `
    -Serial $watchSerial -IncludePrivateDetails -CaptureActivities
```

Esse último arquivo pode conter telas recentes e outros dados sensíveis. Não faça upload do diretório, não copie dumps para issues e revise qualquer trecho antes de compartilhar. O script não coleta pacotes indiscriminadamente nem captura telas.

## Coleta privada de energia e alarmes

A investigação de economia adicional usa uma opção separada, que exige serial explícito,
consentimento privado e confirmação do recurso Android de relógio:

```powershell
.\scripts\Collect-WatchDiagnostics.ps1 `
    -Serial $watchSerial -IncludePrivateDetails -CapturePower
```

As consultas incluem política de energia, DeviceIdle, Batterystats desde a carga, alarmes,
jobs, configuração de processos/freezer, dois pacotes Samsung específicos e chaves de
configuração relacionadas à investigação. O relatório pode conter apps, horários e títulos
de alarmes; fica no mesmo diretório privado com ACL restrita, sem conteúdo bruto no console.
Cada consulta de serviço usa timeout de dez segundos e falhas são registradas separadamente,
preservando os demais resultados. Ausência de informação não é tratada como consumo zero.

Não há reset de estatísticas, simulação de bateria, mudança de Doze, AppOps, pacote ou
permissão. Usa `batterystats --charged`; não usa `--checkin`, cuja implementação AOSP pode
consumir o arquivo histórico de check-in. A coleta acorda CPU/serviços e deve ser feita fora
da janela de autonomia, com depuração desligada entre as leituras. As consultas são
sequenciais; não representam uma amostra atômica de todos os serviços.

No Watch8 do usuário, a economia Samsung desliga Wi-Fi e impede a conexão ADB sem fio durante
esse uso. Faça a primeira leitura antes de ativá-la e a segunda depois do intervalo, após
desativá-la para reconectar. Registre duração e transições; os contadores incluem essa
sobrecarga e a segunda leitura mostra a política já fora da economia. As leituras não
comprovam o estado efetivo durante o intervalo. Não altere a economia para forçar Wi-Fi
ligado na referência. Uma coleta local pelo APK, com exportação posterior, ainda precisa
ser implementada e ter suas permissões comprovadas no firmware.

O pacote de alarme consultado é o observado na investigação Samsung, não uma identificação
universal do app escolhido pelo usuário. Confirmar qual alarme é usado antes de planejar
qualquer restrição. Veja [achados, limites e próximos experimentos](referencias/wearos-profundo.md).

## Investigar “Só relógio”

1. Registre a versão do sistema de forma privada e a versão do pacote da activity candidata encontrado na pilha de atividades. Ele pode diferir do pacote das configurações gerais.
2. Conte as ações do melhor caminho nativo até a confirmação de “Só relógio”. Não ative o modo durante a investigação, a menos que deseje fazê-lo e saiba sair dele.
3. Com essa tela aberta manualmente, use a captura local opcional para encontrar a activity candidata.
4. Verifique se o componente é exportado e exige permissões; `dumpsys package NOME_CONFIRMADO_DO_PACOTE` pode ajudar, mas seu resultado também fica local.
5. Teste a activity pelo APK com permissões comuns. Um `adb shell am start` bem-sucedido usa privilégios do shell e não comprova acesso pelo app.
6. Só habilite uma integração específica após teste físico, mantendo confirmação nativa, fallback e invalidação quando firmware ou versão do pacote candidato mudar. A candidata deve pertencer a um pacote do sistema que atende às configurações gerais ou de economia; não aceite um componente arbitrário de outro app.

A coleta do Watch8 identificou `com.samsung.android.batterysavingsettings/.wom.WatchOnlyModeActivity` como candidata exportada. A v0.2.2 comprovou a abertura pelo próprio APK no firmware L325FXXS3AZF4, preservando a confirmação nativa. Isso não autoriza outros firmwares automaticamente. A descoberta do app também consulta `com.samsung.android.watch.ACTION_BATTERY`, pois neste firmware os intents Android apontam para o pacote geral de configurações. “Buscar tela Samsung” procura a candidata declarada por `ACTION_WATCH_ONLY`; o filtro de pacote do sistema, o teste de abertura e a confirmação manual permanecem obrigatórios.

A resolução de `android.settings.BATTERY_SAVER_SETTINGS` prova somente que o sistema declara um destino para esse intent; seu comportamento e a economia de ações exigem teste. [Contrato público de `ACTION_BATTERY_SAVER_SETTINGS`](https://developer.android.com/reference/android/provider/Settings#ACTION_BATTERY_SAVER_SETTINGS)

## Encerrar

```powershell
adb disconnect $watchSerial
```

Desative também a depuração sem fio no relógio. Desconectar o cliente não garante que a depuração tenha sido desativada. Não são necessários `pm grant`, alteração de `low_power`, remoção de pacotes nem comandos de debloat para usar a prévia.
