# Instalação e investigação por ADB

O app funciona desconectado. ADB é usado para instalar e investigar o comportamento do firmware, não para manter a economia funcionando. A depuração deve ser desligada durante as medições de autonomia.

A instalação e os testes físicos foram adiados a pedido do usuário. Os passos abaixo ficam disponíveis para quando for conveniente; não é necessário conectar o relógio nesta entrega.

## Opção A: instalar pelo celular com Bugjaeger

Baixe o APK de [Releases](https://github.com/brenopoubel0324/Watch8-Battery/releases) no celular e deixe celular e relógio na mesma rede Wi-Fi. No relógio, habilite as opções do desenvolvedor e a depuração sem fio. No Bugjaeger, use o pareamento com o código, IP e porta mostrados pelo relógio; depois conecte à porta da tela principal de depuração, que pode ser diferente da porta de pareamento.

Com o relógio escolhido no Bugjaeger, abra a aba de pacotes e use a ação de instalar APK para selecionar o arquivo baixado. Os nomes e posições podem variar conforme a versão do app. Confirme o sucesso e procure “Só relógio” no launcher do relógio; o nome do app é Watch8 Battery. Este fluxo é baseado na documentação do desenvolvedor do Bugjaeger; ainda não foi validado neste Watch8. [Pareamento de relógio](https://sisik.eu/blog/android/bugjaeger/connect-watch), [instalação de APK](https://sisik.eu/blog/android/bugjaeger/sideload-apk-files)

Ao terminar, desconecte o Bugjaeger e desligue a depuração sem fio no relógio. Não são necessários comandos para conceder permissões ou modificar configurações globais. A coleta por PowerShell abaixo é uma alternativa para investigação futura pelo PC.

## Opção B: preparar e parear pelo PC

Instale Android SDK Platform Tools. Se `adb` não estiver no `PATH`, use seu caminho completo ou adicione a pasta `platform-tools` somente à sessão atual do PowerShell.

No relógio, habilite as opções do desenvolvedor pelo número da versão do software e abra a depuração sem fio. Os rótulos e o caminho podem mudar conforme o firmware. Use a opção de parear novo dispositivo; deixe o código no relógio e responda ao prompt local de `adb pair`:

```powershell
adb pair IP_DO_RELOGIO:PORTA_DE_PAREAMENTO
adb connect IP_DO_RELOGIO:PORTA_DE_CONEXAO
adb devices
```

A porta da conexão é a mostrada na tela principal de depuração sem fio; ela pode ser diferente da porta de pareamento. PC e relógio devem estar na mesma rede compatível. IP, portas, códigos e a saída de `adb devices` são dados locais; não os publique. [Guia oficial de depuração por Wi-Fi no Wear OS](https://developer.android.com/training/wearables/get-started/debug-wifi)

## Instalar um APK

Escolha explicitamente o identificador exibido em `adb devices`. Não use os valores ilustrativos abaixo literalmente.

```powershell
$watchSerial = 'IDENTIFICADOR_LOCAL_DO_RELOGIO'
adb -s $watchSerial install -r '.\app\build\outputs\apk\debug\app-debug.apk'
```

Para uma release, use o caminho do APK baixado em Releases. Atualizações com `install -r` exigem assinatura compatível; debug e release podem ter chaves diferentes. Não desinstale apenas para contornar um erro sem considerar a perda dos dados locais. [Referência oficial de instalação por ADB](https://developer.android.com/tools/adb#move)

Abra “Só relógio” no launcher do relógio. O seletor Samsung pode identificá-lo como Watch8 Battery. Depois configure Home, tile e complicação conforme o README. A prévia pode abrir as configurações de economia ou as configurações gerais; ela não confirma que a tela Samsung de “Só relógio” aceita abertura direta.

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

O resultado fica em `%LOCALAPPDATA%\Watch8BatteryDiagnostics\`, fora do Git. O diretório é restrito ao usuário atual e ao SYSTEM; a coleta aborta se essa proteção não puder ser aplicada. O console informa somente o caminho, nunca o relatório bruto.

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

## Investigar “Só relógio”

1. Registre a versão do sistema de forma privada e a versão do pacote da activity candidata encontrado na pilha de atividades. Ele pode diferir do pacote das configurações gerais.
2. Conte as ações do melhor caminho nativo até a confirmação de “Só relógio”. Não ative o modo durante a investigação, a menos que deseje fazê-lo e saiba sair dele.
3. Com essa tela aberta manualmente, use a captura local opcional para encontrar a activity candidata.
4. Verifique se o componente é exportado e exige permissões; `dumpsys package NOME_CONFIRMADO_DO_PACOTE` pode ajudar, mas seu resultado também fica local.
5. Teste a activity pelo APK com permissões comuns. Um `adb shell am start` bem-sucedido usa privilégios do shell e não comprova acesso pelo app.
6. Só habilite uma integração específica após teste físico, mantendo confirmação nativa, fallback e invalidação quando firmware ou versão do pacote candidato mudar. A candidata deve pertencer a um pacote do sistema que atende às configurações gerais ou de economia; não aceite um componente arbitrário de outro app.

Não existe neste projeto uma activity Samsung presumida como válida. A resolução de `android.settings.BATTERY_SAVER_SETTINGS` prova somente que o sistema declara um destino para esse intent; seu comportamento e a economia de ações exigem teste. [Contrato público de `ACTION_BATTERY_SAVER_SETTINGS`](https://developer.android.com/reference/android/provider/Settings#ACTION_BATTERY_SAVER_SETTINGS)

## Encerrar

```powershell
adb disconnect $watchSerial
```

Desative também a depuração sem fio no relógio. Desconectar o cliente não garante que a depuração tenha sido desativada. Não são necessários `pm grant`, alteração de `low_power`, remoção de pacotes nem comandos de debloat para usar a prévia.
