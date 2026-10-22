# Pressão longa em Voltar — remapeamento no Galaxy Watch

Pesquisa em **22/10/2026**, solicitada pelo usuário para acrescentar funcionalidade ao
WatchReserve. Escopo: identificar solução pronta e avaliar integração, permissões, reversão
e custo energético. Sem instalar APK, conectar o Watch8 ou mudar sua configuração nesta etapa.

## Recomendação

**Testar primeiro o Wear Button Remapper, de Malcolm Bryant/Freepoc.** O autor o apresenta
como evolução do SPayRemap, que remapeia a pressão longa para outro app. O site oferece
instalação e demonstrações para Galaxy Watch. A solução pronta atende ao pedido de escolher
um app; não há necessidade demonstrada de duplicá-la agora. [Página oficial][freepoc],
[demonstração Samsung][video-samsung].

Uma integração futura ganha utilidade se permitir ações próprias do WatchReserve com
configuração simples e acesso mais limitado. Deve ser opcional e separada do aceite do modo
Relógio + alarmes. Remapeamento por acessibilidade **não exige Shizuku/Shevery em execução**;
controle de pacotes no preparo, quando necessário, tem outro nível de permissão.

## Soluções examinadas

| Solução | Evidência | Decisão para o projeto |
|---|---|---|
| SPayRemap / Wear Button Remapper | Freepoc indica WBR como sucessor melhorado; APK WBR examinado é 1.06. Permite escolher app. Código-fonte não foi obtido nesta pesquisa. | Primeira opção para testar uso real. Não declarar baixo consumo sem medir nem copiar implementação sem licença identificada. |
| Galaxy Watch Google Wallet Remap, pc386 | Fonte recebe `KEYCODE_STEM_PRIMARY` por acessibilidade, com callback temporário durante a pressão; abre Google Wallet e evita interceptação no Health. Autor declara teste em Ultra 2025, não Watch8. [Código][wallet-code], [configuração][wallet-config], [README][wallet-readme]. | Boa referência técnica de abordagem por eventos. Destino fixo Wallet e ausência de licença identificada na árvore consultada impedem tratá-lo como biblioteca pronta para incorporar. |
| GW4Remap, BigBoot | Serviço lê `logcat` continuamente para reconhecer pressão longa. Autor encerrou manutenção em 27/05/2024. [Código][gw4-code], [aviso][gw4-status]. | Não usar como base da meta de bateria. Sua licença Apache 2.0 não elimina custo, dependência de logs e necessidade de adaptação ao firmware. |

O remapeador pc386 orienta desativar Samsung Pay porque o handler Samsung pode abri-lo mesmo
com o evento interceptado. Essa é uma condição daquele procedimento, não prova de que todo
remapeamento exige desativação. Não aplicar automaticamente ao Watch8. Seu toque curto chama
a ação global Voltar: isso também não prova preservação de uma configuração nativa diferente,
como Recentes. [Procedimento e limites do candidato][wallet-readme].

## Inspeção estática do APK Freepoc

APK obtido do [endereço HTTPS ligado pela página oficial][wbr-apk], sem executá-lo:

- Pacote `org.freepoc.wearbuttonremapper`, versão **1.06**, versionCode **7**.
- 6.788.178 bytes; SHA-256
  `c55801d4612c8ed6552165699f04537c21b9b15677ca00a362cb87621d8bcbd2`.
- Certificado observado, SHA-256
  `2c4acfa15798b9955c0cdf526294a627fdd2b4e4cd336a5e5255b1436c0e8cb2`;
  `apksigner verify` aprovado. Identifica o artefato examinado, sem pin independente anterior.
- API mínima 30/alvo 34. Manifesto declara `WAKE_LOCK`, vibração e consulta ampla de pacotes;
  não declara `READ_LOGS`, `INTERNET` ou `WRITE_SECURE_SETTINGS`.
- Serviço `MyAccessibilityService`; configuração declara todos os tipos de evento,
  possibilidade de obter conteúdo das janelas e de solicitar filtragem de teclas.

Manifesto e configuração lidos com `aapt2` 36.0.0; verificação de assinatura com JDK 17.
O APK fica fora do Git, no diretório local de pesquisa. O catálogo ainda mostra 1.05, enquanto
o [anúncio oficial de 03/01/2026][wbr-106] e o APK obtido mostram 1.06; usar a versão do
artefato examinado, sem presumir que o texto do catálogo esteja atualizado.

Declarar `WAKE_LOCK` não demonstra que o app o mantém adquirido. Acesso amplo a eventos também
não quantifica CPU/bateria. Não foi feita auditoria do bytecode, teste emulado ou medição
desse APK. A inspeção de manifesto não comprova ausência de timers nem compatibilidade Samsung.

## Como integrar se o teste justificar

A API Android oferece `AccessibilityService.onKeyEvent()` e capacidade de filtrar teclas.
Isso sustenta um protótipo por eventos, com autorização explícita do usuário, sem ler logs,
sensores ou rede. `onKeyEvent` exige fluxo consistente de eventos de pressionar/soltar; não
é uma API para alterar de forma persistente o vínculo Samsung. [Contrato Android][key-api],
[capacidade de filtragem][key-flags].

Para um recurso próprio, propor:

1. Ação configurável: confirmação de Só relógio, painel Economia, alarmes ou app escolhido.
   Ações de perfil só aparecerão depois da validação do respectivo controle. A pressão longa
   abre a ação escolhida; não automatiza a confirmação Samsung nem uma alteração privilegiada.
2. Serviço opcional por eventos, desativado inicialmente, sem leitura de conteúdo de janelas,
   polling, `logcat`, wake lock ou temporizador ocioso. Pedir apenas os eventos necessários;
   um prazo durante a pressão não é monitoramento periódico. Medir seu custo residente.
3. Identificar o código real do botão no Watch8, em vez de presumir que a entrega de
   `KEYCODE_STEM_PRIMARY` por outro relógio se repete. Preservar toque curto e configuração
   nativa; se não for possível, explicitar a limitação e manter recurso indisponível por padrão.
4. Usar intents explícitos para as rotas próprias. O manifesto atual já exporta
   `ShortcutActivity` e `PanelActivity`: um seletor apenas de apps abre a entrada Só relógio;
   abrir Economia exige que o remapeador aceite activity específica ou integração adicional.
   Não adicionar outro launcher para contornar isso nem alterar o duplo clique Home existente.
5. Testar o handler Samsung: se houver abertura concorrente, avaliar redirecionamento pela
   janela do Pay versus desativação temporária do pacote. O primeiro pode mostrar a carteira
   por um instante e afetar sua abertura manual; o segundo impede seu uso enquanto desativado.
   Registrar essas diferenças antes de escolher a abordagem. Não desativar Bixby ou outro
   pacote apenas por instrução de um remapeador antigo.
6. Se desativação for necessária, identificar pacote/usuário e estado original exato antes
   da escrita. Integrar ao registro do acesso opcional, fora da sessão do perfil econômico;
   respeitar conflitos. Ao remover o recurso, desabilitar o serviço antes de restaurar o
   pacote ao estado original. “Habilitar” não equivale sempre a restaurar `DEFAULT`.

Não foi encontrado nesta pesquisa um comando público comprovado, disponível ao app comum ou
shell no Watch8, que troque esse vínculo definitivamente sem serviço residente. Políticas de
[remapeamento Knox][knox] pertencem à gestão empresarial e não comprovam acesso para o app
pessoal; não introduzir Device Owner ou licenciamento Knox para resolver este atalho.

## Teste que decide entre terceiro e integração

Usar um único remapeador ativo. Registrar versão/configuração, permissões e estado Samsung
antes do preparo. Testar pressão curta, longa e sustentada, repetições rápidas, Home intacto,
retorno e ausência de dupla abertura. Testar mostrador, app aberto, tela apagada/ambiente,
economia ligada, reboot após desbloqueio e morte do processo. Não prometer que um serviço
de acessibilidade antecipe o despertar da tela. Se o primeiro gesto só acordar, registrar.

Incluir alarme tocando/adiar/dispensar, Health/treino, bloqueio e configurações, sem interceptar
ações indispensáveis. Verificar negação/revogação de acessibilidade, interrupção no meio de
uma pressão e recuperação sem ficar abrindo apps. Revisar a UI em fonte 1,0/1,3 e tela circular.

Comparar remapeador desligado/ligado em janelas pareadas, com a mesma configuração e frequência
de uso. Se Pay precisar ficar desativado, mantê-lo igual nos dois braços para isolar o custo
do serviço; avaliar à parte o efeito líquido da configuração inteira. Registrar o remapeador
como variável adicional no protocolo R/P/E, sem mudar isso no meio das janelas de autonomia.

**Decisão atual:** solução pronta primeiro; integração própria condicionada a limitação real
ou vantagem comprovada de acesso, permissões ou consumo. Não bloquear o marco de economia
pela função extra. Ainda falta prova física no firmware L325FXXS3AZF4; nenhum ganho declarado.

## Fontes fixadas e verificação

Fonte pc386: `bf8747e24e15e134d90e0b7c01c09ef6a33e4804`, release v1.1.
Fonte BigBoot: `1aeb56267fbe4219b8c4296b7712a414737884f6`. Também consultado o fork
philippdormann/GalaxyWatchRemap (`c9ab7e3afe22a1e5a271922f06c11a042f34716f`), sem adotá-lo.
Nenhum trecho desses apps foi incorporado ao projeto.

Entrega documental: links locais, UTF-8, diff/status/histórico e bug sweep revisados. Sem
mudança no código/build do WatchReserve; testes Android não repetidos. Inspeção estática
do APK de terceiro não substitui revisão visual ou teste físico.

[freepoc]: https://freepoc.org/downloads/
[video-samsung]: https://www.youtube.com/watch?v=dsD8P3SjMgY
[wbr-apk]: https://www.freepoc.org/download/WearButtonRemapper_wear.apk
[wbr-106]: https://freepoc.org/2026/01/03/updated-wear-button-remapper-1-06/
[wallet-readme]: https://github.com/pc386/galaxy-watch-google-wallet-remap/blob/bf8747e24e15e134d90e0b7c01c09ef6a33e4804/README.md
[wallet-code]: https://github.com/pc386/galaxy-watch-google-wallet-remap/blob/bf8747e24e15e134d90e0b7c01c09ef6a33e4804/app/src/main/kotlin/com/galaxywatch/googlewalletremap/WalletRemapAccessibilityService.kt
[wallet-config]: https://github.com/pc386/galaxy-watch-google-wallet-remap/blob/bf8747e24e15e134d90e0b7c01c09ef6a33e4804/app/src/main/res/xml/accessibility_service_config.xml
[gw4-code]: https://github.com/BigBoot/GW4Remap/blob/1aeb56267fbe4219b8c4296b7712a414737884f6/app/src/main/kotlin/de/bigboot/watch4payswitch/AccessibilityService.kt
[gw4-status]: https://github.com/BigBoot/GW4Remap/issues/24
[key-api]: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)
[key-flags]: https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#FLAG_REQUEST_FILTER_KEY_EVENTS
[knox]: https://docs.samsungknox.com/admin/knox-manage/configure/profile/configure-profile-policies/wear-os-policies/
