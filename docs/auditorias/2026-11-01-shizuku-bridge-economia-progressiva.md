# Auditoria de execução — Shizuku, Bridge e economia progressiva

**Data:** 01/11/2026. **Escopo:** [plano canônico](../planos/2026-11-01-economia-progressiva.md), [contrato da Bridge](https://github.com/brenopoubel0324/WatchReserve-Bridge/blob/plan/modular-battery-profiles-2026-11-01/docs/watchreserve-economia-progressiva-contrato.md), código real de ambos repositórios e documentação oficial do Shizuku.

## Veredito

- **Plano anterior:** `REVISAR` — permissões atuais amplas, cliente não qualificado para escrita, recovery não demonstrado, contrato de compatibilidade e namespace inadequados para release.
- **Plano revisado nesta branch:** **APROVADO para P0/P1 (UX sem privilégios e diagnóstico)** e **para projetar/testar P2/P3 em fixture no AVD**.
- **NÃO APROVADO para escrita em Galaxy Watch, desativação de apps Samsung ou lançamento público da Bridge.** São gates independentes; não inferir liberação de produção a partir de testes unitários ou CI verde.

A regra de produto permanece: perfis Completo/Equilibrado/Econômico/Extremo com funções personalizáveis e **atalho “Só relógio” Samsung preservado** como opção nativa máxima. Evitar perda de alarmes, interface, comunicação e meio de recuperação; quantificar autonomia somente após teste.

## Achados concretos e mudanças mandatórias

| ID | Severidade | Arquivo/contrato | Fato e risco | Correção incorporada ao plano |
|---|---|---|---|---|
| S1 | **Bloqueante para release** | `WatchReserve-Bridge/wear/src/main/AndroidManifest.xml` | Declara `moe.shizuku.manager.permission.API_V23`; o [README oficial do Shizuku](https://github.com/RikkaApps/Shizuku#license) proíbe que derivados declarem `moe.shizuku.manager.permission.*`. Trocar o nome isoladamente pode quebrar API/SDK/servidor. | Bloquear release, investigar broker próprio sem declaração reservada e testar migração em AVD, ou obter autorização upstream. Não declarar conformidade sem prova/documentação. |
| S2 | **Bloqueante para escrita no usuário** | `BridgeProvider.java`, `AllowedClient.java`, `PermissionActivity.java` | O provider atual encaminha Binder genérico; a permissão existente é de laboratório, que diz somente leitura. Fazer o app principal consumir Shizuku diretamente daria mais poder que o necessário; não há ACL de operação. | App comum **sem Shizuku ou Binder shell bruto**; criar `ProfileBrokerService` de API própria, executor separado com allowlist tipada, autorização específica de escrita e validação do chamador/target nas duas fronteiras. |
| S3 | **Alto** | `AllowedClient.java` / `BridgeTrust.kt` | Assinaturas, package ID, versionCode/versionName rígidos; `dev.brenopoubel.watch8battery` não é cliente atual. Upgrade unilateral pode quebrar fluxo ou conceder acesso inesperado. | Matriz cliente↔Bridge por certificado/protocolo; versionar endpoints; recusar build desconhecido e preservar laboratório leitura v0.2 até migração. |
| S4 | **Alto** | `JournalController.java`, `AtomicJournalStore.java`, `ReadSessionStore.kt` | Diários de perfil pertencem à Bridge mas ainda não são executados em produção; AtomicFile não serializa processos e a sessão de coleta do Lab não é rollback de escrita. | Proprietário único no manager da Bridge, IPC serializado, estados `PARTIAL/UNKNOWN/CONFLICT`, intenção gravada antes de escrever, exatidão de original/default e nenhum `requireIdle` presumido. |
| S5 | **Alto** | Bootstrap/BridgeActivity/recuperação | A Bridge shell exige PC/ADB novamente após reboot; controles persistentes podem continuar ativos enquanto app normal perde privilégio para desfazê-los. | Ensaio real sem PC, com/sem rede, economia Samsung, revogação, alarme e método de recuperação; sem sucesso, perfis persistentes que afetem recuperação ficam bloqueados. |
| S6 | **Alto** | `COMPONENT_DISABLE` e funções protegidas | Um service/receiver/provider pode ter dependências comuns de alarme, saúde, conexão ou startup; `pm disable-user`, `force-stop`, AppOps, bucket e disable de componente não são equivalentes. | Separar comandos por tipo, qualificar alvo individual por firmware+assinatura, bloquear UID compartilhado não qualificado, nunca inferir segurança por nome do pacote. |
| S7 | **Médio** | `app/src/main/AndroidManifest.xml` e UI | App principal hoje só tem atalhos Samsung e não usa Shizuku; expor parâmetros privilegiados agora causaria regressão e onboarding complexo. | Desenvolver primeiro UX e preview em modo sem Bridge; botão “Só relógio” intacto; sinalizar `NEEDS_SETUP` e restauração indisponível com precisão. |
| S8 | **Médio** | CI e mensuração | Testes do diário e builds em separado não provam integração de dois APKs nem gasto energético. | Teste Binder instrumentado, Watch8 real, custo de shell, R/P/E com pares comparáveis e status `INCONCLUSIVO` permitido. |

**Nuance:** a restrição de namespace consta da posição expressa do projeto upstream em seu README. Esta auditoria não é uma opinião jurídica sobre validade de licença; é um bloqueio de conformidade/distribuição a resolver antes de release.

## Contratos e responsabilidades

| Responsável | Pode fazer | Não pode fazer |
|---|---|---|
| WatchReserve (UID app normal) | Perfil, seleção de capacidades, prévia, status, ajustes nativos e “Só relógio” | Receber Binder shell/Shizuku genérico ou aceitar execução arbitrária |
| Broker da Bridge (manager) | Validar caller/protocolo/consentimento, possuir journals, serializar ordens | Delegar privilégio sem verificar destino e estado; guardar "sucesso" com simples ACK |
| Executor UID 2000 | Executar operação enumerada e qualificada, observar efeito real | Executar shell livre, autorizar serviço protegido, alterar alvo por texto externo |
| Laboratório read-only | Consultas fixas enumeradas e ZIP local | Usar permissão de leitura v0.2 para escrita ou guardar journal de perfil |
| Samsung | Confirmação nativa do “Só relógio” | Ser alterado automaticamente por rotina que contorne confirmação |

A estratégia de autenticação por assinatura depende de verificar se app/Bridge possuem mesmo signer. Uma permissão `signature` não conecta APKs com certificados diferentes; neste caso a API própria precisa validar UID/pacote/certificado/versão por Binder a cada chamada e exercer defesa em profundidade.

## Gate de liberação por etapa

1. **P0/P1 — GO (apenas desenvolvimento):** UI de cinco perfis, seleção por capacidades e prévia sem escrita; parsing de 13 diagnósticos e exportação manual. Testes Android e interfaces 192dp com fontes 1.0/1.3.
2. **P2 — design GO condicionado:** definir interfaces Binder tipadas, versionamento, esquema de proteção, compatibilidade upstream e consentimento de escrita separado. Documentar decisão de namespace antes de construir uma distribuição.
3. **P3 — AVD fixture:** alvo `dev.brenopoubel.watchreserve.lab` exclusivamente; simular apply/restore, UID e assinatura falsos, protocolo incompatível, Binder morto, timeout com comando pendente, journal corrompido, acesso revogado, troca de usuário, crash antes/depois de write e observação com estado externo divergente. O teste não concede autorização para alvos Samsung.
4. **P4 — Watch8 físico:** coleta read-only sob economia Samsung e rádio desligado, identificação UID shell, alarme nativo antes/depois de reboot, recuperação com/sem Wi-Fi e sem PC, mudança de firmware e custo base da ponte. Sem recuperação comprovada, NO-GO para mudanças persistentes que tornem restauração inacessível.
5. **P5/P6 — gradual:** um pacote *opcional*, um componente *opcional* e uma operação AppOp/bucket, individualmente qualificados. Regressões de Home, tile, complicação, alarmes, telefonia/notificações selecionadas e restauração.
6. **P7 — mensuração:** três pares R/E de 24h, estado de rádio, versão, firmware e custo da ponte registrados, exclusão de carga/reboot/uso divergente. Se resolução ou dispersão não suportarem conclusão, reportar **inconclusivo**.

## Checklist de revisão para PRs futuras

- [ ] Endpoint do broker não exporta Shizuku Binder nem aceita string de comando shell.
- [ ] Identidade/consentimento checados no broker **e** executor; erro por versão/assinatura incorretas é fail-closed.
- [ ] `APP_DISABLE`, `COMPONENT_DISABLE`, `APPOP_CHANGE` e `STANDBY_BUCKET_CHANGE` possuem validações e observadores próprios.
- [ ] Journal não é apagado com conflito ou crash; aplicação parcialmente concluída não aparece como sucesso.
- [ ] Invariantes de hora, alarmes, SystemUI, boot, watchface e recuperação persistem em todos os perfis Wear OS.
- [ ] Reboot/Bridge off não elimina caminho efetivo de recuperação; se não houver caminho, operação recusada.
- [ ] A distribuição da Bridge resolve a restrição do namespace; nenhuma release usa aprovação de CI como substituto.
- [ ] Código compilado e testado nos dois repos, `git diff --check`, integração AVD e evidência física com firmware identificado.
- [ ] Sem afirmar horas extras, economia percentual ou redução de wakelocks apenas por desativar app.

## Evidências consultadas e limites

- `WatchReserve`: `app/src/main/AndroidManifest.xml`, `advancedLab/src/main/AndroidManifest.xml`, `advancedLab/.../BridgeTrust.kt`, `advancedLab/.../service/ReadOnlyService.kt`, plano 01/11; main release v0.2.2 preservada.
- `WatchReserve-Bridge`: `wear/src/main/AndroidManifest.xml`, `AllowedClient.java`, `BridgeProvider.java`, `PermissionActivity.java`, `BridgeActivity.java`, `docs/restoration-journals.md`.
- [Shizuku upstream (architecture, ADB permissions and licensing)](https://github.com/RikkaApps/Shizuku).
- [Bridge: plano corrigido](https://github.com/brenopoubel0324/WatchReserve-Bridge/blob/plan/modular-battery-profiles-2026-11-01/docs/watchreserve-economia-progressiva-contrato.md).

**Verificação desta alteração:** revisão documental e vínculos entre PRs. Sem modificar APKs, conceder permissões, inicializar shell, mudar controles de relógio ou executar testes físicos; aprovações de CI de documentação não substituem validação de integração.
