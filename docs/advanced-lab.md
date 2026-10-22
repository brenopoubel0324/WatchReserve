# Diagnóstico local com ponte shell

O módulo `:advancedLab` é um APK de desenvolvimento separado, pacote
`dev.brenopoubel.watchreserve.advancedlab`, com launcher **WatchReserve Lab**.
Não faz parte da release do WatchReserve e não tem variante release. O app principal
continua sem dependência de Shizuku, permissões avançadas ou novos serviços.

Este laboratório **somente lê** o sistema. Ainda não congela apps, aplica economia,
altera rádios ou recupera uma ponte após reinício. Seu relatório ajuda a selecionar
controles para investigar; não demonstra ganho de bateria.

## Acesso e limites

É necessário instalar e iniciar a ponte **no relógio**, além de autorizar o cliente
pela interface do gerenciador. Uma ponte no celular não concede acesso ao relógio.
Não há concessão automática de autorização nem fallback para root.

O laboratório aceita uma única ponte conhecida instalada, com certificado e versão
verificados no código:

| Gerenciador | Build admitido |
|---|---|
| Shevery | `com.hamondev.shevery`, 14.4.0, código 44 |
| Shizuku original | `moe.shizuku.privileged.api`, 13.6.0.r1086.2650830c, código 1086 |

A integração usa a API/provider 13.1.5. O cliente exige UID shell 2000 e também
verifica seu próprio UID no serviço de leitura. Uma atualização do gerenciador
exige nova qualificação antes de atualizar a lista de confiança.

A autorização e as leituras foram comprovadas com o Shevery no AVD Wear OS 5/API 34.
**A autorização original do Shevery fica cortada em 192 dp circular.** A prova do
protocolo exigiu ampliar temporariamente a tela do emulador, depois restaurá-la.
Isso não comprova uma autorização utilizável no Galaxy Watch nem qualifica os
serviços, atualizações automáticas ou mecanismos de recuperação do gerenciador.
O APK original não é uma ponte Wear adaptada.

## O que a coleta faz

“Leitura rápida” consulta energia e processos. “Diagnóstico completo” acrescenta
política de repouso, estatísticas de bateria desde a carga, alarmes, jobs, serviços,
pacotes/UIDs, Bluetooth, Wi-Fi, sensores e as políticas de varredura Wi-Fi/BLE.
São treze consultas fixas; nenhum texto externo vira comando.

Cada consulta tem limite de dez segundos e armazena até 1 MiB, drenando stdout e
stderr juntos. O ZIP inclui um resumo por consulta: duração, bytes recebidos,
código de saída e estados como `ok`, `timeout`, `permission_denied`,
`service_unavailable`, `command_failed` ou `truncated`. Uma falha na gravação
aborta o arquivo. Não usa `batterystats --reset` nem `--checkin`.

`ok` significa que a consulta terminou, não ausência de consumo. Um valor `null`
de uma chave não prova que a função esteja desligada. Serviço ausente, permissão
negada e saída truncada não devem ser interpretados como atividade zero.
Processo em cache ou app nos recentes também não demonstra trabalho de CPU.

O UserService é criado apenas por uma ação manual e é removido ao concluir ou
cancelar. Cada operação tem uma identificação própria, para isolar respostas
atrasadas de coletas canceladas. `daemon(false)` permite à ponte encerrá-lo se o
cliente morrer. O processo principal da ponte pode continuar residente: o teste
de encerramento do helper não mede nem elimina o custo do gerenciador.

## Privacidade e recuperação

Os ZIPs ficam no armazenamento interno `noBackupFilesDir/diagnostics`. Não entram
em backup, Git, logs públicos nem upload automático. Podem conter horários de
alarmes, nomes de apps e informações de rede. **Não os publique sem revisão.**
Arquivos `.pending` não são apresentados como relatórios completos; uma morte
abrupta pode deixar um deles no armazenamento privado. Falhas inesperadas deixam
um `last-error.txt` privado para diagnóstico de desenvolvimento.

“Exportar relatório” usa o seletor de documentos do sistema. Se o firmware não
fornecer esse seletor, o arquivo permanece privado. Relatórios anteriores são
mantidos; limpar os dados deste laboratório os exclui. Isso não restaura nenhuma
configuração do relógio ou da ponte.

## Compilar e testar

Use o JDK 17 e SDK 36 do projeto:

```powershell
.\gradlew.bat --no-daemon `
    :advancedLab:assembleDebug :advancedLab:assembleDebugAndroidTest `
    :advancedLab:testDebugUnitTest :advancedLab:lintDebug
```

Com a ponte já iniciada e o cliente autorizado no AVD reservado:

```powershell
.\scripts\Test-AdvancedLab.ps1 `
    -Serial emulator-5580 `
    -AdbPath "$env:ANDROID_HOME\platform-tools\adb.exe" `
    -OutputDirectory "$env:LOCALAPPDATA\WatchReserveLabReports\advanced"
```

O roteiro exige PowerShell 7.4+, verifica o nome do AVD e a tela Wear circular,
instala somente os dois APKs de laboratório e executa testes em fontes 1,0 e 1,3.
Restaura a escala original no `finally`. A ponte e sua autorização precisam ser
preparadas separadamente; o roteiro não concede permissões nem inicia o servidor.
As capturas e resultados são gravados fora do repositório.

A instrumentação cobre leitura rápida/completa por shell, isolamento do cliente
comum, cancelamento durante conexão, recriação da tela e encerramento do helper.
Ela não comprova sensores Samsung, alarmes físicos, autonomia, comportamento após
reboot ou compatibilidade do gerenciador em outros firmwares. A CI compila a
instrumentação e roda testes JVM/lint; a integração exige o AVD local.

## Próximo controle a qualificar

O congelamento por desativação temporária de pacote é um candidato para o modo
manual. Ele precisará registrar o estado anterior exato, conferir aplicação e
restauração, preservar conflitos e excluir alarme, componentes essenciais e ponte.
Suspensão, parada forçada e desativação são ações diferentes. O [Hail](https://github.com/aistra0528/Hail#freeze)
documenta essas diferenças e o suporte sem root por Shizuku. Nenhum desses
controles está habilitado neste APK; o ganho terá de ser medido no relógio.
