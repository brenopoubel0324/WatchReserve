# Revisão da descoberta Samsung — 21/10/2026

Escopo: coleta física somente leitura e correção restrita de descoberta do provedor de bateria,
a partir de `4aca3a3`. Métodos: revisão de execução e autoauditoria já adotados no projeto,
com revisão funcional e de acabamento. Sem revisão por agente independente.

## Achados e correções

| Severidade | Evidência | Ação |
|---|---|---|
| Alta | As duas ações Android consultadas resolvem para o pacote geral; a candidata pertence ao pacote Samsung separado. | Consultar também a ação Samsung de bateria e declará-la em `queries`; preservar exigência de app do sistema e prova manual. |
| Média | Instruções com `IP_DO_RELOGIO:PORTA` foram copiadas literalmente e comandos colados no prompt do código. | Usar entrada interativa em blocos separados e explicar o formato e o prompt numérico. |
| Média | README e matriz ainda descreviam a conexão como adiada. | Registrar a coleta concluída e separar identificação física de abertura comprovada pelo APK. |
| Média | A entrada via ADB apareceu no editor do teclado Samsung sem preencher o campo do app; digitar o nome longo é pouco prático. | Acrescentar busca declarada pelo firmware, sem salvar ou confirmar automaticamente. Não generalizar a observação ADB como falha da digitação manual. |
| Alta | Home chamou a entrada correta com flags `0x10200000`, mas Android retomou o painel existente; ambas as atividades tinham a mesma afinidade. | Separar a tarefa do atalho, acrescentar regressão instrumentada e repetir com os flags reais. Abrir a atividade sem esses flags não cobria esse comportamento. |

A tela exportada não é habilitada automaticamente. O caminho compartilhado de Home, tile e
complicação, a confirmação visual e a invalidação por firmware/versão permanecem intactos.
Não foram adicionadas permissões, serviços, sensores, rede, polling ou tarefas periódicas.

## Verificações

- Build debug e pacote instrumentado compilados; 23 testes JVM aprovados, sem falhas.
- Lint debug: zero erros e os mesmos doze avisos de atualização de dependências.
- Manifesto final debug contém a consulta Samsung e não solicita permissões avançadas.
- Atividade nativa identificada com abertura manual pelo usuário; manifesto da versão ativa
  revisado quanto a exportação e permissões. Nenhuma abertura pelo shell usada como prova.
- Verificado o comportamento opt-in de filtros de intents no Android 16: o manifesto Samsung
  inspecionado não declara `intentMatchingFlags`. A abertura explícita continua exigindo teste
  pelo APK; não inferida a partir dessa ausência.
  [Contrato Android 16](https://developer.android.com/about/versions/16/behavior-changes-16#safer-intents).
- Funcionalidade e acabamento: botão de busca na tela de validação, mantendo entrada manual,
  mensagens de ausência/ambiguidade e as etapas de salvar, testar e confirmar. Instruções de
  pareamento e estados da documentação revisados. Painel, busca, teste, retorno e confirmação
  final observados no Watch8. Home, tile e complicação reais confirmados pelo usuário em uso normal.
- Build release final aprovado; certificado persistente, backup, assinatura v3 e alinhamento
  verificados. O mesmo APK assinado foi atualizado no Watch8 e usado na prova.
- Atividade Samsung aberta pelo UID comum do APK de produção, com pacote chamador registrado
  na pilha Android e tela correta observada em captura privada. Sem instrumentação ou adoção
  de identidade shell. Destino confirmado na interface e entrada compartilhada exercitada.

Os sete testes instrumentados passaram em cada escala de fonte no AVD reservado (1,0 e 1,3),
incluindo a nova regressão com os flags reais de Home e painel aberto. Oito capturas foram
revisadas; fonte e tempo de tela restaurados e emulador encerrado. Eles não cobrem o provedor
Samsung. A abertura pelo APK foi executada fisicamente nesta sessão. Os
cenários de ausência/ambiguidade da busca foram revisados no código, sem outro firmware
físico para exercitá-los. Não há nova aprovação de fonte ampliada ou acessibilidade.

Revisão final após o ajuste de acabamento: **APROVADO para a prévia 0.2.2 e para a abertura
da confirmação no SM-L325F/L325FXXS3AZF4**. Não é aprovação de todos os acessos ou de economia.
O teste enviado pelo shell abriu apenas a atividade do WatchReserve; a chamada Samsung saiu
do próprio app. Após corrigir a retomada de tarefa, a chamada com os flags reais funcionou no
Watch8 e o usuário confirmou o duplo clique físico. Home reduziu navegação desde o mostrador;
tile e complicação também foram confirmados pelo usuário em uso normal. Ciclo de vida
completo e autonomia continuam pendentes.
[Resumo físico e hash final](../evidencias/2026-10-21-watch8/README.md),
[evidência do AVD](../evidencias/2026-10-21-wearos5/README.md).

Bug sweep: diff, status, histórico recente, contratos de descoberta/prova, permissões e limites
de evidência revisados. Sem achados bloqueantes ou altos remanescentes na correção restrita.
Dumps, APKs Samsung e identificadores ficam privados. A integração completa permanece em
validação; nenhum ganho de autonomia ou redução de ações foi declarado. `git diff --check`,
UTF-8 e links locais verificados. Nenhuma modificação no laboratório ou contrato de armazenamento.
