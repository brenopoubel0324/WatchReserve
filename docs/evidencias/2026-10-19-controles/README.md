# Evidências do laboratório de controles

Investigação iniciada em 19/10/2026; revisão concluída em **21/10/2026**. AVD Wear OS 5/API 34, circular, 384×384 px, 192 dp. Nenhum relógio pessoal foi conectado. Pacotes e comandos do laboratório são descritos no [guia reproduzível](../../laboratory.md).

## Resultado por controle

| Capacidade | Resultado observado |
|---|---|
| Permissão avançada | Escrita bloqueada antes da concessão; `WRITE_SECURE_SETTINGS` concedida ao pacote lab; escrita feita com UID comum do APK; nova escrita bloqueada após revogação. |
| Economia por `low_power` | Escrita aceita e chave relida como `1`; API continuou falsa. A imagem informa suporte AOSP desativado. **Controle não validado.** |
| Política `battery_saver_constants` | Campo de localização alterado, mas modo efetivo permaneceu `0` na API. **Controle não validado.** |
| Localização global | Serviço informou `true` → `false` depois da escrita do APK; restauração confirmou o estado original. **Controle observado somente neste AVD.** |
| Autonomia e Samsung | Não medidos/testados. Nenhum resultado permite declarar ganho de bateria no Watch8. |

`passed=true` significa que a prova e sua limpeza terminaram corretamente. Os campos `powerSaveControlValidated=false` e `policyControlValidated=false` permanecem explícitos; não são convertidos em sucesso de recurso.

## Sequência executada

| Execução | Relatório | Resultado da instrumentação |
|---|---|---|
| Prova normal | [JSON](normal.json) | [Um teste aprovado](normal-instrumentation.txt). |
| Morte real do processo após aplicar a fixture | [JSON](interrupted.json) | [Processo encerrado](interrupted-instrumentation.txt); `pendingRecovery=true`, `passed=false`, resultado esperado desta injeção. |
| Nova aplicação com sessão pendente | Registro original comparado antes/depois, sem alteração. | [Recusa esperada](pending-refused-instrumentation.txt); não é um teste aprovado de aplicação. |
| Recuperação explícita | [JSON](recovery.json) | [Um teste aprovado](recovery-instrumentation.txt): restauração, reset da simulação e revogação confirmados. |
| Repetição após recuperar | [JSON](repeat.json) | [Um teste aprovado](repeat-instrumentation.txt), com mesmos resultados por controle. |

As cinco execuções são cenários do mesmo teste instrumentado; não representam cinco testes distintos. Os dois resultados negativos são intencionais na sequência de recuperação. A primeira investigação de economia, anterior à adaptação à capacidade da imagem, falhou de fato e está registrada na [auditoria](../../auditorias/2026-10-19-laboratorio.md).

## Integridade e limites

O [ambiente e os hashes](environment.json) correspondem aos APKs efetivamente executados. Relatórios contêm somente dados técnicos do AVD e identificadores aleatórios de execução. Nenhum backup bruto da sessão, dump de sistema, credencial ou fingerprint de relógio foi publicado.

Build debug/androidTest e lint do laboratório aprovados, sem erros ou avisos de lint. Os 23 testes JVM do app continuam aprovados. O módulo `app` não foi alterado; a versão pública continua v0.2.1, sem permissões de escrita de configurações. Compilação em CI não substitui estas execuções locais.
