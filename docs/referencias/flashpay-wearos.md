# Referência técnica: Flashpay para Wear OS

Análise em **19/10/2026**, a pedido do usuário. Referência: [TransitNow/flashpay-wearos](https://github.com/TransitNow/flashpay-wearos/tree/9451df887b62e8b172ad930f8ebeb189df02494b), commit `9451df887b62e8b172ad930f8ebeb189df02494b`, comparado ao WatchReserve v0.2.0.

**Resultado:** útil para o acesso rápido e o acabamento em telas circulares. O código examinado não fornece um destino para “Só relógio” nem controles avançados da economia do Wear OS. Nenhum código, ativo ou script externo foi incorporado ou executado.

## Evidência e limites

Foram lidos manifesto, MainActivity, AppLauncher, MenuActivity, RoundSafeColumn, DecisionLog, FlashlightActivity, temas, build, README e instruções ADB. Também foi inspecionada a [captura publicada do menu](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/play-assets/screenshots/menu.png): duas ações grandes, fundo preto e larguras distintas para acomodar a tela circular. Essa captura é material do projeto de referência; não comprova execução no relógio do usuário.

A árvore consultada estava completa e não continha arquivo de licença. A API do GitHub retornou `licenseInfo: null`, e o README examinado não declarou licença. O estudo usa observações e padrões; reaproveitamento de arquivos exigiria esclarecer a licença. A [documentação do GitHub](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository) explica a diferença entre visibilidade pública e autorização de reutilização.

## Comparação com o WatchReserve

| Aspecto observado no Flashpay | Aplicação ao WatchReserve | Limite ou verificação necessária |
|---|---|---|
| Menu separado do acesso rápido; uma `activity-alias` expõe o atalho no launcher. | É uma alternativa para oferecer acesso direto ao painel e uma entrada distinta para Home. A v0.2.0 já usa uma atividade comum para os três atalhos. | O exemplo tem duas entradas no launcher. Não altera a decisão atual de entrada única nem comprova seleção em todo firmware Samsung. |
| Tema translúcido para a atividade intermediária e tema próprio para o menu. | Investigar redução de telas intermediárias ou flashes na abertura do nosso atalho. | Medir antes/depois; manter o fallback visível e acessível. Não corrige o atraso nativo para mostrar a hora em “Só relógio”. |
| Layout adapta a largura de cada linha à área disponível no círculo. | Reforça a revisão do painel em telas pequenas e com fonte ampliada. | A implementação externa atende a poucas linhas estáticas. Nosso painel longo exige solução própria para rolagem e texto multilinha. |
| Na ausência da Carteira, abre um menu em vez de deixar uma atividade vazia. | Confirma a importância do fallback com orientação. O nosso roteador já trata destino ausente ou bloqueado. | Preservar os tratamentos de `ActivityNotFoundException` e `SecurityException` existentes. |
| Registra localmente a decisão de cada acionamento. | Se necessário, melhorar o diagnóstico manual de “Testar abertura”, com resultado e duração do despacho. | Preferir opt-in, dados mínimos e limite de armazenamento. Despachar um intent não comprova que a tela de destino foi desenhada. |

Fontes do acesso rápido: [manifesto](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/AndroidManifest.xml), [temas](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/res/values/styles.xml) e [tema para API 31+](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/res/values-v31/styles.xml). Fontes de interface e diagnóstico: [RoundSafeColumn](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/java/com/jsyntax/nowtap/RoundSafeColumn.kt), [MenuActivity](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/java/com/jsyntax/nowtap/MenuActivity.kt) e [DecisionLog](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/java/com/jsyntax/nowtap/DecisionLog.kt).

## O que não demonstra economia avançada

Na [MainActivity](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/java/com/jsyntax/nowtap/MainActivity.kt), o sensor de luz decide entre Carteira e lanterna. Há espera de 200 ms após a primeira leitura clara e timeout de 1.500 ms sem amostra. O listener é removido após a decisão e na destruição. Isso é uso pontual de sensor, não prova de início instantâneo ou de consumo menor. Nosso atalho manual não precisa dessa decisão por luminosidade.

Na [FlashlightActivity](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/src/main/java/com/jsyntax/nowtap/FlashlightActivity.kt), `screenBrightness` modifica a janela da própria atividade. Não muda o brilho global, o mostrador ou outros apps. A lanterna mantém a tela acesa e atualiza uma contagem enquanto está aberta; esses mecanismos atendem à lanterna e não têm função na nossa central de economia. [Contrato de brilho por janela](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#screenBrightness).

O [build](https://github.com/TransitNow/flashpay-wearos/blob/9451df887b62e8b172ad930f8ebeb189df02494b/app/build.gradle) não declara dependências de AndroidX/Compose e usa Views da plataforma. Isso, isoladamente, não demonstra vantagem de autonomia nem justifica reescrever nossa interface. Comparações de latência ou consumo exigiriam a mesma rotina e aparelho; o código da branch também não deve ser confundido com APKs históricos de outras versões.

Não foram encontrados providers de tile/complicação, implementação de economia global, `WRITE_SECURE_SETTINGS` ou destino Samsung de “Só relógio” nos componentes examinados. O filtro de botão alternativo destinado ao TicWatch não deve ser presumido como compatível com Samsung.

Um alias aponta para atividade do próprio app; não concede acesso a uma atividade Samsung bloqueada. A descoberta continua sujeita à exportação e às permissões do destino. [Contrato de activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element), [exportação de componentes](https://developer.android.com/privacy-and-security/risks/android-exported).

## Prioridades para uma próxima implementação

1. **Interface circular:** avaliar os componentes próprios de listas do Compose para Wear OS antes de criar geometria personalizada. Verificar compatibilidade com as dependências fixadas. Revisar extremos da rolagem, textos multilinha, teclado, bezel e fonte ampliada. A [orientação oficial para listas](https://developer.android.com/training/wearables/compose/lists) contempla telas redondas; não basta transplantar o layout estático do Flashpay.
2. **Abertura do atalho:** comparar início frio e processo já aberto; registrar telas intermediárias, tempo até a confirmação e ações. Uma eventual mudança de tema fica restrita à entrada rápida, com teste do fallback, reinício, processo encerrado e acionamentos repetidos. Medir o despacho separadamente da apresentação do destino.
3. **Acesso ao painel:** investigar menu e atalho separados somente se trouxerem benefício de uso. Preservar a identidade dos componentes atuais e testar a seleção no Home. A v0.2.0 continua com uma entrada no launcher e painel pelo tile.
4. **Diagnóstico:** acrescentar registro apenas se a investigação física demonstrar necessidade. Manter coleta manual, sem sensores, rede ou gravação automática em cada uso normal.

O [roteiro funcional e de acabamento](../validation.md) continua obrigatório. A prova Samsung e a comparação de autonomia permanecem etapas próprias; nenhuma delas foi satisfeita por este estudo.

## Validação desta análise

Inspeção de código e de uma captura externa; sem instalação, compilação ou execução do Flashpay. Alterações no WatchReserve limitadas a Markdown. Build, lint Android e testes JVM não foram repetidos porque não houve mudança no APK, scripts ou dependências. Links locais e UTF-8 conferidos; fatos revisados contra as fontes fixadas. Bug sweep: sem achados bloqueantes ou altos; permanecem pendentes as medições e a validação física descritas acima.
