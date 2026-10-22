# Revisão de execução — Relógio + alarmes

Data: **22/10/2026**. Skill: `execution-plan-review`. Escopo: plano conversacional do novo
marco, contratos atuais do WatchReserve e fontes dos candidatos a ponte. Base local:
`main`, commit `d7a48f0`, Git inicialmente limpo. Revisão realizada sem instalar, conceder
permissões, conectar ou alterar o Watch8 e sem implementar controles.

**Veredito do plano recebido: REVISAR.** Cinco achados altos e dois médios abaixo precisam
de correção antes da implementação. **Veredito da versão corrigida: APROVADO para execução
sequenciada**, com os portões do [plano canônico](../planos/2026-10-22-relogio-alarmes.md).
Essa aprovação avalia a execução proposta; não aprova uma release nem comprova economia.

## Comportamento desejado e limites

Modo manual que preserve hora/alarme nativo, reduza consumo além da referência econômica
atual e tenha aplicação/restauração no relógio, inclusive recuperação após reboot sem
PC/celular. Wi-Fi temporário em rede confiável e recuperação manual no relógio são aceitos.
Sem root, porcentagem mínima de ganho ou monitor permanente no app principal. Preservar
atalhos existentes; rever funcionalidade e acabamento juntos.

Confirmação Samsung acessível e três superfícies em uso normal são fatos físicos já registrados.
Shizuku oficial como shell com Wi-Fi desligado é uma prova emulada do gerenciador. Shevery
como backend do WatchReserve, recuperação Samsung e Health/Gemini como consumidores excessivos
ainda são hipóteses. Não elevar presença nos recentes a evidência de consumo.

## Achados, em ordem de severidade

| ID / severidade | Área e problema concreto | Correção incorporada |
|---|---|---|
| A1 — alto | “Leitura” misturava bootstrap com diagnóstico. `AdbArm.arm()` escreve `adb_enabled=1`, `adb_wifi_enabled=1` e `adb_allowed_connection_time=0`; restauração apenas do perfil deixaria acesso alterado. [Fonte][adb-arm] | Separar preparação, recuperação temporária e perfil; registrar antes da primeira escrita e restaurar prazo/rádios/depuração sob propriedade verificada. Bootstrap upstream só entra se essas mudanças forem controláveis. |
| A2 — alto | O candidato traz watchdog habilitado por padrão e NightDog configura verificações de 60 s; existe também worker periódico de atualização. Desligar um toggle não prova ausência de trabalho restante. [Configuração][module-settings], [NightDog][nightdog], [worker][update-worker] | Desabilitar/cancelar mecanismos desnecessários, verificar tarefas/processos e parada intencional, medir custo real. `postDelayed` não comprova despertar de CPU a cada minuto. |
| A3 — alto | Recuperação pós-reboot não definia desbloqueio, janela finita, autorização expirada ou rede ausente. O diário padrão é inacessível antes do primeiro desbloqueio; partida na mesma sessão não prova boot frio. [Direct Boot][direct-boot], [receiver][boot] | Reconciliar após desbloqueio, uma tentativa automática por boot limitada a 120 s, restauração dos ajustes temporários e recuperação manual utilizável no relógio. Alarme independente da ponte e comportamento nativo antes do desbloqueio preservado. |
| A4 — alto | Compilar Shevery na toolchain atual não é executável: fonte fixada usa Java 21/SDK 37, AGP alpha, plugins Kotlin beta e Gradle RC; o app usa Java 17/SDK 36. “Release estável” não implica build estável/reproduzido. [Build][build], [plugins][plugins], [manager][manager-build], [wrapper][wrapper] | Testar APK publicado primeiro; isolar toolchain/CI do eventual fork. Se fonte/dependências forem inviáveis, base oficial v13.6.0 fixada e backports mínimos com proveniência. Não migrar o app principal por arrasto. |
| A5 — alto | Sessão atômica não serializa UI/boot/IPC; crash entre escrita e confirmação e migração lab→app poderiam perder propriedade. Reativar pacote não recria processos ou dados de saúde do intervalo. [Laboratório atual](../laboratory.md) | Controlador único, intenção persistida antes da escrita, reconciliação por leitura, estados brutos preservados e conflitos explícitos. Restaurar lab antes de migrar; verificar efeito/restauração por controle e proteger alarme/recuperação. Cache zero por último. |
| M1 — médio | “Ganho consistente” não possuía regra operacional; A/B/C e depuração sempre desligada conflitam com uma ponte necessária. Leitor de extremos pode perder recarga intermediária. [Protocolo anterior](../validation.md), [leitor](../../app/src/main/java/dev/brenopoubel/watch8battery/core/BatteryMeasurement.kt) | R/P/E com mesmo APK instalado, custo necessário incluído e três pares R/E de 24 h. Exigir deltas positivos além da resolução dos percentuais, registrar dispersão e invalidar incidentes. Uma rodada adicional de 48 h se limitada pela resolução; depois inconclusivo. |
| M2 — médio | Identidade do fork e API não são intercambiáveis por nome: Shevery usa app ID próprio e declara API 14.4.0, cliente oficial Maven 13.1.5. Providers, permissões e callbacks precisam ser testados. [Manifesto][manifest], [versão API][api-version], [provider][provider], [Maven][maven] | Qualificar pacote/certificado/protocolo/autorização real/UID; uma ponte ativa. Adaptar cliente/provider se necessário e renomear contratos em conjunto no fork, preservando licenças. Não substituir automaticamente a instalação do usuário. |

## Mapeamento ao repositório

- `app/build.gradle.kts`, `build.gradle.kts` e wrapper: API 30 mínima/36 alvo, JDK 17,
  AGP 8.13.2, Kotlin 2.2.21 e Gradle 8.13. Não existe dependência Shizuku no app publicado.
- `settings.gradle.kts`: somente `:app` e `:lab`; `:advancedLab` é proposta, não módulo existente.
- `:lab`: sessão AtomicFile e recuperação da fixture de AVD; recusa relógio físico.
  Não é controlador de produção com conflitos/boot/múltiplos acionadores.
- `BatteryMeasurement.kt`/`DeviceBattery.kt`: medições de extremos, boot/relógio/recarga em
  parte detectáveis. Não há prova de ausência de carga intermediária nem diário de perfil.
- UI atual: launcher/Home usam `ShortcutActivity`; tile “Economia” abre o painel.
  A nova UI deve entrar pelo painel, preservando afinidade e rotas comprovadas.
- Evidências [Watch8](../evidencias/2026-10-21-watch8/README.md) e
  [Shizuku AVD](../evidencias/2026-10-21-shizuku-wearos5/README.md) têm limites diferentes.
  O APK original testado não é Shevery nem um cliente WatchReserve autorizado.

## Validações mínimas e portões

1. Cliente somente leitura autorizado como shell; negação/revogação, morte do Binder,
   timeout e encerramento. AVD primeiro, depois coleta local em economia Samsung.
2. Bootstrap e três registros restauráveis, concorrência/crash e recuperação local após
   boot com rede presente/ausente, economia ativa, credenciais expiradas e parada intencional.
3. Controles isolados, verificação independente, conflito/restauração e alarme nativo em
   repouso/reboot/restauração. Só depois migrar sessão para app principal e rever a UI.
4. Build/lint/testes pertinentes e revisão funcional/visual em 192 dp e fontes 1,0/1,3;
   atalhos sem regressão. R/P exploratório, R/E final, custo necessário e dispersão publicados.

Se uma etapa falhar, não ativar a seguinte nem marcar o modo entregue. Inconclusivo é
resultado permitido para o experimento, mas não satisfaz a meta de ganho consistente.

## Riscos residuais e pendências externas

Compatibilidade do cliente oficial com Shevery, autorização/pareamento circulares, retomada
com economia Samsung e disponibilidade do bootstrap sem root dependem de testes. Ainda falta
identificar versão/dispositivo do Shevery utilizado pelo usuário. Nenhum comando shell é
tratado como irrestrito: SELinux, permissões e proteções Samsung continuam aplicáveis.

Desativar Health pode interromper medições aceitas pelo usuário; restaurar não recupera esse
histórico. Alarme nativo deve ser testado, inclusive sem a ponte. Menos processos pode aumentar
recriações e gasto. Não há ganho medido nem garantia para outros modelos/firmwares.

## Verificação desta entrega documental

Escopo: novo plano/auditoria, links e indicação de prioridade nos documentos anteriores;
protocolo original preservado como estudo separado. Revisão funcional do plano e acabamento
textual verificam clareza dos estados, acentuação, critérios pendentes e distinção entre fato
e hipótese. Não houve revisão visual de UI nova, pois ela ainda não existe.

Checks: 24 arquivos Markdown em UTF-8 estrito, 140 links locais/âncoras, estrutura do plano,
`git diff --check`, revisão integral
do diff/status/histórico e bug sweep. Build, lint e testes Android não repetidos: nenhum
código, manifesto, dependência ou configuração de build foi alterado. Não há APK/release nova.

## Fontes fixadas

Shevery 14.4.0: commit `20e0174836b0e78847c83aef9c27521c65fa61fb`.
Metadados da release/fonte foram consultados; seu APK não foi executado nesta revisão.
Shizuku original v13.6.0: commit `2650830c5b099ae0dd34fedf614d4f592ca05d65`.
O [estudo anterior](../referencias/shizuku-wearos.md) distingue APK testado e snapshot inspecionado.

[adb-arm]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/src/main/java/moe/shizuku/manager/adb/AdbArm.kt
[module-settings]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/src/main/java/moe/shizuku/manager/module/ModuleSettings.kt
[nightdog]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/nightdog/src/main/java/rikka/shizuku/nightdog/NightDog.kt
[update-worker]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/src/main/java/moe/shizuku/manager/module/update/SheveryAutoUpdateWorker.kt
[direct-boot]: https://developer.android.com/privacy-and-security/direct-boot
[boot]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/src/main/java/moe/shizuku/manager/receiver/BootCompleteReceiver.kt
[build]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/build.gradle
[plugins]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/settings.gradle
[manager-build]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/build.gradle
[wrapper]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/gradle/wrapper/gradle-wrapper.properties
[manifest]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/manager/src/main/AndroidManifest.xml
[api-version]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/api/manifest.gradle
[provider]: https://github.com/HmnDev-Tech/shevery/blob/20e0174836b0e78847c83aef9c27521c65fa61fb/api/provider/src/main/java/rikka/shizuku/ShizukuProvider.java
[maven]: https://repo.maven.apache.org/maven2/dev/rikka/shizuku/api/maven-metadata.xml
