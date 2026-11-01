---
card_key: watchreserve-economia-progressiva
title: WatchReserve — economia progressiva, apps e serviços
priority: p0
complexity: alta
labels: [wear-os, bateria, UX, reversibilidade]
---

# WatchReserve — economia progressiva e modular

**Plano de produto revisado em 01/11/2026.** Status: PROPOSTO / não implementado. Esta é a nova prioridade funcional do projeto. O plano [Relógio + alarmes de 22/10](2026-10-22-relogio-alarmes.md) permanece como histórico técnico e referência para bootstrap, recuperação e medições. Não alterar a v0.2.2 publicada nem remover seus atalhos.

## Visão de produto

**Preservar o atalho Samsung “Só relógio” como a alternativa nativa de maior economia**, sem tentar reproduzi-la ou sobrepor controles arbitrários. Adicionar **quatro perfis de economia progressiva** que preservem o Wear OS utilizável e deixem o usuário escolher funções, aplicativos e serviços que podem ou não funcionar. Nenhum perfil intermediário aciona automaticamente “Só relógio”.

O diferencial não é uma lista fixa de debloat: o usuário escolhe **capacidades a manter**, o motor resolve dependências reais por firmware, exibe efeitos esperados e só executa operações qualificadas e reversíveis. Não prometer ganho antes de medi-lo.

## Experiência de poucos toques

1. Tela inicial: cinco escolhas claras: **Completo**, **Equilibrado**, **Econômico**, **Extremo**, **Só relógio (Samsung)**.
2. Nos quatro primeiros, ação **Personalizar** para selecionar funções preservadas (notificações, chamadas/LTE, saúde e sono, GPS/treino, assistente, mídia, pagamentos, apps específicos, conectividade, AOD). **Hora, sistema e alarme nativo** protegidos em todos os perfis Wear OS.
3. Página **Apps e serviços**: lista pesquisável de apps opcionais e serviços/componentes qualificados; estados "Manter", "Restringir", "Desativar", "Indisponível" com explicação do impacto. Não oferecer ação perigosa sobre package, receiver, provider ou service protegido, mesmo em Extremo.
4. **Prévia antes de aplicar**: o que continuará disponível, o que parará, dependências, estado do backend, operações automáticas, passos manuais, suporte do firmware e como restaurar.
5. **Aplicar perfil**, **Restaurar anterior** e **Recuperação necessária** com estados explícitos por operação: aplicado, parcialmente aplicado, requer ação, bloqueado, falhou. Nenhum sucesso fictício caso somente abra Configurações.
6. **Só relógio** mantém a rota atual do app/duplo clique Home/tile/complicação até a confirmação nativa Samsung; ativação final continua manual. Exibir aviso de que os perfis Wear OS não permanecem necessariamente operantes dentro do modo Samsung.

**Perfis são modelos editáveis, não gradações numéricas de economia assegurada.** O indicador inicial pode mostrar "maior/menor restrição", nunca horas extras ou porcentagens inventadas. O app pode guardar mais de um perfil personalizado offline.

## Perfis iniciais

| Perfil | Política sugerida | Restrição |
|---|---|---|
| Completo | Todos os recursos desejados continuam; apenas dicas nativas e ajustes de baixo impacto comprovados | Mínima |
| Equilibrado | Manter chamadas/notificações/saúde conforme opção; restringir extras e processos de segundo plano elegíveis | Moderada |
| Econômico | Manter capacidades marcadas e limitar os demais aplicativos e serviços opcionais validados | Alta |
| Extremo | Preservar hora, mostrador, UI e alarmes; só os módulos explicitamente selecionados continuam | Muito alta, ainda em Wear OS |
| Só relógio | Atalho para modo nativo Samsung, fora do mecanismo de perfis | Economia nativa máxima; ação separada |

O perfil Extremo não é garantia de autonomia próxima a “Só relógio”. Saúde, telefonia, mensagens e GPS só são excluídos do perfil se o usuário aceitar suas consequências.

## Motor de capacidades e intervenções

- **CapabilityCatalog:** funções compreensíveis ao usuário, grupos de apps/serviços observados, dependências e proteções; mapeamento versionado por modelo, firmware, UID e assinatura. Não embutir lista universal de pacotes Samsung.
- **CapabilityResolver:** compila escolhas em intervenções enumeradas e ordenadas sem quebrar uma função marcada como essencial; uma capacidade selecionada tem precedência sobre economia.
- **InterventionCatalog:** tipos distintos de ação, em escala de risco: configurações nativas; standby/restrição de background quando observável; AppOps individuais; desativação temporária de pacote; desativação de componente (service/receiver/provider) **somente se testado e autorizado para aquele alvo**. Não confundir force-stop, suspensão, disable-user e desativação de componente. Sem shell livre.
- **RiskPolicy:** alarme nativo e todos seus componentes/dependências, SystemUI, hora, boot, transporte necessário para recuperar e proprietário do diário são invariantes de proteção; recusar alvo desconhecido, UID compartilhado ambíguo ou dependência não resolvida.
- **EnergyEvidence:** coleta manual por UID, jobs, alarmes, wakelocks e bateria quando disponíveis; diferenciar tarefa recente, processo em cache, serviço ativo e atividade real. Sem polling residente.
- **PlanPreview:** para cada ação mostrar "automática qualificada", "abrir ajustes", "não suportada", "precisa da ponte", "bloqueada para segurança" com motivo.
- **ReversibleController:** preservar valores originais EXATOS, inclusive estados absent/default/enabled; persistir intenção antes da escrita; observar por mecanismo independente; reconciliação após crash, conflito externo e rollback idempotente.

App Standby e AppOps podem ser alterados pelo próprio sistema e não garantem ganho; registrar estado efetivo, não apenas sucesso de comando. Alterações de serviços internos devem ser opt-in avançado e individualmente permitidas por combinação firmware/pacote, nunca por heurística genérica de nome. Não limpar dados, desinstalar pacotes de sistema ou alterar política global agressiva no MVP.

## Arquitetura: separar UX de privilégio

**Sem Bridge:** a UI oferece modelos, seleção, prévia, diagnóstico acessível e atalhos verificados para ajustes nativos. Não pode afirmar que desativou serviço sem permissão. Preferências não são mudanças de sistema.

**Com Bridge:** ativação e restauração de intervenções enumeradas, somente depois de identidade de aplicativo, certificado, versão/protocolo e autorização aprovados. O serviço shell não deve se manter residente quando desnecessário; medir seu custo.

Os três diários da Bridge (preparation/recovery/profile) já possuem núcleo e testes de fixture; **não têm backend de escrita de produção nem IPC com controles**. A Bridge atualmente requer PC/ADB para iniciar após boot; não prometer recuperação autônoma sem prova física. Controles que não possam ser revertidos com o backend ausente devem ser recusados para perfis de usuário.

Somente um controlador é dono dos diários de alterações, serializando UI/IPC/boot. O Lab permanece dono apenas de **receitas de leitura**, não dos diários de escrita. Não migrar diários entre IDs sem recuperação completa e autorização explícita. Atualizar contratos e texto de permissão da Bridge: o cliente atual admite somente a versão 0.2 de leitura e informa "somente leitura".

## Plano em fases, do menor ao maior risco

| Etapa | Entrega | Gate |
|---|---|---|
| P0 — UX do produto | Escolha de perfis, editor de capacidades/apps, prévia, opções de restauração, atalho Só relógio preservado; nenhuma escrita privilegiada | Wear 192 dp, fontes 1.0/1.3, fluxo intuitivo, testes de dependências e proteção |
| P1 — Diagnóstico | Parser do ZIP das 13 consultas existentes, interpretação de erros e UID, hipóteses por app/serviço; exportação manual | Dados reais distinguem ausência/zero/erro e reset de contadores |
| P2 — Contrato e backend restrito | Contrato tipado Lab↔Bridge; fixture allowlisted em AVD usando diário existente | apply/restore/duas execuções/crash/resposta perdida/conflito/revogação/IPC com identidade |
| P3 — Watch8 físico | Coleta local sob economia Samsung, alarme, boot/rede e viabilidade da recuperação sem PC | Evidência física por firmware, com limitações e fail-closed |
| P4 — Primeiros controles | Um controle de app e um de serviço **opcional verificado**; observar independência, impacto, restauração e dependências | Não afetar alarme, atualização, SystemUI, recovery; sem lista automática |
| P5 — Integrar perfis avançados | Policy compiler + execução transacional de operações, progresso e estado parcial no app | Teste cross-repo, CI, UI, regressões das rotas atuais |
| P6 — Autonomia | R/P/E com ponte no custo e três pares R/E 24h, se mensuração válida | Ganho consistente acima da resolução ou declarar inconclusivo |

P0 e P1 não dependem de P3; P4 e P5 não podem liberar controles físicos antes dos seus gates. Dar preferência a mudanças que persistem sem daemon, evitando ligar o Wear OS com mais um supervisor permanente.

## Segurança, testagem e rollout

- Proteger hora, alarme nativo único/recorrente, inicialização, mostrador, SystemUI, áudio/vibração do alarme e acesso de restauração.
- Ao selecionar "notificações", preservar o caminho real de transporte e serviços necessários; quando sem telefone/rede mostrar indisponibilidade.
- Testar app desativado, componente desativado, restrição de segundo plano e AppOps separadamente, com observação real, reboot e reinício; nenhum controle genérico.
- Registrar estado bruto e identidade antes da primeira alteração; não sobrescrever mudanças externas e não usar "allow"/"active" como restauração genérica.
- Testar morte entre intenção/escrita/verificação, atualização de firmware, mudança de UID, versão/assinatura, storage corrompido e Bridge off.
- Manter Android app release v0.2.2 sem mudanças enquanto a nova política está em desenvolvimento; usar branches, CI, PRs e explicitamente não incorporar SDK Shizuku ao app de atalhos sem necessidade.
- Separar provas AVD, física Samsung e ganho medido. Matriz de compatibilidade por firmware. Rollback seguro antes de remover a Bridge.
- **Sem root** como premissa; se dependências de plataforma impedirem uma ação, bloquear e explicar. Não permitir "desativar tudo" indiscriminadamente.

## Pendências e não objetivos

- Não há prova de economia incremental em horas nos perfis.
- Não há backend Samsung de desativação de serviços qualificado, nem bootstrap sem PC após reboot.
- "Só relógio" continua pela rota existente, como alternativa de máxima economia, não como parte de um preset personalizado.
- Não incluir undervolt, overclock, ajustes arbitrários de Doze, desinstalação de serviços essenciais, exportação de segredos ou monitoração contínua.
- O objetivo de autonomia é **comparar perfis úteis com funções diferentes**, não impor apenas uma única configuração de alarme e hora.

## Fontes técnicas

- [Android App Standby Buckets](https://developer.android.com/topic/performance/appstandby)
- [Android Power management resource limits](https://developer.android.com/topic/performance/power/power-details)
- [AOSP Doze/platform power management](https://source.android.com/docs/core/power/platform_mgmt)
- [Arquitetura e diários da Bridge](https://github.com/brenopoubel0324/WatchReserve-Bridge/blob/main/docs/restoration-journals.md)
