---
card_key: watch8-battery
title: WatchReserve — economia e atalhos para Galaxy Watch com Wear OS
priority: p1
complexity: média
depends_on: []
labels:
  - wear-os
  - android
---

# WatchReserve — plano e progresso

## Resumo Trello

Criar um app pessoal para Galaxy Watch com Wear OS/API 30+, da família Watch4 em diante, com Home, tile e complicação para facilitar o acesso ao modo Samsung “Só relógio”, além de uma central de ajustes manuais. Incluir variantes Classic, Pro, FE e Ultra quando atenderem à plataforma; excluir Tizen. O Watch8 LTE de 40 mm continua a primeira referência, sem validação física. Publicar código, instruções, verificações e APKs assinados no repositório público `brenopoubel0324/WatchReserve`.

O projeto foi iniciado como Watch8-Battery e passa a usar a pasta independente `C:\Users\breno\code\GitHub\WatchReserve`, incluindo a documentação neste repositório; o backend permanece fora do escopo. O plano aprovado é executado em etapas para não afirmar funcionamento Samsung nem economia sem prova física. `card_key: watch8-battery` permanece como identificador persistido do plano.

### Ampliação de escopo em 19/10/2026

O usuário escolheu o nome **WatchReserve** e pediu que o projeto contemple outros Galaxy Watch com Wear OS, além do Watch8. Aplicar o nome ao app, ao repositório público, à pasta local e aos próximos APKs. Preservar application ID, componentes Android, certificado e chave de assinatura para manter a atualização da versão anterior. Registros históricos de releases permanecem fiéis aos artefatos publicados.

1. Revisar compatibilidade por recursos, sem lista fixa de modelos; omitir LTE onde não houver telefonia e aceitar os pacotes Samsung Health já declarados.
2. Revisar funcionalidade e acabamento juntos: navegação Voltar, textos, nomes consistentes, alvos de toque, legibilidade e limites das telas pequenas. Registrar separadamente inspeção de código e verificação visual em dispositivo.
3. Executar build, lint, testes existentes e bug sweep; publicar uma nova prévia assinada com as limitações físicas explícitas.

Compatibilidade prevista abrange a linha Galaxy Watch com Wear OS, a partir da família Watch4; modelos Tizen ficam fora do APK. Nenhum modelo será declarado validado sem teste físico. Confirmações Samsung continuam locais e vinculadas ao firmware e ao pacote do destino.

Achados corrigidos na revisão: nível médio — botão LTE sem verificar telefonia, Saúde restrita a um dos dois pacotes declarados, ausência de retorno interno pelo botão Voltar, rolagem compartilhada entre páginas e mensagens fora da área visível; nível baixo — nome do tile duplicado, ícones opacos, campo com alvo pequeno e repetição de avisos sem nova rolagem. Três rodadas de inspeção concluídas, sem achados bloqueantes, altos ou médios remanescentes nos diffs de UI. A revisão identificou também a falha da primeira CI por ausência de SDK; preparação explícita adicionada e validada com `actionlint`.

Entrega publicada: **v0.2.0**, com APK `WatchReserve-0.2.0.apk`. Application ID `dev.brenopoubel.watch8battery`, nomes dos componentes, alias, certificado e os diretórios privados legados de assinatura foram preservados. Diagnósticos novos usam `%LOCALAPPDATA%\WatchReserveDiagnostics` e prefixo `watchreserve-`. A v0.1.0 permanece histórica, sem alterar asset, hash ou commit.

## Critérios de Aceite

- [x] Repositório público em `main`, com código, wrapper, documentação e CI de leitura.
- [x] Build local, lint, 17 testes JVM, diagnóstico simulado e bug sweep concluídos.
- [x] APK de prévia assinado com chave persistente e publicado com tag e limitações explícitas.
- [x] Nome WatchReserve e URLs canônicas aplicados; identidade Android preservada para atualização da v0.1.0.
- [x] Adaptação por recursos implementada e revisada no código: LTE condicionado à telefonia e ambos os pacotes Saúde declarados.
- [x] Revisão funcional e de acabamento da v0.2.0 concluída no código; testes visuais/físicos pendentes explicitamente.
- [x] Build, lint, testes, assinatura e CI da v0.2.0 executados e registrados após a mudança.
- [ ] Home, tile e complicação comprovados no Watch8, inclusive após reinício e morte normal do processo.
- [ ] Firmware e versão do pacote da activity candidata registrados de forma privada; resumo técnico público revisado.
- [ ] Ao menos um atalho reduz ações até a confirmação de “Só relógio”, contra o melhor caminho nativo.
- [ ] Nenhuma activity Samsung habilitada sem teste pelo próprio APK com permissões comuns.
- [ ] Ganho de autonomia medido em janelas equivalentes; resultado inconclusivo aceito.

## Etapas de implementação

1. Preparar projeto Kotlin/Compose para Wear OS, com API mínima 30 e alvo 36; validar build e testes.
2. Implementar uma entrada “Só relógio” compartilhada por launcher/Home, tile e complicação, com fallback por intents públicos. Alguns seletores podem usar o nome WatchReserve. Manter confirmação Samsung.
3. Entregar a central com bateria atual e atalhos para configurações. Não depender de permissões de escrita.
4. Publicar prévia técnica assinada, documentação e resultados locais verificáveis.
5. Conectar o Watch8 por ADB e investigar a tela de “Só relógio”. Provar acesso no APK antes de habilitar um componente Samsung; vincular a validação ao firmware e à versão do pacote candidato e invalidá-la após mudanças. Aceitar somente pacote do sistema associado às configurações gerais ou de economia.
6. Executar testes físicos e protocolo de autonomia. Publicar somente resumos sem identificadores pessoais.
7. Investigar ajustes adicionais por ADB em etapa separada; liberar apenas após prova de aplicação e restauração.

## Controles futuros e reversibilidade

Prioridade reafirmada pelo usuário após a v0.2.1: avançar em ações reais de redução de consumo com permissões avançadas, além dos atalhos. [Pesquisa registrada](../referencias/permissoes-e-so-relogio.md): AOSP/Buoy sustentam a investigação da política de economia; DNDSync fornece um exemplo Wear OS/Samsung com permissão via ADB. Nenhuma referência comprova funcionamento no Watch8 ou ativação direta de “Só relógio”. A prévia atual continua sem escritas globais. Protótipos podem ser desenvolvidos e verificados em emulador; o aceite de cada controle ainda exige aplicação e restauração no firmware real.

Economia padrão, localização, brilho e tempo de tela são investigações separadas. Escrever e reler uma chave não prova o efeito no relógio; confirmar por API independente, interface e comportamento. Para economia padrão, incluir recarga e reinício.

Uma eventual implementação de controles deve manter uma única sessão persistente dos valores anteriores e dos resultados confirmados. Reaplicar não sobrescreve o registro original; restauração preserva conflitos com mudanças posteriores do usuário. Permissões revogadas e falhas parciais devem aparecer no resultado. O registro fica excluído do backup, com recuperação manual documentada. Desinstalar não restaura configurações do sistema.

Esta prévia não aplica esses controles nem solicita `WRITE_SECURE_SETTINGS`.

### Próximo incremento — prova isolada no emulador

Revisão do plano pela skill `execution-plan-review` em 19/10/2026. Achados antes de implementar:

| Severidade | Lacuna | Ajuste obrigatório |
|---|---|---|
| Alta | Sucesso pelo shell não demonstra escrita pelo APK. | Módulo `lab` com application ID próprio; escritas por `ContentResolver` com UID comum, sem adotar identidade shell. Usar ADB somente para conceder/revogar a permissão do laboratório e preparar a bateria simulada. |
| Alta | `finally` não protege uma interrupção do processo durante alterações globais. | Salvar uma única sessão em arquivo atômico antes das alterações; recusar nova prova com sessão pendente e oferecer recuperação explícita. Conservar o registro se a restauração falhar. |
| Alta | Adicionar permissões ao app publicado confundiria prévia e experimento. | Manter o módulo `app` e seu manifesto intactos; laboratório sem launcher, rede, serviços ou variante release. |
| Média | Ler a chave escrita não comprova efeito no Android. | Observar `PowerManager.isPowerSaveMode` e `getLocationPowerSaveMode`; relatar separadamente compatibilidade emulada e autonomia ainda não medida. |
| Média | Carregamento bloqueia a economia e altera a interpretação. | Recusar bateria já simulada; simular desconexão apenas no AVD reservado e executar `dumpsys battery reset` ao concluir. |

**APROVADO para esta prova de laboratório após incorporar os ajustes acima.** O plano completo continua parcialmente implementado. A solicitação original já autoriza investigar controles por ADB; esta etapa executa somente essa investigação em um emulador próprio.

1. Criar `:lab`, sem dependência do app principal, com `WRITE_SECURE_SETTINGS` e instrumentação. Não incluir `WRITE_SETTINGS` ainda: brilho e tempo de tela terão prova própria.
2. Proteger a execução pelo serial, AVD reservado, hardware emulado, tela Wear circular e argumento explícito de laboratório. Gravar os valores anteriores e a observação inicial em `noBackupFilesDir` antes da primeira alteração.
3. Confirmar bloqueio sem permissão; conceder somente ao APK lab. Solicitar economia padrão por `low_power` e observar o estado pela API pública.
4. Alterar somente `location_mode` dentro de `battery_saver_constants`, preservando os demais campos; observar o modo efetivo pela API. Esse é um teste de capacidade, não um perfil de economia recomendado.
5. Revogar a permissão e confirmar bloqueio de escrita. Recuperar a permissão para restaurar a sessão, verificar chaves e APIs, remover a simulação da bateria e devolver a permissão ao estado inicial. Não apagar o registro em falha.
6. Expor uma execução de recuperação que apenas restaura uma sessão pendente, sem aplicar novo perfil. Recusar execução normal enquanto houver registro pendente.
7. Compilar/lintar laboratório e app, executar os 23 testes JVM existentes, rodar a prova no AVD e autoauditar implementação/resultados antes de commit. A CI compila a instrumentação, mas não declara execução emulada na nuvem.
8. Publicar código, roteiro e evidência revisada no GitHub. Não gerar nova release do app de usuário por esta prova. Próximas etapas dependem do resultado: sessão restaurável no app, brilho/tempo de tela e validação Samsung.

Ajuste após a primeira execução: a imagem Wear OS 5 declara `config_batterySaverSupported=false`; a escrita de `low_power` não foi observada pela API e não foi aprovada como controle funcional. Incluir essa capacidade no relatório e testar também localização global por `LocationManager.isLocationEnabled`, com valor anterior na mesma sessão. Uma prova de infraestrutura aprovada não implica economia ou política AOSP aprovadas. Os resultados por controle devem ser explícitos.

Etapa concluída em **21/10/2026**: módulo `:lab` isolado, prova de localização global observada e restaurada no AVD, bloqueio após revogação e recuperação após morte real do processo. Build/lint do laboratório sem erros/avisos e 23 testes JVM do app aprovados. [Auditoria em três rodadas](../auditorias/2026-10-19-laboratorio.md) aprovada para o laboratório; [evidências por controle](../evidencias/2026-10-19-controles/README.md). Economia/política AOSP não validadas na imagem; Samsung e autonomia continuam pendentes. Não houve alteração no APK publicado. O próximo passo específico Samsung é descoberta no Watch8 quando disponível, antes de novas telas de perfil ou automação de “Só relógio”.

Para “Só relógio”, priorizar a descoberta da tela nativa quando o Watch8 estiver disponível. Automação por acessibilidade e comando Samsung direto permanecem alternativas posteriores, sem ativação presumida. Não manter serviço de acessibilidade ou depuração no relógio apenas para sustentar um atalho ainda não comprovado.

## Estudo do Flashpay em 19/10/2026

Referência indicada pelo usuário: `TransitNow/flashpay-wearos`, commit `9451df887b62e8b172ad930f8ebeb189df02494b`. O estudo compara atalhos, início da atividade, interface circular e custos de execução com a v0.2.0 do WatchReserve.

1. [x] Inspecionar manifesto, atividades, temas, layout, diagnóstico, build e captura pública do menu; conferir a existência de licença explícita.
2. [x] Separar padrões úteis de mecanismos que não controlam a economia do sistema; comparar com o código atual.
3. [x] Registrar [evidências e prioridades](../referencias/flashpay-wearos.md), sem copiar código/ativos nem alterar o APK.

Priorizar a revisão de layout circular e a medição da abertura do atalho. O exemplo de menu e alias separados é uma alternativa de navegação a investigar; a entrada única do launcher prevista neste plano permanece na v0.2.0. Qualquer mudança futura deve preservar os componentes já usados por Home, tile e complicação e ser comprovada no seletor Samsung do firmware testado.

O estudo não identificou integração com “Só relógio”, tile, complicação ou controle global de economia no código examinado do Flashpay. A descoberta e a validação Samsung continuam pendentes de teste físico; nenhuma evidência de ganho de autonomia foi adicionada.

## Progresso em 19/10/2026

### Revisão de execução e emulação solicitadas pelo usuário

Aplicar a skill `self-audit-loop` antes da execução em emulador. Escopo: implementação no commit `5361393`, requisitos aprovados e alterações de código da v0.1.0 para a v0.2.0; o diff vazio contra `origin/main` não substitui a revisão dos arquivos e contratos atuais.

1. Registrar achados por severidade antes de editar, revisar simplificações e comparar entregas com o plano.
2. Corrigir achados bloqueantes/altos e médios triviais; executar testes focados, build, lint e nova rodada de revisão, no máximo três rodadas.
3. Preparar um emulador Wear OS, testar a interface e os fluxos possíveis e guardar evidências públicas revisadas no GitHub. Não alterar BIOS, recursos de virtualização ou reiniciar o PC para essa preparação; investigar alternativa se a aceleração não estiver disponível.
4. Distinguir sempre revisão de código, execução em emulador e validação Samsung física. O emulador não comprova “Só relógio” nem autonomia real.

Achados e resultados registrados na [auditoria de execução](../auditorias/2026-10-19-execucao.md). Autoauditoria encerrada em três rodadas: aprovado para prévia v0.2.1, sem achados bloqueantes ou altos remanescentes. O plano completo permanece parcialmente implementado.

Correções: cache anterior preservado após falha de persistência, estado visual baseado na validação atual e ações principais priorizadas nas telas pequenas. Validação: 23 testes JVM, seis instrumentados em cada fonte (1,0 e 1,3), build debug/release, lint sem erros, assinatura/alinhamento, atualização real v0.2.0 → v0.2.1 no AVD e abertura do fallback geral. [Capturas revisadas](../evidencias/2026-10-19-wearos5/README.md) registram execução em tela circular; não comprovam Samsung ou ganho de autonomia. O roteiro de emulação restaura fonte e tempo de tela e recusa outro AVD.

**v0.2.1 publicada:** tag no commit `0532462f3adb10f33d11cacc606b872e1657bf36`, [release assinada](https://github.com/brenopoubel0324/WatchReserve/releases/tag/v0.2.1), [CI aprovada](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39318073299) nos dois jobs e download público conferido. Painel e fallback reabriram após reinício do AVD; painel também após parada forçada. O AVD foi encerrado ao final. Nenhuma conexão ou alteração no relógio pessoal.

**Histórico da v0.1.0:** código, wrapper, CI, documentação, diagnóstico e APK assinado foram publicados, com validação física adiada. Builds debug/release, 17 testes JVM, lint e download público passaram localmente. A assinatura foi validada com os arquivos do backup privado. A CI aguardava runners naquela entrega; depois falhou por ausência do SDK, corrigida na v0.2.0 abaixo.

**v0.2.0 publicada — histórico:** ampliação para Galaxy Watch Wear OS, nome WatchReserve, adaptação de recursos e revisão funcional com acabamento em três rodadas de inspeção de código. Builds debug/release, 17 testes JVM, lint sem erros, actionlint, diagnóstico em PowerShell 7.6/5.1, assinatura, alinhamento e download público passaram. A [CI desta versão](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39311475179) foi aprovada nos dois jobs. O APK mantém a identidade da primeira release. Naquela entrega os testes visuais estavam pendentes e a tentativa de renderizar vetores no Chrome foi bloqueada; a v0.2.1 adiciona a emulação descrita acima. Consulte os [resultados](../results.md), a [matriz de compatibilidade](../compatibility.md) e o [roteiro de revisão](../validation.md).

O usuário adiou a conexão física e poderá instalar depois, pelo celular com Bugjaeger ou pelo PC com ADB. Esta entrega pode publicar uma prévia sem depender do relógio; nenhum novo pareamento é exigido agora. Continuam pendentes a identificação do firmware, a abertura da confirmação Samsung pelo APK, a seleção em Home, os testes em mostrador e as medições de autonomia. Nenhum resultado de economia está confirmado.

## Premissas

Uso pessoal, repositório público, instalação por APK, ADB aceito e sem root. Acionamento manual e máxima autonomia; LTE desligado é uma preferência do usuário. A autonomia informada de três dias e algumas horas com Bluetooth desligado e economia ativa é uma referência inicial, não uma medição controlada. Mostrador próprio e alteração do atraso de “Só relógio” ficam fora desta versão.
