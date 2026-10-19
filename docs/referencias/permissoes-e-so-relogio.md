# Permissões avançadas e “Só relógio”

Pesquisa em **19/10/2026**, solicitada pelo usuário após a v0.2.1. Escopo: ações reais de economia sem root, possibilidade de acesso/ativação de “Só relógio” e explicação da funcionalidade já entregue. Foram consultados documentação Android/Samsung, código AOSP e dois projetos de referência. Nenhum código externo foi incorporado ou executado e nenhuma permissão foi concedida nesta etapa.

## Resultado

Há fundamentos técnicos para investigar controles diretos com permissões concedidas por ADB. Isso inclui configurar a política do modo de economia, além de ajustes de tela. A v0.2.1 ainda não implementa esses controles; a central de atalhos e a medição não cumprem sozinhas o objetivo de reduzir consumo.

Existência no Android, implementação em outro app e relato em outro Galaxy Watch são evidências diferentes de funcionamento no Watch8. O próximo incremento deve comprovar aplicação e restauração antes de disponibilizar os ajustes como funcionais. Nenhum ganho de autonomia foi medido nesta pesquisa.

## Permissões e possibilidades

| Candidato | Evidência | Limite |
|---|---|---|
| Brilho e tempo de tela | `Settings.System` documenta brilho, modo automático e `screen_off_timeout`; a escrita depende da autorização apropriada. | Confirmar `WRITE_SETTINGS`/AppOps e efeito na tela. Não tratar uma escrita de chave como prova; considerar brilho automático e limites do firmware. |
| Economia padrão | O DNDSync para Wear OS altera parâmetros globais e Samsung; o autor relata teste no Watch4. | Não comprova equivalência com o botão Samsung no Watch8. Testar estado por API independente, interface, recarga e reinício. |
| Política de economia mais restritiva | AOSP lê `battery_saver_constants`; Buoy usa essa interface. Há parâmetros de AOD, dados/segundo plano, localização e Quick Doze. | Investigar quais são efetivamente consumidos pelo firmware. Algumas restrições já podem estar ativas na configuração atual do usuário; reaplicá-las não constitui ganho adicional. |
| Localização global, AOD e gestos | São candidatos para descoberta por firmware, além de restrições dentro da política de economia. | Diferenciar o interruptor global da restrição aplicada apenas enquanto a economia está ativa. Não presumir nomes Samsung nem equivalência de chaves Android. |
| LTE e limitação de CPU | Precisam de investigação separada. | `WRITE_SECURE_SETTINGS` não oferece controle irrestrito do modem/kernel. Escrever um valor não comprova desligamento do rádio ou redução de frequência. |

`WRITE_SECURE_SETTINGS` é uma permissão protegida, definida com a categoria `development` no AOSP. Projetos pessoais a solicitam no manifesto e orientam concessão via ADB; o firmware e a configuração de segurança ainda podem impedir a concessão ou determinadas escritas. Ela não equivale a root. A versão pública atual do WatchReserve não solicita essa permissão, portanto não há um comando de concessão útil para a v0.2.1.

Fontes primárias: [permissões Android](https://developer.android.com/reference/android/Manifest.permission#WRITE_SECURE_SETTINGS), [definição de permissões no AOSP](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/AndroidManifest.xml), [Settings.System](https://developer.android.com/reference/android/provider/Settings.System), [BatterySaverPolicy](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/power/batterysaver/BatterySaverPolicy.java).

## Referências de implementação

- **Buoy**, ref `72b8e38b6089e38446ed80afed4d464eb11af7c0`: [BatterySaverManager](https://github.com/tytydraco/Buoy/blob/72b8e38b6089e38446ed80afed4d464eb11af7c0/app/src/main/java/com/draco/buoy/utils/BatterySaverManager.kt) escreve a política de economia e parâmetros de persistência do modo. Demonstra a técnica Android, sem prova Wear OS/Watch8. Seu reset para padrões não equivale a restaurar a configuração original do usuário; o WatchReserve precisa do registro de sessão previsto no plano.
- **DNDSync**, ref `6b279e5b3ffca9b927310f41b38618734bfaa668`: [serviço Wear](https://github.com/DreadedLama/dnd-sync/blob/6b279e5b3ffca9b927310f41b38618734bfaa668/wear/src/main/java/in/dreadedlama/dndsync/DNDSyncListenerService.kt) usa `low_power`, `restricted_device_performance`, `low_power_back_data_off` e `sm_connectivity_disable`; [manifesto](https://github.com/DreadedLama/dnd-sync/blob/6b279e5b3ffca9b927310f41b38618734bfaa668/wear/src/main/AndroidManifest.xml) solicita a permissão protegida. O [README](https://github.com/DreadedLama/dnd-sync/blob/6b279e5b3ffca9b927310f41b38618734bfaa668/README.md) limita o teste declarado ao Watch4. Ausência de uma chave e algumas exceções são tratadas como sucesso no código auxiliar desse projeto; esse retorno não serve como prova para o WatchReserve.

As referências sustentam uma investigação, não uma recomendação de instalar esses apps nem uma afirmação de economia. Serviços contínuos de sincronização do DNDSync ficam fora da preferência do usuário por acionamento manual.

## “Só relógio”: pesquisa e lacuna concreta

A [Samsung documenta](https://www.samsung.com/us/support/answer/ANS10002905/) a entrada pelas configurações de bateria, a escolha de “Watch only” e a confirmação “Turn on”. É diferente da economia padrão: a função mantém a exibição da hora e desabilita as funções inteligentes.

Nas fontes consultadas não foi encontrada uma API pública, um intent documentado ou um componente comprovado no Watch8 para ativar esse modo diretamente por um app de terceiros. Isso é um resultado negativo da pesquisa, não prova de impossibilidade. Os exemplos de `low_power` encontrados controlam economia padrão e não demonstram “Só relógio”.

A investigação específica precisa identificar no firmware real a tela e a operação acionada pelo botão nativo. Primeiro testar a abertura da confirmação pelo UID comum do APK; se apenas o shell conseguir, registrar essa diferença. Uma eventual ativação direta exige um experimento separado e não é comprovada por abrir a tela; nenhum comando presumido deve ser incluído como funcional. A confirmação nativa continua sendo o comportamento previsto na versão atual.

## O que a v0.2.1 faz

**Central de atalhos:** mostra a bateria atual e botões para abrir configurações de economia, tela, localização, conexões e Saúde. O usuário altera os ajustes nas telas do sistema. Abrir essas telas, por si só, não reduz consumo.

**Medição manual:** registra uma leitura inicial, depois calcula a diferença de bateria e o tempo transcorrido quando o usuário finaliza. Exemplo hipotético: 80% → 56% em 24 h resulta em 1 ponto percentual por hora. Não é um monitor contínuo nem identifica quais serviços consumiram energia; serve para comparar condições semelhantes. Recargas intermediárias podem passar despercebidas e exigem descarte manual.

## Próxima etapa de desenvolvimento

1. Prototipar os ajustes básicos em ambiente de teste: permissões negadas/concedidas/revogadas, estado efetivo por API independente e restauração. Manter o APK publicado sem escritas globais até comprovação.
2. Comparar a política efetiva de economia com a configuração de referência do usuário. Investigar `battery_saver_constants` por parâmetro, preservando valores e campos desconhecidos; não importar um perfil agressivo completo de terceiros.
3. Criar uma única sessão persistente antes das escritas e mostrar aplicação parcial, conflitos e restauração confirmada. Não sobrescrever a escolha manual posterior do usuário.
4. Validar os candidatos no Watch8 quando o usuário decidir conectar; registrar modelo/firmware e só então medir diferença de consumo em janelas equivalentes.
5. Investigar a confirmação Samsung em paralelo, separando abrir a tela de executar a ativação. Continuar a revisão funcional e de acabamento em cada incremento.

Restrições adicionais podem alterar notificações, localização ou outros recursos usados no cotidiano. O perfil deve explicar quais comportamentos muda e permitir escolher e restaurar cada ajuste. Não se assume que todo consumo do Wear OS é desnecessário ou controlável por esse app.

Bug sweep documental: hipóteses separadas de provas, refs verificadas, sem comandos de escrita executados, sem mudança de APK/permissões ou arquivos privados publicados. Testes Android não foram repetidos: esta entrega altera somente documentação.
