# Regressão de Home e acabamento — Wear OS 5

Execução em 21/10/2026 no AVD reservado `WatchReserve_Wear5_Small_Round`, API 34,
circular 384×384 px, com a compilação debug da v0.2.2. Não representa firmware Samsung.

[Resumo e hash do APK debug](summary.json). Sete testes instrumentados aprovados em cada
escala de fonte: [1,0](instrumentation-font-1_0.txt) e [1,3](instrumentation-font-1_3.txt).
São sete casos, executados duas vezes, além dos 23 testes JVM do app.

O caso novo abre o painel em “Validar atalho” e inicia a entrada compartilhada com
`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`, os flags observados no Home
Samsung. Um monitor exige que `ShortcutActivity` seja criada; apenas retomar o painel não
passa. A afinidade do atalho foi separada após a falha física reportada pelo usuário.

| Estado inspecionado | Fonte 1,0 | Fonte 1,3 |
|---|---|---|
| Painel inicial | [Captura](home-font-1_0.png) | [Captura](home-font-1_3.png) |
| Medição manual | [Captura](measurement-font-1_0.png) | [Captura](measurement-font-1_3.png) |
| Componente inválido | [Captura](destination-error-font-1_0.png) | [Captura](destination-error-font-1_3.png) |
| Rolagem rotativa | [Captura](rotary-scroll-font-1_0.png) | [Captura](rotary-scroll-font-1_3.png) |

Oito capturas revisadas. Ação principal e início de medição permanecem visíveis; o botão de
busca Samsung aparece após a mensagem de erro e continua alcançável por rolagem. Na fonte
ampliada, mensagens longas exigem rolar; os itens atravessam a máscara circular, limitação já
registrada da lista atual. Isso não equivale a auditoria de leitor de tela ou teclado físico.

A fixture anterior do app no AVD usava assinatura release e foi removida somente nesse
emulador antes da instalação debug. O relógio pessoal recebeu atualização release com dados
preservados. Fonte e tempo de tela do AVD foram restaurados pelo roteiro; o emulador foi
encerrado ao terminar. Nenhum ajuste do relógio foi feito por esse roteiro.

A [prova física do Watch8](../2026-10-21-watch8/README.md) registra separadamente a abertura
pelo UID comum do APK e a confirmação do duplo clique real pelo usuário. O AVD não comprova
autonomia, o seletor Samsung ou os demais acessos físicos.
