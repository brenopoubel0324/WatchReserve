# Revisão da prova de controles — 19/10/2026

Escopo: módulo `:lab`, roteiro de emulador e integração de build/CI, a partir de `c734d53`. O APK de usuário não recebe controles ou permissões. Plano e revisão prévia registrados na [etapa de laboratório](../planos/2026-10-19-watchreserve.md#próximo-incremento--prova-isolada-no-emulador). Método de implementação: skill `self-audit-loop`, até três rodadas, sem agente independente.

## Rodada 1 — achados antes de executar

| Severidade | Local | Problema e correção |
|---|---|---|
| Alta | `restore` | A verificação da API ocorria antes de remover a bateria simulada; o retorno ao carregamento pode alterar o estado novamente. Verificar estado efetivo também após o reset e limitar a fixture ao estado inicial de economia desligada. |
| Média | Identidade da escrita | O relatório calculava um booleano de UID, sem exigir correspondência ao UID real do pacote. Rejeitar execução se a identidade divergir. |
| Média | Arquivo de sessão | Confirmar sincronização e releitura do JSON antes de qualquer alteração, sem tratar apenas o retorno de `finishWrite()` como prova da gravação. |

Sem prova de funcionamento dos controles no início desta rodada. Preparação e execução no AVD ainda pendentes; não apresentar revisão como concluída.

## Rodada 2 — resultado negativo e ajuste da prova

Build inicial falhou no lint por ausência da declaração standalone do laboratório; corrigida. Build debug/androidTest e lint do laboratório, além de build/lint e 23 testes JVM do app, passaram em seguida.

A primeira prova recebeu a permissão avançada e escreveu `low_power`, mas falhou ao aguardar `PowerManager.isPowerSaveMode`. A restauração terminou e o registro foi removido; a bateria simulada foi resetada e a permissão revogada. A imagem informa `config_batterySaverSupported=false` e `Battery Saver: DISABLED` no serviço de energia. Isso explica o limite da prova AOSP, sem concluir nada sobre o firmware Samsung.

Achado alto: a capacidade nativa da imagem não foi incorporada ao primeiro roteiro. Corrigir o relatório para separar sucesso da infraestrutura/restauração de validação de cada controle, consultar a capacidade e registrar explicitamente controles não observados. Não transformar escrita de chave em aprovação de economia.

Ampliar a prova somente com localização global, verificável por `LocationManager.isLocationEnabled`, e incluir sua restauração no mesmo registro. Economia AOSP e sua política permanecem não validadas nesta imagem. A recuperação após morte do processo será exercitada com os ajustes de laboratório, sem declarar economia medida.

## Rodada 3 — revisão final e recuperação

A prova de capacidades executou com sucesso: bloqueio sem permissão, concessão ao UID do APK, localização global alterada e observada pelo serviço, bloqueio após revogação e restauração completa. Economia e política AOSP ficaram explicitamente `false` nos campos de controle validado; aprovação do teste não significa aprovação desses controles.

Achado médio antes da correção: uma falha precoce ao restaurar economia poderia impedir a tentativa independente de restaurar localização. Cada escrita passa a ter tentativa separada; falhas acumulam e conservam a sessão. Acrescentadas regras explícitas de backup/transferência e ícone do laboratório para resolver os dois avisos do manifesto. Não há interface de usuário nova; acabamento revisado nas mensagens, nomes e distinção entre prova técnica e compatibilidade.

Validação final conferida em **21/10/2026**:

- Build debug/androidTest e lint do laboratório aprovados; zero erros e zero avisos. App: 23 testes JVM aprovados e lint com zero erros/doze avisos de versões de dependências.
- Sequência normal → morte real do processo → recusa com sessão pendente e registro preservado → recuperação → nova prova executada. Três execuções completas aprovadas do mesmo teste; dois cenários negativos intencionais, identificados como tais.
- Localização global alterada pelo UID comum do APK e restaurada, com observação por `LocationManager`. Concessão e revogação verificadas; escrita sem permissão bloqueada.
- Economia AOSP e política não validadas na imagem, sem promover a escrita da chave a sucesso funcional. Os relatórios preservam os campos de controle como `false`.
- Serial inválido e combinação de modos incompatíveis recusados pelo roteiro antes da execução.
- Hashes dos dois APKs conferidos contra a evidência. Nenhuma alteração no diretório `app`; laboratório sem variante release, launcher, rede ou serviços. Dados da sessão excluídos de backup/transferência.
- `actionlint`, sintaxe dos roteiros, `git diff --check`, UTF-8 e links locais aprovados. Listagem de tarefas confirmou ausência de release no lab; permissões conferidas nos dois APKs, sem permissão avançada no app principal.
- [Resultados e saídas revisadas](../evidencias/2026-10-19-controles/README.md) preservados no repositório. Nenhum dispositivo permanece conectado ao ADB na conferência final.

**APROVADO para laboratório exclusivo do AVD**, após três rodadas, sem achados bloqueantes ou altos remanescentes. Os achados altos de verificação após reset e capacidade da imagem foram corrigidos. Não é aprovação de controles para Watch8 nem de ganho de autonomia.

Pendências: descoberta e confirmação Samsung, prova física por modelo/firmware e mecanismo de sessão do app de usuário com resolução de conflitos. O laboratório é uma fixture que restaura seu AVD reservado; não substitui esse mecanismo. Brilho/tempo de tela, LTE e CPU continuam fora da prova. Acabamento revisado nas instruções e mensagens; não houve nova interface visual.

Bug sweep: diff, status, alterações recentes, isolamento dos módulos, manifesto/permissões, dependências, relatórios e lacunas de teste revisados. Validações adicionais de workflow/links e CI registradas em [resultados](../results.md). Sem achados bloqueantes ou altos; sem dados privados publicados.
