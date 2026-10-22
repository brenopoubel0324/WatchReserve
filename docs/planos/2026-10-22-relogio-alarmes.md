---
card_key: watchreserve-relogio-alarmes
title: WatchReserve — relógio e alarmes com economia avançada
priority: p1
complexity: alta
depends_on: []
labels:
  - wear-os
  - android
---

# Relógio + alarmes — plano de execução

## Resumo Trello

Entregar um modo manual de economia avançada que preserve a hora e o alarme nativo criado
no relógio, com ganho consistente sobre a configuração econômica atual do usuário. Investigar
trabalho residual de Gemini, Samsung Health e outros componentes opcionais, usando uma ponte
shell sem root. A meta inclui aplicar e restaurar no relógio e recuperar o acesso após reinício
sem PC ou celular; um laboratório de diagnóstico sozinho não conclui essa entrega.

Plano atual em **22/10/2026**, a partir de `d7a48f0`. Substitui a prioridade de próximos passos
do [plano histórico](2026-10-19-watchreserve.md), preservando suas evidências e pendências.
[Revisão de execução e fontes fixadas](../auditorias/2026-10-22-plano-relogio-alarmes.md).
Os controles abaixo ainda não foram implementados ou validados no Watch8.

## Ponto de partida e decisões

| Estado | Evidência e consequência |
|---|---|
| Comprovado no Watch8 | v0.2.2 abre a confirmação Samsung pelo UID comum do app; Home, tile e complicação confirmados pelo usuário em uso normal no SM-L325F/L325FXXS3AZF4. Preservar esses acessos e a confirmação nativa. |
| Referência informada | Três dias e algumas horas, Bluetooth desligado e economia Samsung ligada; ainda sem comparação controlada. Repetir ajustes nativos não representa ganho adicional. |
| Restrição física conhecida | Economia Samsung desliga Wi-Fi. ADB antes/depois não comprova política efetiva durante o intervalo econômico; é necessário cliente local. |
| Prova emulada limitada | Shizuku oficial 13.6.0 iniciou como shell no Wear OS 5/API 34 e respondeu ao gerenciador com Wi-Fi desligado. Não prova cliente WatchReserve, Samsung, reboot ou autonomia. |
| Hipóteses | Health e Gemini aparecem nos recentes. Isso não demonstra processo ativo nem consumo excessivo; cache ocupado também não é evidência de gasto. |
| Ainda desconhecido | Shevery instalado em qual dispositivo/versão, alvos e UIDs reais, atividade residual, dependências do alarme, compatibilidade da API e recuperação no firmware Samsung. |

Decisões do usuário: acionamento manual, sem root, relógio e alarme nativo como funções
indispensáveis, ganho consistente sem porcentagem mínima e revisão funcional junto à de
acabamento. A recuperação pode usar Wi-Fi brevemente em WLAN confiável, automaticamente ou
por ação no próprio relógio. Não exigir PC/celular na rotina após a preparação inicial.

O app principal continuará sem polling, sensores, limpador de RAM ou trabalho periódico.
O processo privilegiado adicional, se necessário, faz parte do custo do modo. Preferir
controles persistentes que permitam encerrar a ponte e reabri-la apenas sob demanda.
Não prometer recuperação offline após um boot completo.

Ficam fora: root, Device Owner, kernel/undervolt, desinstalação de pacotes do sistema,
desativação ampla de Play Services ou sensores, automação da confirmação de “Só relógio”,
mostrador próprio e alteração do atraso de exibição da hora nesse modo Samsung.

## 1. Qualificar a ponte e o cliente local

### Base e saída alternativa

Primeiro candidato: **Shevery 14.4.0**, fonte fixada em
`20e0174836b0e78847c83aef9c27521c65fa61fb`. Conferir o que o usuário já utiliza, sem presumir
instalação no Watch8 ou substituir essa instalação. Registrar origem do APK, hash, certificado
observado e versão; assinatura válida sozinha não comprova a identidade do publicador.

Primeiro testar o APK publicado e uma chamada real do cliente, sem exigir compilar o fork.
Usar inicialmente API/provider oficial **13.1.5**, verificando a compatibilidade real com
Shevery, cujo projeto declara API 14.4.0. Conferir pacote, certificado, provider, permissão,
ações, versão de protocolo, autorização por app e UID efetivo 2000. Manter somente uma ponte
selecionada e ativa. Estado “gerenciador funcionando” não substitui chamada pelo cliente.
Se houver incompatibilidade de descoberta/autorização, adaptar o cliente/provider ao contrato
fixado antes de qualquer controle; não mascarar a falha por grants ou comandos ADB diretos.

Criar `:advancedLab`, com application ID próprio `dev.brenopoubel.watchreserve.advancedlab`,
interface Wear e distribuição de desenvolvimento separada. Não remover a proteção de
emulador do `:lab` existente. O novo laboratório não será dependência do `:app` nem terá
variante release. Usar operações enumeradas; nenhum terminal arbitrário exportado.

Se autorização, pareamento, partida ou recuperação forem impraticáveis na tela circular,
ou mecanismos residentes não puderem ser desligados/verificados, preparar o fork público
**WatchReserve-Bridge**, preferencialmente derivado do Shevery. Remover recursos sem utilidade
para a meta, como terminal, IA, módulos e consultas de atualização. Adaptar nome, ícone,
application ID, permissões, ações, provider e identidade dos processos em conjunto com o
cliente; preservar licenças e atribuição dos trechos incorporados.

O Shevery fixado exige JDK 21, SDK/Build Tools 37, NDK 29.0.14206865, CMake 3.22.1,
AGP 9.4.0-alpha07, plugins Kotlin Compose/serialization 2.4.20-Beta2 e Gradle 9.7.0-rc-2.
O WatchReserve usa JDK 17,
SDK 36, AGP 8.13.2, Kotlin 2.2.21 e Gradle 8.13. Isolar instalação, build e CI da ponte;
não migrar o app principal para essa toolchain de prévia. Se o build fixado não puder ser
reproduzido ou for incompatível com API 36, usar como base alternativa o Shizuku oficial
**v13.6.0**, commit `2650830c5b099ae0dd34fedf614d4f592ca05d65`, incorporando somente as
mudanças necessárias do Shevery/outros forks, com origem registrada. Não perseguir forks
sucessivos sem um bloqueio reproduzido. O fork thedjchi/Shizuku serve como referência de
boot/TCP; reiniciar o servidor na mesma sessão não comprova partida após reboot.

### Leitura e ciclo de vida

Implementar contrato interno pequeno: consultar estado, recuperar ponte, ler diagnóstico,
aplicar, restaurar e encerrar. No primeiro incremento, habilitar apenas estado/leitura.
Testar autorização negada, concedida e revogada, Binder morto, timeout, morte/reabertura do
cliente e término do serviço. UserService com `daemon(false)`, encerramento explícito e
verificação de processos restantes; não confundir sua parada com a do servidor central.

Coletar localmente sob economia Samsung: energia/política efetiva, bateria, processos e
serviços, jobs, alarmes e contadores por UID. Identificar pacotes antes de consultas dirigidas.
Separar tarefa recente, processo em cache/congelado, serviço ativo e atividade acumulada;
UID compartilhado não identifica sozinho o responsável. Limitar cada consulta a 10 segundos,
com drenagem da saída, cancelamento e indicação de resultado parcial. Falha ou contador
reiniciado não significa consumo zero. Relatórios privados, exportados só por ação manual.

**Portão 1:** chamada somente leitura executada pelo cliente como shell, autorização e
encerramento provados no AVD e repetidos no Watch8 com economia ligada/Wi-Fi desligado.
Preparar a ponte é uma operação de configuração separada; não chamar essa etapa inteira
de “somente leitura” se houver instalação, grants, pareamento ou escrita de ajustes.

## 2. Recuperação e restauração antes dos controles

### Três registros com responsabilidades distintas

| Registro | Conteúdo e encerramento |
|---|---|
| Preparação do acesso | Estado anterior de grants, preferências de depuração e prazo de autorização alterados pela integração; pareamento tratado como credencial privada. Persiste enquanto a integração estiver habilitada. “Desabilitar integração” restaura apenas mudanças de sua propriedade, depois de resolver o perfil. |
| Recuperação temporária | Valores anteriores e valores aplicados de Wi-Fi, ADB/ADB Wi-Fi e qualquer outro ajuste necessário ao bootstrap. Fecha ao restaurar a configuração anterior, inclusive em timeout. Não altera silenciosamente `adb_allowed_connection_time` para zero. |
| Perfil econômico | Uma única sessão de ajustes, com original, valor aplicado, verificação, alvo/UID/usuário/versão e resultado por operação. “Restaurar perfil” atua nessa sessão; não revoga a preparação enquanto ainda precisa da ponte para recuperar. |

Capturar o estado **antes da primeira alteração**, inclusive na preparação. O `AdbArm` do
Shevery habilita ADB/ADB Wi-Fi e zera o prazo de autorização: o gerenciador original só poderá
ser usado nesse caminho se essas alterações puderem ser registradas e restauradas; caso
contrário, adaptar o bootstrap no fork antes de habilitar recuperação automática.

Um único controlador serializa UI, boot e IPC. Escrita atômica não substitui exclusão mútua.
Persistir operação pendente antes da escrita, confirmar depois de observação independente;
se morrer entre escrita e confirmação, reler e reconciliar antes de tentar novamente.
Reaplicar não substitui o original. Restauração só escreve se o estado atual ainda corresponder
ao aplicado; preservar conflitos do usuário, registrar o motivo e pedir decisão explícita
antes de sobrescrevê-los. Valor ausente, `DEFAULT`, `ENABLED` e modo AppOps são estados distintos.

Registros privados, versionados e excluídos de backup; não expor tokens nem copiar credenciais
para armazenamento público. JSON inválido, permissão revogada ou versão desconhecida mantém
a recuperação pendente e bloqueia novas alterações. Mudança de firmware/pacote invalida
a aprovação do controle; usar leitura para decidir se ainda existe uma restauração segura.
Manter recuperação manual por ajuste documentada; desinstalar/apagar dados não restaura.

### Boot e redes

Por padrão, reconciliar a sessão **após o primeiro desbloqueio**, quando os dados criptografados
do usuário estiverem disponíveis. Não mover o diário ou os tokens para Device Protected
Storage por conveniência. O alarme nativo deve funcionar sem depender do WatchReserve ou da
ponte; registrar seu comportamento original antes do primeiro desbloqueio e preservá-lo.
[Limites de armazenamento no Direct Boot](https://developer.android.com/privacy-and-security/direct-boot).

Após desbloquear, consultar primeiro o estado: ajustes persistentes não serão reaplicados
por suposição. Quando necessário, permitir **uma tentativa automática por boot**, em janela
máxima de **120 segundos**, usando apenas WLAN já confiável e restaurando Wi-Fi/depuração
ao terminar. Persistir o boot e o prazo para que a morte do processo não reinicie a contagem.
Se falhar, apresentar “Recuperação necessária”, com ação manual no relógio;
não manter loop, watchdog ou sondagem permanente. Sem rede, manter o estado verdadeiro e
explicar o que continua aplicado e o que ainda não pode ser restaurado.

Testar boot com WLAN presente, ausente e economia Samsung ativa; autorização expirada ou
revogada; processo/ponte mortos no meio do bootstrap; e restauração dos ajustes temporários.
Se for necessário refazer o pareamento, a operação precisa ser utilizável no relógio para
cumprir a autonomia prometida. Parada intencional não pode provocar reinício pelo watchdog.
Verificar também parada forçada e recuperação após abertura explícita.

Desligar watchdog e atualização periódica por padrão e confirmar que não sobraram tarefas
ou supervisores recorrentes. O código do Shevery configura watchdog habilitado por padrão e
NightDog com verificações de 60 segundos; isso não prova despertar de CPU a cada minuto.
Medir o ciclo de vida efetivamente usado, inclusive quando a ponte fica residente.

**Portão 2:** recuperação local após boot e restauração das três classes de mudança provadas
no Watch8. Se não for possível sem root sob as condições aceitas, manter controles indisponíveis
e registrar o impedimento; não anunciar o novo modo como entregue.

## 3. Intervenções e entrega no app

### Primeira ação prática — reaplicar o econômico habitual

Prioridade acrescentada em 22/10/2026: o usuário relata perda de sua configuração ao reiniciar
ou sair de Só relógio. Antes das restrições de apps, qualificar uma ação manual **“Aplicar
econômico”**: ligar a economia Samsung e desligar Bluetooth; Wi-Fi desligado como opção separada.
Reaplicar define estados, sem inverter toggles. Usar a mesma sessão e o backend aprovado,
sem criar fork ou serviço de vigilância apenas para esse atalho.

A [análise de custo e viabilidade](../referencias/acao-economica-rapida.md) separa esse ganho
de conveniência de economia adicional. A API pública atual não permite desligar BT; controles
shell e equivalência com a economia Samsung continuam sujeitos a prova física. Não assumir
que `WRITE_SECURE_SETTINGS`, escrita de chave ou exit code zero comprovem as duas ações.

Entregar o botão após comprovar aplicação/restauração, alarme e redução de esforço frente ao
melhor caminho nativo, incluindo bootstrap quando necessário. Esse aceite é independente do
ganho das restrições avançadas no portão 4; não libera o modo completo por antecipação.
Manter o acesso Home/Só relógio e a confirmação Samsung existentes.

Reaplicação pós-boot será opção posterior, pelo evento de desbloqueio e dentro da recuperação
limitada já definida; sem observação permanente de BT/Wi-Fi. Confirmar se sair de Só relógio
gera reboot nesse firmware. O usuário informou rede móvel em **Sempre desligado**; preservar
esse estado na referência e confirmar na coleta, sem tratá-lo como ajuste novo do app.

### Restrições adicionais

Investigar nesta ordem, uma intervenção por vez, sempre contra a referência econômica atual:

1. **Gemini:** identificar o pacote que hospeda o assistente, seus gatilhos e atividade.
   Comparar gatilhos nativos desligados; depois AppOps/restrição ou desativação temporária
   do pacote real, como experimentos separados.
2. **Samsung Health:** observar coleta, sensores, sincronização e “Limitar funções de saúde”.
   Testar restrição do componente opcional identificado, sem presumir que desativar a UI
   interrompe toda a coleta nem comprometer dependências do alarme.
3. **Outros componentes opcionais:** selecionar por atividade residual observada e revisar
   dependências/UID compartilhado. Proteger alarme, System UI, hora, inicialização, acesso
   à ponte e recuperação; nenhuma lista genérica de pacotes a desativar.
4. **Processos em cache:** testar limite menor, inclusive zero, separadamente e por último.
   Verificar permissão/API efetiva e restauração; comparar recriações, CPU e despertares.
   O limite também pode matar o cliente em cache; aplicar em primeiro plano e testar recuperação.

Para cada controle: registrar valor bruto anterior, escrever por identidade shell, confirmar
por API/serviço independente e comportamento, observar a UI pertinente e restaurar. Reler a
mesma chave não basta. Para pacote desativado, confirmar PackageManager e ausência de atividade
esperada; ao restaurar, confirmar habilitação exata e reabertura. Interromper Health pode
perder medições do intervalo; restaurar configuração não recria dados perdidos nem processos
anteriores. Não limpar dados dos apps. Economia padrão, se investigada, inclui recarga/reboot.

Identificar primeiro o pacote nativo de alarme (`com.samsung.android.watch.alarm` foi observado),
UID/usuário, dependências e comportamento de referência. Confirmar alarme local audível/vibratório
com tela apagada, em repouso prolongado, depois de reboot e depois de restaurar. Cobrir alarme
único e recorrente, sem telefone/rede, respeitando o comportamento nativo antes do desbloqueio.
Qualquer falha atribuível à intervenção impede sua promoção. Testes curtos não provam autonomia.

**Portão 3:** aplicação, conflito, restauração e alarmes passam por controle isolado. Testar
erro parcial, crash antes/depois da escrita, reaplicação, edição manual, permissão revogada,
mudança de pacote e concorrência entre ações. Consolidar só controles aprovados no perfil.

Migrar para o app principal apenas após restaurar totalmente a sessão de `:advancedLab`;
nunca dois apps controlando os mesmos alvos. Não copiar automaticamente diário entre app IDs
ou assinaturas. O app principal passa a ser o único proprietário da sessão de produção.

Disponibilizar pelo **tile → Economia**: “Ativar Relógio + alarmes”, “Restaurar perfil”,
“Diagnóstico” e “Recuperar acesso”. Mostrar o que será interrompido e estados distintos de
disponível, aplicado/parcial, conflito, permissão ausente e recuperação pendente. Preservar
a única entrada launcher `ShortcutActivity`, afinidade, duplo clique Home, tile “Só relógio”
e complicação. Não ativar o perfil ao abrir o atalho Samsung.

## 4. Validação funcional, visual e de autonomia

Revisar funcionalidade e acabamento em cada etapa: tela circular de 192 dp, fontes 1,0/1,3,
rolagem/coroa, Voltar, alvos de toque, textos com acentuação e mensagens de erro alcançáveis.
Testar UI do cliente **e** pareamento/autorização/recuperação do gerenciador; não basta a
tela principal caber. Separar inspeção de código, prova emulada e validação Samsung.

Executar build/lint/testes focados; conferir manifesto final, autorização, ausência de
rede/sensores/serviços periódicos inesperados e processos restantes. Repetir os três atalhos
após reboot, morte normal e parada forçada com reabertura. Examinar diff, histórico, contratos
com a ponte e lacunas antes de cada commit/release.

### Protocolo R/P/E do novo marco

O [estudo original A/B/C](../validation.md#medição-de-autonomia) continua histórico e separado.
Neste marco, manter **o mesmo APK principal assinado instalado nas três condições**, inclusive
na referência, para não apagar sessões ou confundir instalação com efeito do perfil:

| Condição | Configuração |
|---|---|
| R — referência | Uso econômico habitual fixado, nenhum controle novo, ponte parada e acesso de depuração desligado quando não necessário. |
| P — custo da ponte | Mesma referência, ponte disponível sem controles novos; registrar processos, depuração exigida e política de recuperação. Comparação exploratória R/P antes do perfil. |
| E — modo econômico | Controles aprovados e ciclo de vida real da ponte: encerrada se os controles persistirem, disponível sob demanda ou residente se necessário. Todo custo necessário entra na condição. |

Manter iguais firmware, mostrador, uso e estabilização após recarga. Conexões, saúde, brilho
e gesto permanecem iguais, exceto pelas intervenções declaradas em E; registrar seus estados
original e aplicado. Sem PC ADB conectado; não exigir desligar uma opção de depuração indispensável
ao modo e omitir seu custo. Registrar a diferença para R. Não coletar continuamente para medir.

Comparação final: **três pares válidos R/E de 24 horas**, na ordem R/E, E/R, R/E. Registrar
percentual e tempo real no início/fim, uso e incidentes. Recarga, reboot, atualização,
instalação ou mudança relevante dentro da janela a invalidam; repetir o par inteiro, sem
selecionar apenas resultados favoráveis. O leitor atual detecta alguns problemas nos extremos,
mas pode não identificar uma recarga intermediária: exigir confirmação do usuário e descartar
a janela quando houver dúvida. Reset de contadores é perda de comparabilidade.

Calcular `r = (percentual inicial - percentual final) / horas`, em pontos percentuais/hora.
Para o par i, `delta_i = r_R - r_E` e `q_i = 1/T_R + 1/T_E`, com T em horas. `q_i` considera
incerteza de até 1 ponto percentual na descarga de cada janela por resolução dos extremos.
Esse limite trata somente da quantização; não corrige erro do estimador de bateria do firmware.
Declarar consistência observada somente se **os três deltas forem positivos e maiores que
q_i**, com rotina equivalente e dispersão apresentada. Não é teste de significância nem prova
de ganho universal; não há piso percentual de economia imposto ao usuário.

Se as diferenças forem positivas mas não superarem a resolução, executar **uma rodada adicional
completa de três pares de 48 horas**. Se o resultado continuar pequeno, misto ou não comparável,
reportar **inconclusivo**, sem prolongar indefinidamente. Resultado negativo impede anunciar
ganho. Testes de reboot ficam fora dessas janelas; medir e informar separadamente o custo
pontual de bootstrap/recuperação, sem afirmar que o ensaio sem reboot o amortizou.

**Portão 4:** perfil funcional, restaurável e resultado R/E consistente. A integração pode ser
tecnicamente válida e a meta de economia continuar pendente/inconclusiva. Não trocar esse aceite
por “há menos cartões nos recentes”, “RAM livre” ou uma leitura curta de batterystats.

## 5. GitHub, rollout e critérios de aceite

Código, plano, revisão, resumo de evidências e limitações no repositório público WatchReserve.
Criar WatchReserve-Bridge somente se o critério de fork for atingido; registrar upstream,
SHA, licenças, identidade, dependências fixadas e CI separada com permissões de leitura.
Chave persistente da ponte com backup privado próprio; preservar a identidade de assinatura
do app principal. Publicar APKs assinados, checksums e tags, distinguindo AVD de prova física.

Entregar em etapas verificadas: cliente de leitura → recuperação/sessão → controles isolados
→ integração do modo → comparação de autonomia. Commits PT-BR com escopo e corpo; não publicar
controle experimental como validado. Uma falha de portão mantém o modo indisponível e a meta
aberta, com causa e próximo dado necessário. Não substituir o Shevery do usuário ou desinstalar
lab/ponte enquanto houver sessão pendente. Sem atualização de firmware durante experimentos.

Rollback: restaurar perfil, resolver conflitos, encerrar recuperação temporária e, se desejado,
desabilitar integração. Só então remover a ponte ou retornar à versão anterior do APK. Não
fazer downgrade com diário incompatível ou estado ainda aplicado. Atualizar evidências por
modelo/firmware; compatibilidade de outros Samsung Wear OS será por capacidade e prova própria.

### Funcionalidade complementar — pressão longa em Voltar

Pedido acrescentado em 22/10/2026: permitir reutilizar o gesto hoje associado ao Samsung Pay.
A [pesquisa e comparação de soluções](../referencias/remapeamento-botao-voltar.md) recomenda
testar primeiro Wear Button Remapper, sucessor do SPayRemap. Não depende do Shevery para
receber teclas. Não instalar ou desativar pacotes como parte automática da preparação da ponte.

Se a solução pronta atender ao uso e ao consumo, manter integração por atalho/documentação.
Considerar implementação própria somente se houver vantagem concreta: ações internas,
configuração mais simples ou permissões/custo menores. Seria recurso opcional por eventos,
desativado inicialmente, com teste físico e restauração independente. Preservar toque curto,
alarme, Home e confirmação Samsung; não usar leitura contínua de logs. Registrar o serviço
adicional no protocolo de autonomia. Essa investigação não substitui nem bloqueia os quatro
portões do modo Relógio + alarmes, cuja implementação permanece pendente.

## Critérios de Aceite

- [x] Estado publicado e limites conhecidos registrados; atalhos existentes preservados no plano.
- [ ] Cliente real autorizado executa leitura como shell e encerra serviço no AVD/Watch8.
- [ ] Coleta local sob economia Samsung distingue política efetiva e atividade residual.
- [ ] Ponte escolhida tem contrato e identidade verificados; fork só com necessidade documentada.
- [ ] Preparação, recuperação temporária e perfil possuem restauração própria, serializada e persistente.
- [ ] Reboot com/sem rede e economia ligada tem recuperação local utilizável após desbloqueio.
- [ ] Watchdog/trabalho periódico não necessário ausente; custo residente necessário medido.
- [ ] Ação pontual economia Samsung ligada + BT desligado comprovada, restaurável e com menos esforço.
- [ ] Cada controle promovido passa por aplicação, verificação independente, conflito e restauração.
- [ ] Alarme nativo e hora preservados em repouso, reboot e restauração, sem depender da ponte.
- [ ] Modo manual integrado pelo painel, sem regressão dos três acessos Samsung.
- [ ] Revisões funcional e de acabamento realizadas e evidenciadas em fontes 1,0/1,3.
- [ ] Três comparações R/E sustentam ganho consistente; caso contrário, meta permanece inconclusiva.
- [ ] Build, lint, testes focados, bug sweep, evidências revisadas e APK assinado publicados.

Revisão do plano: **APROVADO para execução sequenciada**, conforme auditoria vinculada.
Não declara nenhum controle, recuperação Samsung ou ganho de bateria já comprovado.
