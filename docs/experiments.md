# Investigação futura de controles por ADB

**Nenhum controle desta página está habilitado na prévia.** O app não pede `WRITE_SECURE_SETTINGS` e o diagnóstico não executa `pm grant` nem escreve configurações. Esta etapa depende de conexão e testes físicos posteriores; foi adiada junto com a instalação do relógio.

Pesquisa e protótipos podem avançar sem conectar o relógio; a liberação como recurso funcional depende da comprovação no firmware real. A [pesquisa de permissões e “Só relógio” em 19/10/2026](referencias/permissoes-e-so-relogio.md) encontrou referências Android e Galaxy Watch para controles diretos. O objetivo do próximo incremento é aplicação e restauração verificáveis; a central de atalhos atual é uma base, não a conclusão do objetivo de economia.

O [laboratório separado](laboratory.md) iniciou essa prova no AVD: o mecanismo AOSP de economia está desativado nessa imagem, por isso uma escrita aceita não foi declarada controle funcional. O módulo lab tem pacote/permissões próprios e não modifica o APK publicado. Localização global e restauração são verificadas separadamente; resultados não representam economia medida ou compatibilidade Samsung.

## Critério para liberar um controle

Investigue separadamente economia padrão, localização, brilho e tempo de tela. Primeiro identifique a interface suportada no firmware. Confirme aplicação por uma API independente, interface Samsung e comportamento observado; escrever e reler o mesmo campo não basta.

Investigue também a política `battery_saver_constants`, por parâmetro, sem presumir que opções AOSP são respeitadas pela Samsung. Preserve os demais campos e compare com a política já ativa: o usuário já utiliza economia de energia e reaplicar restrições existentes não demonstra ganho adicional. LTE, CPU e “Só relógio” não estão automaticamente liberados por uma permissão de escrita de configurações.

Para economia padrão, inclua desligar e reativar manualmente, recarga e reinício. A máquina de estados do Android pode manter efeitos relacionados à economia além de uma chave isolada; o comportamento de referência não comprova equivalência com a implementação Samsung. [Código do BatterySaverStateMachine no Android](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/power/batterysaver/BatterySaverStateMachine.java)

Se um teste exigir permissão especial, documente o motivo, a permissão exata, o procedimento manual de concessão e revogação e a recuperação antes de executar qualquer alteração. Não inclua grants em scripts de instalação ou diagnóstico. Somente depois da prova física o controle pode fazer parte de uma versão experimental explicitamente identificada.

## Contrato de uma futura sessão de ajustes

Antes da primeira escrita, persistir o valor original. Manter uma única sessão com registro por ajuste, estado de aplicação e confirmação. Reaplicação não substitui o valor original. Aplicação parcial deve deixar visível quais ajustes mudaram e quais falharam.

Ao restaurar, comparar o estado atual com o estado aplicado. Mudança manual posterior deve ser preservada como conflito, sem sobrescrever a escolha do usuário silenciosamente. Falhas de leitura, escrita ou revogação de permissão mantêm o registro para recuperação; não tratar uma tentativa como restauração confirmada.

O registro deve ficar excluído do backup Android. Desinstalar apaga dados do app e não garante restaurar configurações do sistema; documentar a recuperação manual antes da inclusão de qualquer escrita. Nenhuma destas obrigações é acionada pela prévia, pois ela apenas abre configurações para alterações manuais do usuário.

## Prova e economia

Cobrir aplicação parcial, reaplicação, encerramento do processo, reinício, conflito com alteração manual e permissão revogada. A liberação exige restauração confiável, além de efeito real. Depois executar a comparação B/C do [protocolo de autonomia](validation.md). Um ajuste funcional não prova ganho adicional de bateria.
