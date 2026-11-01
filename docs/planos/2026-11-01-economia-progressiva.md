---
card_key: watchreserve-economia-progressiva
title: WatchReserve — economia progressiva, apps e serviços
priority: p0
complexity: alta
labels: [wear-os, bateria, UX, reversibilidade]
---

# WatchReserve — economia progressiva e modular

**Plano de produto revisado em 01/11/2026.** Status: PROPOSTO / não implementado. [Auditoria da arquitetura privilegiada](../auditorias/2026-11-01-shizuku-bridge-economia-progressiva.md): **REVISAR antes de controles reais**; P0/P1 podem avançar. Esta é a nova prioridade funcional do projeto. O plano [Relógio + alarmes de 22/10](2026-10-22-relogio-alarmes.md) permanece como histórico técnico e referência para bootstrap, recuperação e medições. Não alterar a v0.2.2 publicada nem remover seus atalhos.

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

## Arquitetura: UX sem shell direto; Bridge como broker e executor limitado

**Sem Bridge:** o app apresenta perfis, prévia, leitura permitida ao UID comum, atalhos para ajustes nativos e a entrada Samsung “Só relógio”. Não afirma ter desativado app ou serviço sem observação da mudança efetiva.

**Com Bridge (arquitetura-alvo, ainda NÃO implementada):**

```text
WatchReserve (UID comum; sem Shizuku SDK/permissão de shell)
    -> ProfileBrokerService explícito da Bridge (Binder próprio)
       [reautentica UID/pacote/certificado/versão/capacidade]
    -> JournalController + AtomicJournalStore (único dono no processo gerenciador)
       [prepara originais, intenção durável, idempotência, conflito]
    -> ProfileExecutorUserService (UID shell 2000 via infraestrutura Shizuku)
       [revalida identidade do broker, operação tipada, alvo e allowlist]
    -> Android PackageManager / AppOps / outras APIs QUALIFICADAS
```

O cliente final **não recebe Binder Shizuku, API genérica `transactRemote`, terminal, UserService arbitrário nem permissão `API_V23`**. O protocolo Shizuku é detalhe **interno da Bridge**, onde for tecnicamente e juridicamente admissível; não é a API do produto. O laboratório de leitura mantém seu contrato atual somente para desenvolvimento, até migração controlada.

**Autenticação em dois pontos:** serviço Binder explícito exportado para cliente qualificado; conferir `Binder.getCallingUid()`, resolução unívoca UID → package, assinatura e versão/protocolo em **cada chamada**. Se os dois APKs forem assinados pelo mesmo certificado, usar também uma permissão Android `signature` própria; se não, usar verificação rigorosa de UID/certificado na implementação do Binder, sem assumir que uma permissão customizada genérica substitui a checagem. O executor shell deve aceitar somente o broker autorizado, revalidar o alvo/estado do sistema e restringir operações em sua própria fronteira; **nenhuma validação apenas na UI**. Não transportar um Binder privilegiado bruto para o app normal.

**Conformidade da Bridge:** o manifesto atual declara `moe.shizuku.manager.permission.API_V23` e o README oficial do Shizuku proíbe declarar `moe.shizuku.manager.permission.*` em projetos derivados ([fonte oficial](https://github.com/RikkaApps/Shizuku#license)). **Bloqueio para distribuir nova Bridge.** Avaliar primeiro (a) contrato privado sem declarar o namespace proibido, com migração e testes de bootstrap/SDK; ou (b) autorização explícita dos mantenedores upstream. Não renomear a permissão isoladamente: o SDK e o servidor podem depender do nome legado. Não declarar conformidade antes de evidência verificável.

**Restauração:** a Bridge gerencia os diários PREPARATION, RECOVERY e PROFILE; o Lab guarda somente receitas de diagnóstico, nunca uma cópia concorrente de perfil. A gravação por `AtomicFile` não serializa processos: todas as requisições e gatilhos de boot precisam passar pelo mesmo controlador proprietário. A aplicação por lote pode ficar parcialmente concluída; oferecer estados claros e rollback assistido, não prometer atomicidade de múltiplos controles. `requireIdle` precisa provar ausência de comandos ainda em voo.

**Reboot e bateria:** o shell via ADB normalmente precisa ser reiniciado após reboot e a Bridge atual não se inicia sem PC. Até prova física do contrário, depois do reboot mostrar **Bridge desligada; intervenção ainda pode estar ativa; restauração indisponível até reautorizar/iniciar a Bridge**. Não oferecer perfis persistentes que possam cortar a rota de retorno. Uma operação não é "segura" só por ser persistente: o dispositivo deve continuar utilizável e oferecer recuperação praticável. Medir o custo do serviço shell e encerrá-lo quando sua ausência não comprometer restauração.

**Estados de execução necessários:** `PREVIEW_ONLY`, `NEEDS_SETUP`, `READY`, `APPLYING`, `APPLIED_VERIFIED`, `PARTIAL`, `RESTORATION_BLOCKED`, `CONFLICT`, `RESTORED`. O app nunca mostra `APPLIED_VERIFIED` após abrir Configurações ou apenas receber ACK do shell.

**Desativar serviço não é desativar processo:** distinguir `pm disable-user` para pacote, componente `ComponentName` individual, AppOp por operação, restrição de background e `force-stop` temporário. A identidade efetiva do alvo pode mudar por firmware, UID compartilhado, instalação e preferências do usuário. Proteções e dependências têm precedência sobre o perfil extremo.

## Plano em fases, do menor ao maior risco

| Etapa | Entrega | Gate verificável |
|---|---|---|
| P0 — UX sem privilégios | Cinco perfis, editor por função/app, estados honestos, prévia, rota “Só relógio” existente e restauração visível | Testes UI 192 dp/1.0/1.3, seleção/reversão de preferências sem efeitos de sistema |
| P1 — Diagnóstico | Parser das 13 consultas atuais; interpretação por UID e erros; coleta acionada somente pelo usuário | Relatórios identificam indisponível, valor zero, timeout, reboot, dados truncados e comparação válida |
| P2 — Contrato e conformidade | Decidir interface própria, assinatura/protocolo/UID, fronteiras do executor, identidade dos pacotes, namespace e migração do legado | Revisão de ameaça/contrato + prova de compatibilidade do protótipo **sem** novo privilégio ao app normal; bloqueio de release do namespace resolvido |
| P3 — Backend de ensaio | Broker de testes + executor shell enumerado, **somente fixture AVD**, integrado ao diário da Bridge | apply→observar→matar gerenciador/shell→reconciliar→restaurar; conflito, replay, timeout, usuário errado, UID compartilhado e revogação recusados |
| P4 — Watch8 físico | Leitura real no Samsung; qualificar bootstrap, reboot/rede, custo de shell e caminho de recuperação; verificar alarme sem ponte | Evidências do SM-L325F/firmware explícito; se recuperação falha, manter controls persistentes bloqueados |
| P5 — Um alvo opcional por tipo | Controles progressivos de package, componente/serviço e AppOp, após allowlist e testes específicos | Observação independente, efeito, rollback exato, proteção do alarme, e estado parcial visível |
| P6 — Integração final | Resolver capacidades e dependências no app principal, aplicar e restaurar por broker próprio | Compatibilidade entre APKs, CI cruzada, regressões Home/tile/complicação, interface simples |
| P7 — Autonomia | Comparações R/P/E por capacidades preservadas, incluindo custo da Bridge | Três pares R/E de 24 h com integridade dos dados; ganho acima da resolução ou INCONCLUSIVO |

P0/P1 não exigem P4. **P5/P6 não liberam escrita em relógio físico sem os gates P2/P3/P4**. Não executar um "perfil de serviços" se o usuário não puder desfazer as mudanças em condições realistas de uso.

## Cenários mínimos de aceite do produto

- **Onboarding em poucos toques:** instalar o app → ver cinco perfis → personalizar sem conhecimento de ADB; ausência de Bridge produz `NEEDS_SETUP`, não falso sucesso; o atalho “Só relógio” ainda abre a confirmação Samsung.
- **Função protegida:** escolher notificações, saúde ou alarmes deve recusar qualquer ação que afete dependência requerida, inclusive componente que sirva vários recursos.
- **Perfil parcial:** entre cinco operações, a terceira falha → mostrar duas aplicadas e três pendentes/recusadas, com restauração acessível e journal preservado.
- **Versões/identidades:** certificado errado, aplicação errada, UID compartilhado, versão desconhecida, protocolo incompatível, cliente revogado, Binder morto → negar escrita e manter journal.
- **Reboot:** com/sem Bridge ativa, com/sem Wi-Fi e sem PC; uma intervenção persistente só é elegível quando o caminho de restauração foi comprovado para esse cenário.
- **Medida comparável:** modo Completo e Econômico testados com mesmas capacidades relevantes, conectividade, uso e custo da Bridge; sem afirmação quantitativa antes de coleta válida.

## Segurança, testagem e rollout

- Proteger hora, alarme nativo único/recorrente, inicialização, mostrador, SystemUI, áudio/vibração do alarme e acesso de restauração.
- Ao selecionar "notificações", preservar o caminho real de transporte e serviços necessários; quando sem telefone/rede mostrar indisponibilidade.
- Testar app desativado, componente desativado, restrição de segundo plano e AppOps separadamente, com observação real, reboot e reinício; nenhum controle genérico.
- Registrar estado bruto e identidade antes da primeira alteração; não sobrescrever mudanças externas e não usar "allow"/"active" como restauração genérica.
- Testar morte entre intenção/escrita/verificação, atualização de firmware, mudança de UID, versão/assinatura, storage corrompido e Bridge off.
- Manter Android app release v0.2.2 sem mudanças enquanto a nova política está em desenvolvimento; usar branches, CI, PRs e explicitamente não incorporar SDK Shizuku ao app de atalhos sem necessidade.
- Separar provas AVD, física Samsung e ganho medido. Matriz de compatibilidade por firmware. Rollback seguro antes de remover a Bridge.
- **Sem root** como premissa; se dependências de plataforma impedirem uma ação, bloquear e explicar. Não permitir "desativar tudo" indiscriminadamente.
- Revisão de release documental: `git diff --check`, links relativos, Markdown legível e histórico das duas PRs. Qualquer código futuro: Gradle build/lint/unit/instrumentado, testes cross-repo e checagem física correspondente; CI de documentação não valida permissões reais.


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
