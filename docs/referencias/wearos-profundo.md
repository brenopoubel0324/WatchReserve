# Investigação profunda — Wear OS, Samsung e alarmes

Pesquisa em 21/10/2026. O usuário quer investigar ganho adicional sobre a economia Samsung
que já utiliza; organizar os mesmos ajustes em perfis não atende sozinho a esse objetivo.
A função indispensável informada é **alarme**. O alvo passa a ser um possível modo
“relógio + alarmes”, sem root e com acionamento manual. Ainda não é um recurso funcional.

## Evidência real disponível

Referência física: SM-L325F, Android 16/API 36, firmware L325FXXS3AZF4; pacote Samsung de
bateria 4.0.01.1. Foi reanalisado localmente o diagnóstico privado da sessão anterior e o
APK Samsung já copiado. Não houve nova conexão, instalação, grant ou escrita no relógio.
Binários, disassembly, dumps, horários de alarmes e identificadores não são publicados.

O dump foi obtido com economia **desligada** (`mPolicyLevel=0`). Ele distingue a política
completa configurada da política efetiva naquele instante. A política completa, tanto
`default full` como `current full`, já declara:

| Campo | Valor configurado |
|---|---|
| `enable_quick_doze` | `true` |
| `force_all_apps_standby` | `true` |
| `force_background_check` | `true` |
| `disable_launch_boost` | `true` |
| `disable_optional_sensors` | `true` |
| `disable_aod` | `true` |
| `enable_firewall` | `true` |
| `defer_full_backup` / `defer_keyvalue_backup` | `true` / `true` |
| `location_mode` | `1` |
| `soundtrigger_mode` | `1` |
| `enable_datasaver` | `false` |
| `disable_vibration` / `disable_animation` | `false` / `false` |
| `adjust_brightness_factor` | `0.9` |

**Consequência:** oferecer Quick Doze ou bloqueio de boost como novidade seria enganoso.
É preciso observar a economia Samsung ligada para confirmar a política efetiva, os estados
do DeviceIdle e o comportamento no pulso. Um campo configurado não prova suspensão da CPU.

O controlador de Low Power Standby informa `mSupportedConfig=false`, além de desabilitado.
Esse mecanismo específico não é candidato funcional neste firmware. Não confundir suporte
ausente com um interruptor simplesmente desligado. Isso também não significa ausência de
Doze, suspend ou dos mecanismos próprios Samsung.

O dump instantâneo tinha zero wake locks. Isso não identifica o consumo ao longo do dia nem
prova que não existam despertares periódicos. Não foi coletada uma janela de Batterystats.

Na análise estática privada do APK Samsung, o controle nativo de rede escreve
`low_power_back_data_off` em Global e `sm_connectivity_disable` em Secure. Também há controle
de `restricted_device_performance`, com escrita inteira de 0/1 nos caminhos inspecionados.
Não foi identificado ali um controle de frequência arbitrária. São mecanismos da economia
nativa, sem prova de ganho
adicional ao reescrever esses valores. Os recursos do APK incluem “Limit health features”;
não foi confirmado seu estado no relógio. Isso precisa entrar na referência antes de atribuir
novidade a desligamentos de saúde. A documentação Samsung consultada descreve a opção para
Watch7/Ultra, sem servir como validação física automática do Watch8. [Samsung][samsung]

## Caminhos mais profundos

| Caminho | O que acrescentaria | Acesso e evidência | Decisão |
|---|---|---|---|
| Diagnóstico por CPU, wake locks, alarmes de despertar, jobs e rádio | Encontrar atividade residual que a configuração atual não eliminou | ADB somente leitura. Investigar depois leitura pelo UID do APK com `DUMP`/`BATTERY_STATS`, que têm flag `development` no AOSP; a Samsung ainda precisa permitir a concessão e a leitura. | Primeiro incremento. Sem monitoramento contínuo. |
| Restrição seletiva por aplicativo | Impedir trabalho de apps opcionais que ainda executem em segundo plano | AppOps `RUN_ANY_IN_BACKGROUND` e buckets de standby são mecanismos Android. Exigem shell ou outro acesso apropriado; `WRITE_SECURE_SETTINGS` não concede gestão de AppOps. | Candidato somente se os contadores mostrarem atividade residual. |
| Desativação temporária de pacotes opcionais | Interromper também componentes ativos que escapem de restrições de segundo plano | Package Manager oferece `disable-user` e restauração ao estado anterior. App comum não recebe esse poder com `WRITE_SECURE_SETTINGS`. | Experimento por pacote, com dependências e UID revisados; sem listas genéricas de debloat. Não desinstalar. |
| Doze profundo e transições enquanto está no pulso | Investigar por que o relógio permanece ativo apesar de Quick Doze configurado | DeviceIdle possui estados próprios, manutenção, allowlists e integração com detecção de corpo. Forçar idle é comando de teste protegido por `DEVICE_POWER`, não uma simples escrita de chave. | Prova limitada e restaurável, após diagnóstico e teste do alarme. Não transformar comando de teste em modo permanente. |
| Freezer de apps em cache | Reduzir execução residual de processos elegíveis em cache | AOSP lê `cached_apps_freezer`/`use_freezer` e verifica suporte nativo. Não congela indiscriminadamente serviços ativos ou apps essenciais. | Primeiro verificar se já está ligado e se há processos elegíveis; pode não acrescentar nada. |
| Scans e reconexões de rádio | Eliminar despertares mesmo sem conexão útil | Investigar permissões, flags e contadores efetivos no firmware, comparando com a economia nativa e rádios realmente desligados. | Condicional a atividade medida. Desligar um rádio pode interromper o próprio ADB sem fio. |
| Low Power Standby | Ignorar wake locks e rede em outra política de standby | Suporte declarado falso na unidade analisada. | Descartado para este firmware sem alteração do sistema. |
| Underclock, undervolt e controle de núcleos/driver | Alterar política de hardware além da interface nativa | Não há acesso comprovado por APK comum ou `WRITE_SECURE_SETTINGS`; antigas chaves `cpufreq-i`/`cpufreq-n` aparecem como obsoletas no AOSP. | Fora do escopo sem root; não oferecer como recurso. |

Fontes: [política de economia][policy], [DeviceIdle][idle], [restrições de segundo plano][background],
[Package Manager][pm], [freezer][freezer] e [permissões][permissions]. A presença no AOSP não
confirma exposição ou funcionamento no Samsung. O commit Android 16 consultado é
`99b01a65cc4c104933788b3143285ab6bae65827`; não é o código proprietário exato do firmware.

## Limite de permissões e arquitetura

O AOSP distingue permissões concedíveis em desenvolvimento de permissões de sistema.
`WRITE_SECURE_SETTINGS`, `DUMP` e `BATTERY_STATS` incluem `development`; ainda exigem prova
no aparelho. `DEVICE_POWER`, `CHANGE_COMPONENT_ENABLED_STATE` e `MANAGE_APP_OPS_MODES` não
incluem essa flag no manifesto consultado. Conceder a primeira não transforma o app em shell.

Um experimento via PC pode mostrar benefício sem que o APK consiga aplicar/restaurar sozinho.
Essa diferença deve aparecer no resultado. Uma ponte como Shizuku introduz outro processo,
precisa de validação no Wear OS e, sem root, normalmente exige nova inicialização após reinício.
Não será incluída automaticamente nem tratada como consumo zero. [Manual do Shizuku][shizuku]

Pacotes com UID compartilhado não são unidades independentes de isolamento. Não restringir
UIDs de sistema, o alarme, a interface, os serviços necessários à hora e suas dependências
por uma lista presumida. O contrato do plano continua exigindo sessão persistente, restauração
do estado original e preservação de mudanças posteriores do usuário.

## Alarme é critério de aceite

Android documenta que `setAlarmClock()` continua disparando no Doze, enquanto alarmes comuns
podem ser adiados. Isso **não prova** a API usada pela versão Samsung nem o som/vibração no
relógio. A fonte AOSP também prevê exceção de vibração para uso `ALARM`, sem garantir o mesmo
comportamento do firmware. [Doze e alarmes][doze], [vibração][vibration]

Antes de liberar qualquer restrição: identificar o pacote e UID do alarme nativo; testar um
alarme separado com a configuração atual e novamente com cada candidato isolado, tela apagada
e relógio no pulso. Verificar som/vibração escolhidos, horário, soneca, dispensa e retorno da
interface; depois testar noite, reinício e restauração. Não apagar nem modificar os alarmes
existentes. Alarme espelhado do celular depende de conectividade; primeiro confirmar se o uso
pretendido é um alarme criado localmente no relógio.

## Execução proposta e revisão

Achados prévios pela revisão de execução já adotada no projeto:

| Severidade | Lacuna | Correção no plano |
|---|---|---|
| Alta | Chamar flags já configuradas de economia adicional | Registrar referência com economia nativa ligada e exigir diferença observada. |
| Alta | Supor que um grant permite AppOps, pacotes ou DeviceIdle | Separar APK comum, permissões de desenvolvimento e shell; provar cada operação pelo chamador real. |
| Alta | Preservação de alarmes sem teste físico | Fazer o alarme nativo ser critério de aceite de cada restrição; sem aprovação por API AOSP apenas. |
| Alta | Lista genérica de pacotes pode atingir UID/dependências essenciais | Selecionar um candidato por evidência, com estado anterior e recuperação documentados. |
| Média | Dump com economia desligada confundido com referência diária | Rotular o estado observado e coletar novamente com a configuração real do usuário. |
| Alta | `batterystats --checkin` pode apagar o arquivo histórico após consumi-lo | Usar `--charged`, sem reset, simulação ou flag de check-in; manter teste de comandos de leitura permitidos. |

1. Ampliar a coleta existente com opção privada de energia: power, DeviceIdle, Batterystats,
   alarmes, jobs, configuração de processos/freezer e pacote do alarme. Consultas de leitura;
   nenhum grant, reset de estatísticas, bateria simulada, force-idle ou alteração de pacote.
2. Testar o coletor com ADB simulado em PowerShell 7 e 5.1, incluindo recusa sem opt-in privado,
   falha isolada/timeout de serviço e garantia de não executar comandos de escrita.
3. Na próxima sessão física, coletar a configuração econômica real. Usar duas leituras de
   contadores em janela com depuração desligada entre elas; rejeitar recarga/reset/atualização
   que invalide a comparação. Traces curtos só quando necessários, fora da medição de autonomia.
4. Escolher a primeira intervenção pelos contadores. Preparar a prova isolada e a recuperação,
   testar o alarme e comparar com a referência. Não construir novos perfis de interface antes
   de encontrar um controle útil e acessível.

**APROVADO para preparar a coleta somente leitura e analisar os resultados.** As intervenções
continuam hipóteses com dependências físicas explícitas; nenhum comando de alteração foi
executado nesta pesquisa e nenhum ganho incremental foi medido. A [medição pareada](../validation.md)
continua necessária depois da prova funcional.

Coletor preparado com `-CapturePower`: exige `-IncludePrivateDetails`, serial explícito e
recurso Android de relógio. Falhas de serviço, saída negada e timeout parcial ficam distintos
de ausência de consumo. A coleta continua local e não modifica o APK publicado. O uso de
`--charged` foi confirmado contra [BatteryStatsService no Android 16][batterystats].

Priorizar contadores do sistema e tracing suportado. Battery Historian não é mais mantido
ativamente segundo o Android; não torná-lo dependência nova do projeto. [Android][historian]

Validação desta entrega: testes do coletor com ADB simulado aprovados em PowerShell 7.6 e
Windows PowerShell 5.1; parser sem erros e `git diff --check` aprovado. Cobertos opt-in,
serial explícito, recusa de alvo sem recurso de relógio, ACL, ausência de dados privados no
console, falha isolada, permissão negada e timeout parcial. Nenhum teste físico desta nova
coleta ou teste de alteração de energia foi executado. Código Android e APK permanecem iguais.

Bug sweep: diff, estado do Git, histórico recente, contratos do diagnóstico, comandos de
leitura e privacidade revisados. Achados corrigidos: evitar `--checkin`, exigir seleção do
relógio e distinguir consulta parcial de ausência de consumo. Sem achados bloqueantes ou
altos remanescentes na coleta; ganho adicional e preservação do alarme sob restrições seguem
dependendo de provas físicas.

[policy]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/power/batterysaver/BatterySaverPolicy.java
[idle]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/apex/jobscheduler/service/java/com/android/server/DeviceIdleController.java
[permissions]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/core/res/AndroidManifest.xml
[pm]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/pm/PackageManagerShellCommand.java
[freezer]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/am/CachedAppOptimizer.java
[vibration]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/vibrator/VibrationSettings.java
[background]: https://developer.android.com/topic/performance/background-optimization
[doze]: https://developer.android.com/training/monitoring-device-state/doze-standby
[historian]: https://developer.android.com/topic/performance/power/battery-historian
[samsung]: https://www.samsung.com/ae/support/mobile-devices/how-to-extend-battery-life-and-use-power-saving-modes-on-watch7-and-watch-ultra/
[shizuku]: https://shizuku.rikka.app/guide/setup/
[batterystats]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/am/BatteryStatsService.java
