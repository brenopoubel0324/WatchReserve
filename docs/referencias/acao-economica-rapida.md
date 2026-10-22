# Reaplicar economia + desligar Bluetooth — custo e viabilidade

Pesquisa e decisão de escopo em **22/10/2026**. O usuário relata que reiniciar o Watch8 ou
sair de Só relógio reativa Bluetooth, desliga economia e, às vezes, reativa Wi-Fi. Deseja
reaplicar a configuração com menos toques. Relato aceito como sintoma; causa, sequência
exata e estado final ainda não foram capturados no firmware. Nenhuma alteração física nesta etapa.

## Decisão

**Vale priorizar uma ação manual “Aplicar econômico”**, aproveitando a ponte já planejada,
desde que o caminho completo reduza esforço. Destino explícito: economia Samsung ligada e
Bluetooth desligado; opção separada para Wi-Fi desligado. Repetir a ação mantém esses estados,
sem inverter um toggle nem substituir o registro original da sessão.

Essa ação recupera a configuração que o usuário já escolheu. Seu benefício imediato é
conveniência e evitar períodos fora dela; não demonstra economia adicional à mesma
configuração aplicada manualmente. O ganho líquido depende também do custo da ponte.

| Opção | Custo esperado e limite | Recomendação |
|---|---|---|
| Aplicação manual pontual | Cliente executa poucas operações e encerra; não exige observador permanente. Custo da ponte/recuperação continua desconhecido. | Primeiro recurso útil a validar depois da leitura e recuperação. Não criar fork apenas por esses dois comandos. |
| Reaplicar após boot/desbloqueio | Execução por evento, sem polling. Exige recuperar acesso, esperar o estado Samsung e verificar resultado. | Opção posterior, desativada inicialmente e sujeita às provas de boot. |
| Impedir qualquer reativação, o dia inteiro | Envolve observação recorrente, disputas com o sistema e escolhas manuais do usuário. | Não implementar como condição desse atalho. Sem watchdog para manter rádios desligados. |

Se cada uso exigir ligar Wi-Fi, iniciar ponte e repetir pareamento, a soma pode ser mais lenta
que os menus nativos. Nesse cenário não vale introduzir essa infraestrutura somente para
esse botão; primeiro resolver a recuperação compartilhada ou usar os controles Samsung.

## Limites técnicos comprovados nas fontes

O app atual tem target SDK 36. A API pública `BluetoothAdapter.disable()` falha para apps
com alvo 33+, salvo exceções de sistema/gestão. Conceder `BLUETOOTH_CONNECT` ou
`WRITE_SECURE_SETTINGS` não remove essa restrição. [Contrato público Android][bt-api]

O AOSP oferece comando shell de Bluetooth com `disable`, marcado como não restrito a root
no código consultado. É candidato por identidade shell via Shevery/Shizuku, não garantia
de acesso no Samsung. Não confundir escrever `bluetooth_on=0` com desligamento confirmado
do adaptador. [Implementação Bluetooth][bt-shell]

Para economia, `cmd power set-mode 1` chama o serviço de energia no AOSP Android 16.
O comando retorna zero sem propagar o booleano de sucesso da operação: exit code zero
não basta. Reler `low_power` também não comprova equivalência com o botão Samsung.
Exigir `PowerManager.isPowerSaveMode`, interface nativa e efeitos observados, inclusive
as opções Samsung de conectividade/desempenho. [Implementação do comando][power-shell],
[pesquisa do firmware](wearos-profundo.md).

No AVD usado anteriormente, economia AOSP foi declarada sem suporte. Essa imagem permite
verificar contratos/erros do cliente, mas não aprovar a economia Samsung. Não transformar
a escrita de `low_power`, `low_power_sticky` ou outras chaves em receita de persistência
sem prova. O [laboratório existente](../laboratory.md) documenta essa limitação.

## Fluxo proposto e reversão

1. Usuário aciona o botão; verificar backend e permissões. Se indisponível, informar
   recuperação necessária antes de tentar mudar rádios. Não prometer um toque incluindo
   um bootstrap ainda não resolvido.
2. Ler e persistir estado anterior, estado desejado e operação pendente na sessão única.
   Serializar com as outras ações; não abrir sessão independente conflitante.
3. Executar e verificar Bluetooth desligado e economia ligada pelo mecanismo aprovado no
   firmware. O ordenamento final deve tolerar a economia desligando Wi-Fi; chamadas locais
   e registro precisam continuar funcionando sem o ADB do PC. Wi-Fi explícito somente se
   escolhido, sem desativar LTE ou outros recursos silenciosamente.
4. Mostrar resultado por ajuste: confirmado, falhou ou pendente. Verificar por API e UI;
   não transformar sucesso parcial em “econômico aplicado”. Reaplicar é idempotente.
5. Encerrar serviço de operação e, quando possível, a ponte. Contabilizar eventual processo
   residente; medir antes de afirmar que seu custo é desprezível.
6. “Restaurar” usa os valores originais e preserva conflitos posteriores. Cobrir economia
   que já estava ativa, BT que já estava desligado, erro parcial, recarga, reboot e revogação.

Para “Lembrar e reaplicar após reinício”, manter preferência distinta do diário de restauração.
O usuário precisa habilitá-la; restaurar/desativar o perfil desarma a reaplicação. Usar evento
de boot/desbloqueio, uma execução limitada pela janela de recuperação de 120 s já definida,
sem loop para vencer o sistema. Só compartilhar o gatilho com a saída de Só relógio se essa
saída realmente produzir boot no firmware. Não inventar um broadcast Samsung não verificado.
[Eventos Android](https://developer.android.com/develop/background-work/background-tasks/broadcasts).

## Quando o custo compensa

Antes de desenvolver integração definitiva, provar as duas ações individualmente no Watch8,
com restauração, e depois pelo cliente real. Medir ações/tempo partindo do mesmo mostrador,
incluindo navegação até tile/painel e recuperação da ponte. Comparar com o melhor caminho
nativo disponível; não presumir que BT tenha ícone no painel rápido em todo firmware.

Aceite de conveniência: redução comprovada de ações sem piora relevante de tempo/confiabilidade,
sem perda de alarme e sem processo de vigilância. Pode ser entregue antes do modo avançado
completo, mas depende das provas de aplicação/restauração; não depende de afirmar ganho novo.
Se não reduzir esforço, manter como experimento e não promovê-lo como melhoria.

Para autonomia, separar três perguntas: custo de aplicar o botão, custo de manter a ponte e
tempo evitado com a configuração indesejada. Comparar o mesmo estado econômico, aplicado
manualmente versus pelo app, para medir o custo da ferramenta. O protocolo R/P/E do plano
continua responsável pelo ganho das restrições avançadas; não atribuir ao novo botão os
três dias já obtidos pelo usuário.

O usuário informou em 22/10/2026 que já utiliza rede móvel em **Sempre desligado**.
Preservar essa referência e conferir o estado efetivo na coleta; não introduzir LTE como
alteração nova ou atribuir a ele um ganho que já fazia parte do uso. Isso reduz o possível
confundimento de trocar Bluetooth por rede móvel. Ainda registrar Wi-Fi e sincronizações:
desligar BT não demonstra, sozinho, qual trabalho foi eliminado. [Orientação Samsung sobre
conectividade remota e bateria][samsung-battery]

Entrega desta pesquisa: documentação e prioridade atualizadas; sem implementação, grant,
conexão ao Watch8 ou medição nova. Bug sweep de fontes, contratos, links e diff; build/lint/testes
Android não repetidos porque código e configuração de build não mudaram.

[bt-api]: https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#disable()
[bt-shell]: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/0f98ddca48c98e78ba2720ab25c43d02c87a5027/service/java/com/android/server/bluetooth/BluetoothShellCommand.java
[power-shell]: https://github.com/aosp-mirror/platform_frameworks_base/blob/99b01a65cc4c104933788b3143285ab6bae65827/services/core/java/com/android/server/power/PowerManagerShellCommand.java
[samsung-battery]: https://www.samsung.com/uk/support/mobile-devices/reduced-battery-life-on-your-samsung-galaxy-watch/
