# Shizuku no Wear OS — viabilidade e próximo incremento

**Atualização em 22/10/2026:** o [plano atual Relógio + alarmes](../planos/2026-10-22-relogio-alarmes.md)
prioriza Shevery 14.4.0 e inclui recuperação após reboot, registros separados de restauração
e medição R/P/E. A [auditoria do plano](../auditorias/2026-10-22-plano-relogio-alarmes.md)
registra os contratos e riscos encontrados na fonte fixada. O restante desta página preserva
a pesquisa e a decisão de 21/10/2026; “API original primeiro” não é a seleção atual do gerenciador.

Pesquisa e prova em emulador em **21/10/2026**, por solicitação do usuário. Objetivo:
investigar controles adicionais à economia Samsung, preservando relógio e alarmes, sem root.
O APK publicado do WatchReserve não foi alterado e ainda não usa Shizuku.

## Conclusão e evidência

Shizuku amplia o acesso disponível ao app por uma ponte Binder para um processo iniciado
com identidade ADB (`shell`, UID 2000). Isso permite investigar operações que não são
concedidas por `WRITE_SECURE_SETTINGS`. Instalar o gerenciador sozinho não acrescenta
funções ao WatchReserve: é necessário integrar a API e obter autorização. [API][api]

O **APK oficial 13.6.0** instalou e iniciou no AVD circular Wear OS 5/API 34. O processo
`shizuku_server` executou como `shell`; o gerenciador, como UID comum. Após encerrar somente
o processo do gerenciador e reabri-lo, ele recebeu a ponte e mostrou o servidor ativo.
O mesmo ocorreu com **Wi-Fi desligado em modo avião**, confirmado pela chave e pelo serviço
Wi-Fi. A comunicação local não exigiu Wi-Fi ativo nessa prova.
[Procedimento, capturas e limites](../evidencias/2026-10-21-shizuku-wearos5/README.md).

Isso comprova a instalação, inicialização e consulta de estado pelo gerenciador oficial
no emulador. Não comprova a autorização de um cliente WatchReserve, execução de controles,
persistência sob economia Samsung, funcionamento de alarmes ou ganho de bateria.

## O que acrescentaria ao projeto

| Possibilidade | Utilidade para o objetivo | Limite atual |
|---|---|---|
| Diagnóstico local de energia e atividade | Coletar política efetiva, despertares, jobs e alarmes com economia ligada, exportando depois | A API permite código em um `UserService` com identidade shell; as leituras precisam ser testadas no firmware. Grants de diagnóstico podem ser uma alternativa mais simples, sem ponte residente. |
| AppOps e standby por aplicativo | Restringir trabalho residual de apps opcionais identificado nos contadores | Confirmar autorização e efeito real por operação. Repetir a política nativa pode não acrescentar economia. |
| Desativação temporária de pacotes opcionais | Interromper componentes que continuem ativos e não sejam necessários ao uso de relógio e alarmes | Revisar UID compartilhado e dependências, preservar estado exato e restaurar. Sem desinstalação ou lista genérica. |
| Aplicar e restaurar ajustes diretamente no relógio | Executar a ação manual sem reconectar o PC a cada mudança enquanto a ponte estiver disponível | A perda da ponte pode impedir restauração imediata. Sessão persistente e recuperação continuam obrigatórias. |
| Investigação do DeviceIdle | Comparar estados e testar uma hipótese isolada de repouso | Forçar Doze permanece comando de teste, sem garantia de repouso estável no pulso ou de segurança para o alarme Samsung. |

A identidade shell continua sujeita a permissões e SELinux. Não implica root, acesso livre
a dados privados, controle do kernel/undervolt ou remoção de bloqueios de assinatura Samsung.
O acesso específico ao broadcast de ativação de “Só relógio” não foi comprovado para shell;
o atalho já validado mantém a confirmação nativa. [API][api], [investigação do firmware](wearos-profundo.md).

## Wi-Fi, reinício e custo da ponte

Iniciar o processo e usá-lo são etapas distintas. O APK oficial pode ser iniciado por ADB
no PC; depois, Binder é comunicação local. A prova offline do AVD é promissora para a
restrição informada pelo usuário, mas ainda precisa ser repetida no Watch8 com a economia
Samsung ligada. Desligar Wi-Fi não equivale a desabilitar opções do desenvolvedor/depuração;
o manual registra condições que podem interromper a ponte. [Manual][setup]

A [release 13.6.0][release] anuncia início automático sem root no Android 13+ com WLAN
confiável. Portanto, a afirmação genérica anterior de que sempre é necessário reiniciar
manualmente após todo reboot precisa dessa ressalva. O código também exige concessão de
`WRITE_SECURE_SETTINGS` ao gerenciador e último início via ADB; no boot, tenta habilitar
ADB/ADB Wi-Fi e desabilita seu prazo de autorização. Esse comportamento não foi testado
no Samsung e pode contrariar o uso com rádios desligados. Não depender dele no primeiro
incremento nem modificar essas preferências no relógio silenciosamente. [Receiver][boot]

O servidor é um processo adicional; ganho líquido de bateria é uma hipótese. Comparar:
economia nativa; economia nativa com ponte sem novos controles; ponte com a intervenção.
Usar o protocolo pareado existente, fora da depuração. Separar ajustes persistentes que
continuam sem a ponte daqueles que exigem nova chamada para aplicação ou restauração.

Para tarefas pontuais, planejar `UserService` com `daemon(false)` e encerramento explícito.
O padrão da API é `daemon=true`, e apenas desconectar o cliente não garante terminar o
processo do serviço. Não introduzir polling, rede, wake lock ou trabalho periódico no app.
Isso não elimina o processo central Shizuku, cujo custo deve entrar na comparação.
[API e ciclo de vida][api-code]

## Integração ou fork

**Começar pela API oficial**, com o gerenciador original. A prova no AVD não encontrou
necessidade de fork para instalar ou iniciar o servidor. A interface do gerenciador ficou
cortada na tela de 192 dp circular; ainda falta testar a autorização de um cliente nessa tela.
O WatchReserve pode apresentar seus próprios controles com UI Wear, sem reescrever a ponte.

Um fork passa a ter justificativa se autorização, pareamento ou inicialização forem
impraticáveis no relógio, ou se precisarmos adaptar o ciclo de vida e verificar seu custo.
O código do gerenciador/servidor declara Apache 2.0 e restrições de identidade no README;
a API declara MIT. Preservar avisos/licenças e usar identidade visual, nome e application ID
próprios no derivado, respeitando os namespaces reservados. [README upstream][upstream],
[licença da API][api-license]

Renomear só o application ID é insuficiente: servidor e provider da API fixam identificador
do gerenciador, permissão e ações. Um derivado precisa adaptar os contratos correspondentes
e provar autorização/isolamento; não presumir compatibilidade com todos os clientes Shizuku.
[Constantes do servidor][constants], [provider][provider]

## Prioridade: Samsung Health, Gemini e memória

O usuário relatou que Samsung Health e Gemini continuam aparecendo nos recentes. São os
primeiros candidatos à investigação, **sem consumo excessivo confirmado**. Os recentes
representam tarefas acessadas; a presença ali não demonstra processo ativo, serviço,
despertar ou uso de sensores. [Recentes Android][recents]

O Android pode manter um processo em RAM e congelar suas threads, evitando trabalho de CPU
e o custo de recriá-lo. Confirmar suporte e estado real do freezer no Samsung; RAM ocupada
ou quantidade de cartões nos recentes não serão métricas de ganho de bateria. Um limite
menor pode aumentar reinicializações de apps. Não adicionar um serviço que mate processos
periodicamente. [Freezer AOSP][freezer-doc]

| Candidato | O que observar | Experimento possível depois da leitura |
|---|---|---|
| Gemini | Pacote que realmente hospeda o assistente, UID, serviços, CPU, despertares e estado de “Hey Google”/“Levantar para falar” | Comparar gatilhos desligados; se necessário, testar restrição ou desativação temporária do pacote identificado. Não desativar Google Play Services por suposição. |
| Samsung Health | Pacote/UID, serviços vinculados, sensores, tarefas, sincronização e estado de “Limitar funções de saúde” na economia | Comparar a referência nativa e depois uma restrição isolada do app. Separar a UI de saúde dos serviços que efetivamente coletam dados. Não desativar a infraestrutura inteira de sensores. |
| Outros serviços | Atividade acumulada e dependências, inclusive UID compartilhado | Selecionar por evidência de atividade residual; proteger alarme, hora e interface. Sem lista genérica de pacotes a remover. |

O Google documenta que “Hey Google” exige o relógio acordado e que “Levantar para falar”,
quando disponível e ativado, detecta movimento do braço. Também informa que o app Google
hospeda recursos usados pelo Gemini; o nome mostrado no launcher não identifica sozinho
qual pacote/serviço restringir. Os estados dessas opções no Watch8 ainda não foram lidos.
[Google][gemini]

A Samsung documenta “Limitar funções de saúde” para Watch7/Ultra, com redução de medições
e detecções em segundo plano. O APK de bateria já inspecionado também contém essa função,
mas seu estado na unidade Watch8 não foi confirmado. Repetir o ajuste nativo não comprova
economia adicional. [Samsung][health-saving], [análise do firmware](wearos-profundo.md).

Também investigar um **limite de processos em cache**, inclusive zero, como teste separado.
No AOSP consultado, `ActivityManagerService.setProcessLimit()` exige `SET_PROCESS_LIMIT`,
declarada pelo pacote Shell, e altera o limite de cache. É um caminho potencial via Shizuku,
sem equivaler a desligar todos os serviços de segundo plano. “Não manter atividades” também
atua sobre atividades, sem representar esse bloqueio geral. Nenhuma dessas alterações foi
executada no AVD ou no Watch8 nesta etapa. [Activity Manager][am], [limites][am-constants],
[manifesto Shell][shell-manifest]

Antes de qualquer limite: guardar o valor original, verificar aplicação por API/estado
independente, comparar CPU/despertares e reinicializações, testar o alarme e restaurar ao
valor anterior. Só oferecer o controle se houver benefício observado. A prioridade continua
ser impedir trabalho dispensável, com acionamento manual e sem um monitor permanente.

O próximo cliente de leitura deve distinguir tarefa recente, processo em cache/congelado,
serviço ativo e contadores acumulados por UID. Identificar os pacotes reais antes da coleta
direcionada; nomes de apps não serão convertidos em uma lista presumida de packages.
Atividade atribuída a UID compartilhado não identifica sozinha qual app causou o consumo.

## Revisão de execução do próximo incremento

Aplicada a revisão de execução já usada no projeto. Achados antes de implementar:

| Severidade | Lacuna | Ajuste obrigatório |
|---|---|---|
| Alta | Status do gerenciador confundido com controle validado pelo WatchReserve | Cliente de laboratório separado, autorização explícita e primeira chamada somente leitura com identidade shell verificada. |
| Alta | Assumir que a ponte sobreviverá à economia Samsung/reinício | Testar no firmware, registrar perda do Binder e distinguir Wi-Fi desligado de depuração desativada. Não depender de autoarranque. |
| Alta | Restaurar exige ponte que pode desaparecer | Reutilizar o contrato de sessão única, estado original persistente, conflitos preservados e recuperação manual documentada antes de qualquer escrita. |
| Alta | Restringir pacote necessário ao alarme | Identificar alarme local, UID e dependências; testar cada intervenção separadamente com os critérios físicos existentes. |
| Média | Serviços do cliente deixados em modo daemon | Optar por `daemon(false)`, implementar encerramento e verificar ausência de processo residual ao terminar. |
| Média | Fork por antecipação aumenta contratos e manutenção | Integrar API original primeiro; condicionar fork a limitação reproduzida de uso ou compatibilidade. |

**APROVADO para preparar um cliente de laboratório somente leitura**, separado do app de
usuário. Não é aprovação de controles funcionais nem de um fork como solução necessária.

1. Cliente com identidade própria, sem variante release ou dependência do app principal;
   conferir manifesto final, autorização negada/concedida/revogada e ciclo do Binder.
2. Operações enumeradas de leitura, sem terminal arbitrário exposto. Verificar ponte viva,
   autorização e UID 2000; tratar timeout/morte do processo sem repetir alterações.
   Guardar relatórios privados localmente e exportar apenas por ação manual.
3. Provar no emulador a chamada do cliente e encerramento do `UserService`, incluindo
   morte/reabertura e UI de autorização em tela circular. Não confundir o gerenciador com
   esse cliente nem uma chamada direta por ADB com chamada pela ponte.
4. Repetir no Watch8: iniciar com economia desligada, autorizar cliente, desligar Wi-Fi pela
   economia nativa, coletar localmente, reconectar e ler o resultado. Sem restrições de apps.
5. Usar a evidência para selecionar uma intervenção com restauração e teste do alarme;
   medir o custo da ponte antes de disponibilizar um modo “relógio + alarmes”.

O emulador foi restaurado e encerrado após a prova. Nenhum contato, instalação, permissão
ou ajuste novo no relógio físico. Sem nova release do WatchReserve. Bug sweep funcional e
de acabamento: evidências, limites de permissão, contratos, estado do Git e documentação
revisados; autorização do cliente, Samsung, alarmes e autonomia permanecem pendentes.

## Fontes e versões fixadas

- Gerenciador/servidor: `RikkaApps/Shizuku`, commit
  `b844bc491f1790c72328e1a8e5b2349f8978f0ea`; inspeção estática, sem alterar o upstream.
- API: `RikkaApps/Shizuku-API`, commit `a27f6e4151ba7b39965ca47edb2bf0aeed7102e5`.
- APK executado: asset oficial `shizuku-v13.6.0.r1086.2650830c-release.apk` da release 13.6.0;
  o snapshot de fonte inspecionado não é apresentado como build reproduzido desse APK.

[upstream]: https://github.com/RikkaApps/Shizuku/blob/b844bc491f1790c72328e1a8e5b2349f8978f0ea/README.md
[release]: https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0
[api]: https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/README.md
[api-code]: https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/api/src/main/java/rikka/shizuku/Shizuku.java
[api-license]: https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/LICENSE
[provider]: https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/provider/src/main/java/rikka/shizuku/ShizukuProvider.java
[boot]: https://github.com/RikkaApps/Shizuku/blob/b844bc491f1790c72328e1a8e5b2349f8978f0ea/manager/src/main/java/moe/shizuku/manager/receiver/BootCompleteReceiver.kt
[constants]: https://github.com/RikkaApps/Shizuku/blob/b844bc491f1790c72328e1a8e5b2349f8978f0ea/server/src/main/java/rikka/shizuku/server/ServerConstants.java
[setup]: https://shizuku.rikka.app/guide/setup/
[recents]: https://developer.android.com/guide/components/activities/recents
[freezer-doc]: https://source.android.com/docs/core/perf/cached-apps-freezer
[gemini]: https://support.google.com/gemini/answer/16401122
[health-saving]: https://www.samsung.com/ae/support/mobile-devices/how-to-extend-battery-life-and-use-power-saving-modes-on-watch7-and-watch-ultra/
[am]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/am/ActivityManagerService.java
[am-constants]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/am/ActivityManagerConstants.java
[shell-manifest]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/packages/Shell/AndroidManifest.xml
