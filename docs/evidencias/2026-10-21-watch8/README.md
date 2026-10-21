# Watch8 — descoberta e abertura pelo APK em 21/10/2026

Primeira etapa: coleta somente leitura pelo PC. O usuário abriu manualmente a confirmação de “Só relógio”.
Nenhum APK foi instalado, permissão concedida, ajuste alterado, componente iniciado pelo shell
ou modo ativado durante esta coleta. Pareamento e conexão ADB foram concluídos.

## Ambiente técnico

| Campo | Valor observado |
|---|---|
| Modelo | Samsung SM-L325F; Watch8 LTE de 40 mm informado pelo usuário |
| Android / API | 16 / 36 |
| Firmware incremental | L325FXXS3AZF4 |
| Patch de segurança | 2026-07-05 |
| Configurações gerais | `com.google.android.apps.wearable.settings`, versão 16, code 200 |
| Bateria Samsung, versão ativa | `com.samsung.android.batterysavingsettings`, 4.0.01.1, code 400101000 |
| Bateria Samsung, estado do pacote | App do sistema atualizado |
| One UI Watch / Wear OS, versões comerciais | Não confirmadas nesta coleta; não inferidas da API |

## Candidata encontrada

- Componente: `com.samsung.android.batterysavingsettings/.wom.WatchOnlyModeActivity`.
- Ação declarada: `com.samsung.android.watch.ACTION_WATCH_ONLY`.
- Manifesto da versão ativa: atividade exportada, sem permissão declarada na atividade ou na
  aplicação. Isso permitiu investigar acesso por app comum; a abertura foi comprovada na
  etapa seguinte, sem inferir o resultado somente do manifesto.
- A pilha de atividades identificou esse componente enquanto o usuário estava na confirmação.
  Uma captura local privada mostrou o botão nativo `Turn on`, sem pressioná-lo.
- A resolução de `ACTION_SETTINGS` e `ACTION_BATTERY_SAVER_SETTINGS` aponta para
  `com.samsung.android.clockwork.settings.StMainSettingsActivity` no pacote geral.
- A ação Samsung `com.samsung.android.watch.ACTION_BATTERY` aponta para
  `com.samsung.android.batterysavingsettings/.battery.view.BatteryMainActivity`.

A v0.2.1 só descobre provedores das duas ações Android. Ela rejeitaria o pacote separado de
bateria na validação da candidata. A v0.2.2 acrescenta a consulta da ação Samsung de bateria
e sua visibilidade no manifesto, preservando as demais verificações. “Buscar tela Samsung”
preenche a candidata declarada pelo firmware; não a salva nem a valida automaticamente.

## Prova pelo próprio APK

Após a coleta, o usuário autorizou explicitamente instalar o APK e testar a abertura, sem
ativar o modo ou conceder permissões avançadas. Foi instalado o APK release assinado:

- Versão 0.2.2, versionCode 4; 2.984.649 bytes.
- SHA-256 final: `79f2d524ec1360c1a82aa67db9b82d4534e69980f7ca0ca2b747fc650ef855cf`.
- Mesmo application ID e certificado persistente das releases anteriores.

O botão de busca encontrou a candidata pelo gerenciador de pacotes do app. A candidata foi
salva e “Testar abertura” acionado na interface do WatchReserve. A captura local mostrou
`Watch only`, instruções nativas e o botão de ativação. A pilha Android registrou
`launchedFromPackage=dev.brenopoubel.watch8battery`; o UID chamador correspondeu ao UID comum
do pacote instalado, sem instrumentação nem identidade shell adotada pelo app.

O shell iniciou somente o painel e enviou interações de interface; a operação que abriu a
atividade Samsung saiu do app de produção. Não foi usado `am start` com componente Samsung
para comprovar a integração. Nenhuma permissão avançada foi concedida.

Após retornar, o app reconheceu o retorno do teste. A inspeção visual da captura pelo agente
confirmou a tela correta e a confirmação foi registrada na interface, vinculada ao firmware e
à versão do pacote. A entrada compartilhada `ShortcutActivity` abriu novamente a confirmação.
Esse último teste iniciou a atividade do WatchReserve pelo shell e **não substitui** o teste
real de Home, tile ou complicação. Em nenhum momento o botão nativo `Turn on` foi pressionado.

## Home e comparação com o caminho nativo

O primeiro teste físico do usuário retomou “Validar atalho”. A captura dos lançamentos
mostrou que a Samsung chamou `ShortcutActivity` com flags `0x10200000` (NEW_TASK e
RESET_TASK_IF_NEEDED), mas o Android trouxe o painel existente para frente, sem executar a
entrada: as duas atividades compartilhavam afinidade de tarefa. Um lançamento sem esses
flags não reproduzia a falha, por isso o teste inicial da entrada compartilhada era insuficiente.

A afinidade de `ShortcutActivity` foi separada da do painel, preservando os componentes e a
configuração já escolhida no Home. A atualização assinada foi instalada sem apagar os dados.
Com o painel aberto, a repetição dos flags reais abriu a confirmação Samsung. Depois, partindo
do mostrador, o usuário confirmou que **o duplo clique físico abre diretamente Só relógio**.

Referência nativa informada pelo usuário: abrir o painel rápido → pressionar o ícone de
economia → rolar até Só relógio → tocar em Só relógio. O toque seguinte para ativar fica fora
da comparação até a confirmação. O atalho chega à mesma confirmação com um duplo clique,
isto é, um gesto com dois pressionamentos do botão. Houve redução de navegação nesse acesso;
não foram cronometradas latências nem feita uma contagem controlada de todos os movimentos.

O requisito de utilidade de pelo menos uma superfície foi observado por relato físico do
usuário, desde o mostrador. Depois, o usuário adicionou o tile e a complicação a um campo
compatível e confirmou que **ambos também abrem a confirmação Samsung**. Os três acessos
foram observados em uso normal; reinício, morte do processo e parada forçada ainda precisam
de verificação própria. O [AVD](../2026-10-21-wearos5/README.md) passou
na nova regressão com os flags do Home e painel aberto, nas duas escalas de fonte.

O teclado Samsung mostrou o texto enviado por ADB no seu editor de tela cheia, sem preencher
o campo Compose ao concluir. Isso não comprova falha da digitação manual. A busca automática
permitiu configurar e testar sem depender desse caminho; a entrada manual permanece como
alternativa ainda não validada fisicamente.

Acabamento físico observado: painel circular legível, busca com mensagem visível, candidata
preenchida, botão de teste habilitado após salvar, mensagem de retorno e confirmação final.
Os itens atravessam as bordas durante a rolagem, como no AVD; permanecem alcançáveis. Não foram
exercitados fonte ampliada, leitor de tela ou bezel nesta sessão.

O receiver Samsung de ativação exige `com.samsung.permission.CHANGE_WATCH_ONLY_MODE`,
declarada com proteção `signature` (valor 2). Esse mecanismo não é uma API comum concedida
por `WRITE_SECURE_SETTINGS`. Não foi enviado qualquer broadcast de ativação.
[Contrato Android de permissão por assinatura](https://developer.android.com/reference/android/content/pm/PermissionInfo#PROTECTION_SIGNATURE).

## Controles avançados — observação inicial

O recurso `android:bool/config_batterySaverSupported` retornou `true`, diferente do AVD usado
no laboratório. O serviço de energia informou economia desligada no momento da coleta;
`low_power`, `restricted_device_performance`, `low_power_back_data_off` e
`sm_connectivity_disable` estavam em `0`; `battery_saver_constants` estava ausente (`null`).
Não houve escrita dessas chaves. O estado observado não constitui validação de aplicação,
restauração, interface Samsung ou consumo. O relato anterior de uso habitual com economia
ligada continua sendo uma referência de uso, sem substituir esta observação pontual.

## O que permanece pendente

1. Completar a comparação de navegação de tile/complicação; Home já reduziu navegação na
   comparação relatada pelo usuário, sem medição de latência.
2. Exercitar os três acessos após reinício, morte do processo e parada forçada; revisar funcionalidade e
   acabamento na tela física.
3. Investigar controles separadamente, com aplicação e restauração comprovadas, antes de
   disponibilizá-los no produto. Executar o protocolo de autonomia em outra etapa.

Dumps, APKs Samsung, captura de tela, fingerprint, serial, rede e código de pareamento ficam
somente no diretório privado local, fora do repositório. Esta página é um resumo revisado;
não publica material binário Samsung ou identificadores da unidade.
