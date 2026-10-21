# Resultados técnicos

Última revisão: 19/10/2026.

## Evidência disponível

| Item | Resultado |
|---|---|
| Alvos do projeto | Linha Galaxy Watch Wear OS/API 30+, da família Watch4 em diante, incluindo variantes Classic, Pro, FE e Ultra com essa plataforma. Nenhum modelo validado fisicamente. |
| Primeira referência informada | Galaxy Watch8 LTE de 40 mm; versão exata do firmware ainda não registrada. |
| Autonomia informada | Três dias e algumas horas, com Bluetooth desligado e economia ativa; sem protocolo controlado. |
| Conexão ADB com Watch8 nesta sessão | Adiada pelo usuário; não necessária para publicar a prévia. |
| Destino Samsung de “Só relógio” acessível pelo APK | Não comprovado. Não foi presumido um componente privado. |
| Atalho nativo mais curto | Ainda não medido. |
| Home, tile e complicação no aparelho | Pendente. |
| Firmware e versão do pacote da activity candidata | Pendente de coleta privada; o pacote pode diferir das configurações gerais. |
| Economia adicional | Não medida; nenhum ganho declarado. |
| Controles globais / `WRITE_SECURE_SETTINGS` | Não habilitados nesta prévia. |

## Verificações históricas do diagnóstico local

O script `Collect-WatchDiagnostics.ps1` passou por análise sintática e execução com ADB simulado no PowerShell 7.6 e Windows PowerShell 5.1. Foram verificados: nenhum aparelho, múltiplos aparelhos exigindo seleção, seleção desconectada, ausência de identificadores e dumps por padrão, opt-in para dados privados, repetição da coleta com ACL restrita e uso exclusivo de consultas de leitura. Esses testes não acessaram um relógio real.

O teste reproduzível está em [`scripts/tests/Test-Diagnostics.ps1`](../scripts/tests/Test-Diagnostics.ps1). Ele executa um subprocesso com `LOCALAPPDATA` temporário, mantém o ambiente do processo pai e remove somente a fixture temporária validada.

Links locais da documentação foram verificados. Os resultados de build, lint, testes do app e assinatura devem ser registrados nas notas de cada release após execução; não são substituídos pela validação do script.

## Estados que não devem ser confundidos

- **Compilado:** o APK passou pelo build; isso não confirma o comportamento Samsung.
- **Intent resolvido:** o sistema declara um destino; a tela e o número de ações ainda precisam ser observados.
- **Tela acessível pelo APK:** o app abriu a tela com suas próprias permissões; chamadas feitas pelo shell ADB não bastam.
- **Atalho útil:** houve redução de ações até a confirmação, comparando o mesmo estado inicial com o melhor caminho nativo.
- **Economia medida:** comparações repetidas e equivalentes sustentam uma diferença além da variação observada.

## Verificações locais da v0.1.0 — histórico

- APK de desenvolvimento compilado com `assembleDebug`.
- 17 testes JVM passaram: prova de destino, seleção de provedores do sistema e integridade das medições.
- `lintDebug`: zero erros e dez avisos de versões mais recentes de dependências; versões fixadas compatíveis com API 36.
- APK de release otimizado com R8 e `lintVitalRelease` concluídos.
- APK assinado com certificado persistente; `apksigner verify` e `zipalign -c -P 16 4` passaram.
- O certificado do APK corresponde ao hash público em `signing-certificate.sha256`.
- A assinatura foi repetida no Windows PowerShell 5.1 usando os arquivos do backup privado, sem gerar outra identidade.
- O APK final não é depurável e declara somente a permissão de assinatura interna adicionada pelo AndroidX; não solicita internet, sensores ou escrita de configurações.
- Diagnóstico simulado passou em PowerShell 7.6 e Windows PowerShell 5.1, incluindo privacidade, seleção de aparelho e reaplicação de ACL.
- Revisão independente do código e dos scripts sem achados bloqueantes ou altos após as correções.

O [workflow Android](https://github.com/brenopoubel0324/WatchReserve/actions/workflows/android.yml) verifica o app no Linux e o diagnóstico nas duas versões de PowerShell no Windows. Seu histórico público registra o resultado de cada execução.

## Publicação v0.1.0

- [Prévia publicada](https://github.com/brenopoubel0324/WatchReserve/releases/tag/v0.1.0), marcada como prerelease.
- Commit de código e tag: `c66d8b747f5b2ebdd3971b147fa790e7a5ba54e4`.
- APK: `Watch8-Battery-0.1.0.apk`, 2.968.209 bytes.
- SHA-256: `8f8777f4282dd63d06164f924346808cd55431676b1ae1eb58129dce5a1617dc`.
- Download dos assets do GitHub conferido: hash e assinatura válidos.
- [Primeira execução de CI](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39279051822): diagnóstico Windows aprovado; build Android interrompido antes da compilação porque `sdkmanager` não estava disponível no runner. A v0.2.0 passa a preparar explicitamente o SDK com uma ação fixada por SHA. Os resultados locais acima foram executados de fato.

Na entrega da v0.1.0, o bug sweep local não encontrou achados bloqueantes ou altos e a CI ainda aguardava execução. A falha posterior de preparação do SDK está registrada acima e foi incorporada à revisão da v0.2.0. Nenhuma chave, senha ou diagnóstico bruto foi versionado.

## Publicação v0.2.0 — WatchReserve

Nome público WatchReserve e foco ampliado para a linha Galaxy Watch com Wear OS. A documentação descreve a [matriz de alvos](compatibility.md), seleção por recursos e [revisão funcional com acabamento](validation.md). A prova Samsung continua específica ao firmware e à versão do pacote candidato.

Application ID, componentes e identidade de assinatura da v0.1.0 permanecem iguais. Os artefatos históricos acima conservam seus nomes, commit e hash; a nova release usa `WatchReserve-0.2.0.apk`.

Nesta etapa foram implementados LTE condicionado a `FEATURE_TELEPHONY`, procura dos dois pacotes Samsung Health declarados, retorno das páginas internas, estado de rolagem por página, mensagens novas no topo com anúncio acessível, campo de toque mínimo de 48 dp e tratamento de rolagem por coroa/bezel. Tile e ícones foram alinhados ao nome; a prévia vetorial do tile é uma ilustração aproximada, não uma captura de execução.

O diagnóstico simulado passou no PowerShell 7.6 e no Windows PowerShell 5.1 após a troca para o diretório WatchReserveDiagnostics; o BOM UTF-8 foi preservado nos scripts. A revisão funcional e de acabamento teve três rodadas de inspeção de código. Foram corrigidos os achados de navegação, recursos opcionais, rolagem, repetição de avisos e ícones; não restaram achados bloqueantes, altos ou médios nos diffs de UI. O workflow passou no `actionlint`.

Após a última correção de mensagens, `assembleDebug`, os 17 testes JVM e `lintDebug` passaram no caminho local final WatchReserve. O lint manteve zero erros e dez avisos de atualização de dependências. `assembleRelease`, R8 e `lintVitalRelease` também passaram. Nome, versionCode `2`, versionName `0.2.0`, API mínima 30/alvo 36 e permissões foram conferidos no APK. A release não é depurável e mantém somente a permissão de assinatura interna do AndroidX.

- [Prévia publicada](https://github.com/brenopoubel0324/WatchReserve/releases/tag/v0.2.0), marcada como prerelease.
- Commit de código e tag: `46174e9f4903f6dbd8c00666dd81c2143080d336`.
- APK: `WatchReserve-0.2.0.apk`, 2.968.265 bytes.
- SHA-256: `5634321f42c1ff39496db7982cfd3e87269ea6e97db8effd8acc879880ac850d`.
- Assinatura persistente, certificado fixado e alinhamento de 16 KiB verificados.
- Download público sem autenticação conferido: APK e checksum correspondem; assinatura válida e mesmo certificado da v0.1.0.
- [CI da v0.2.0](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39311475179): **aprovada**, incluindo build/lint/testes no Ubuntu e diagnóstico nas duas versões de PowerShell no Windows. O SDK foi preparado com sucesso; os avisos de atualização das actions não impediram a execução.

Os checks históricos não são usados como evidência desta versão. Não há teste físico nem ganho de autonomia confirmado em nenhum alvo.

**Estado visual na entrega da v0.2.0:** a tentativa de renderização dos vetores no Chrome foi bloqueada pela política de execução do ambiente. Naquela entrega não houve observação da interface em emulador ou relógio. A v0.2.1 adiciona evidência em emulador abaixo, com seus limites próprios.

Bug sweep final: diff, Git, mudanças recentes, recursos opcionais, componentes Android, assinatura, links, permissões e resultados revisados; sem achados bloqueantes ou altos remanescentes. As lacunas são a verificação visual, a validação física por modelo/firmware e a medição de autonomia. Nenhuma chave, senha ou coleta privada foi publicada.

## Validação v0.2.1 — auditoria e emulador

Autoauditoria em três rodadas pela skill `self-audit-loop`, solicitada pelo usuário antes da emulação. [Relatório completo](auditorias/2026-10-19-execucao.md): aprovado para esta prévia, sem achados bloqueantes ou altos remanescentes. Plano completo parcialmente implementado; não há prova Samsung nem controles avançados nesta versão.

Corrigidos o estado de destino validado após falha de limpeza e a atualização do cache de preferências após erro de escrita. A revisão visual priorizou as ações principais e reduziu textos iniciais. Nomes de arquivos/chaves de preferências, application ID, componentes e certificado foram preservados.

- **23 testes JVM aprovados**, seis novos cobrindo falhas de armazenamento.
- **Seis testes instrumentados em cada fonte, 1,0 e 1,3**, com falhas reais de disco no Android, recriação/Back, mensagens repetidas e evento rotativo.
- Imagem Wear OS 5/API 34, 384×384 px e 192 dp, forma circular confirmada pelo Android; nenhuma conexão física.
- [Oito capturas finais e resultados](evidencias/2026-10-19-wearos5/README.md) inspecionados. Ações principais visíveis nas duas fontes; itens que passam pelas bordas ainda são parcialmente cortados durante a rolagem.
- Builds debug/androidTest/release, R8, lint e actionlint aprovados. Lint: zero erros, doze avisos de atualização de dependências.
- APK release v0.2.0 instalado e atualizado para v0.2.1 com `install -r` no AVD: atualização e painel aprovados. A entrada do atalho chegou às configurações gerais do Wear OS via código do app; não comprova atalho útil para “Só relógio”.
- Assinatura/certificado, metadados, ausência de modo depurável, permissões e alinhamento de 16 KiB conferidos.
- O roteiro do emulador recusa alvo divergente; fonte e tempo de tela restaurados. Não executa testes automaticamente em todos os dispositivos.

- [Prévia v0.2.1 publicada](https://github.com/brenopoubel0324/WatchReserve/releases/tag/v0.2.1), marcada como prerelease.
- Commit de código e tag: `0532462f3adb10f33d11cacc606b872e1657bf36`.
- APK: `WatchReserve-0.2.1.apk`, versionCode `3`, 2.968.265 bytes.
- SHA-256: `55e615db70ced8236322db28b4f019d036730663408f280e8d3721b536ff6697`.
- Download público sem autenticação conferido: APK e checksum correspondem, assinatura válida com o certificado persistente.
- [CI da v0.2.1](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39318073299): **aprovada nos dois jobs**, com build/lint/testes JVM e compilação do pacote instrumentado no Ubuntu; diagnóstico PowerShell 7/5.1 e sintaxe do roteiro no Windows. A instrumentação foi executada localmente no AVD, não na CI.
- No APK assinado, painel reabriu após parada forçada; painel e fallback abriram após reiniciar o AVD. O emulador foi encerrado ao terminar, sem encerrar o servidor ADB global nem reiniciar o computador.

Bug sweep: sem achados bloqueantes ou altos remanescentes; contratos e permissões preservados. Cobertura parcial de teclado/acessibilidade e lista circular registrada no relatório. Continuam pendentes as superfícies externas no firmware Samsung, o ciclo de vida completo, outros modelos/API e a autonomia física. Nenhum ganho de bateria declarado.

## Laboratório de controles — revisão concluída em 21/10/2026

Foi implementado um módulo separado, `:lab`, com aplicação de teste e permissão via ADB apenas nesse pacote. O diretório `app` permanece intacto e a v0.2.1 pública continua sem controles globais. O [guia do laboratório](laboratory.md) descreve as proteções, comandos e limites.

Resultado: localização global alterada e restaurada pelo UID comum do APK, com observação pelo serviço de localização. Escrita sem permissão e após revogação bloqueadas. Interrupção real do processo, preservação da sessão, recusa de reaplicação e recuperação foram exercitadas; a prova normal passou novamente após recuperar.

Economia por `low_power` e política `battery_saver_constants` **não foram validadas**: a imagem Wear OS 5/API 34 informa mecanismo AOSP desativado. Escrita aceita/relida não resultou em estado efetivo reconhecido pelas APIs. Os relatórios mantêm essa distinção; nenhum benefício Samsung ou ganho de autonomia é declarado.

Build debug/androidTest e lint do laboratório aprovados, zero erros e avisos. Os 23 testes JVM e lint do app continuam aprovados (zero erros/doze avisos de atualização). [Autoauditoria](auditorias/2026-10-19-laboratorio.md) concluída em três rodadas, sem achados bloqueantes ou altos remanescentes. [Evidências e hashes dos APKs](evidencias/2026-10-19-controles/README.md) revisados, sem dados privados.

- Código e evidências publicados no commit `3387f646ca08d740fa905114a6f5a631c03acf5b`.
- [CI do laboratório](https://github.com/brenopoubel0324/WatchReserve/actions/runs/39615004887): **aprovada nos dois jobs**, com build/lint/JVM do app, build/lint e compilação da instrumentação do lab, além do diagnóstico PowerShell 7/5.1 e sintaxe dos roteiros. As execuções instrumentadas ocorreram no AVD local.
- Nenhuma nova release do app de usuário: a v0.2.1 continua sendo a prévia publicada e não solicita permissões de escrita.
- Avisos de manutenção das versões fixadas das actions e do runner foram emitidos pela CI, sem impedir os checks. Atualização dessas ferramentas fica como manutenção separada.

Bug sweep final: contratos, isolamento dos módulos, permissões dos APKs, status Git, links, evidências e CI revisados. Sem achados bloqueantes ou altos; validação física Samsung e autonomia permanecem pendentes.

## Descoberta e prova física — 21/10/2026

Watch8 conectado pelo PC; modelo SM-L325F, Android 16/API 36, firmware L325FXXS3AZF4.
Com a confirmação aberta manualmente pelo usuário, a coleta identificou
`com.samsung.android.batterysavingsettings/.wom.WatchOnlyModeActivity`, exportada e sem
permissão declarada para abertura. A primeira etapa foi somente leitura.
[Resumo técnico revisado](evidencias/2026-10-21-watch8/README.md).

Achado corrigido na v0.2.2: os intents Android retornam o pacote geral e deixam
o pacote Samsung de bateria fora da descoberta. A consulta da ação Samsung de bateria foi
adicionada, preservando filtro de sistema, prova manual e invalidação por versão/firmware.
Build debug/androidTest e release, lint e 23 testes JVM aprovados; assinatura e alinhamento
verificados. APK final com 2.984.649 bytes, SHA-256
`79f2d524ec1360c1a82aa67db9b82d4534e69980f7ca0ca2b747fc650ef855cf`.
[Revisão funcional e de acabamento](auditorias/2026-10-21-descoberta.md).

A coleta inicial não instalou APK, concedeu permissões ou alterou configurações. Depois, o
usuário autorizou instalar e testar a v0.2.2: busca, salvamento, abertura pelo UID comum do
APK, retorno e confirmação local foram executados. A entrada compartilhada abriu novamente
a tela Samsung. O teste físico de Home encontrou retomada indevida do painel pela afinidade
compartilhada; a tarefa do atalho foi separada e uma regressão instrumentada foi acrescentada.
Sete testes passaram em cada escala de fonte (1,0 e 1,3), com oito capturas revisadas.
[Evidências do AVD](evidencias/2026-10-21-wearos5/README.md).

Após atualizar, o usuário confirmou o duplo clique físico abrindo diretamente a confirmação,
desde o mostrador. O caminho nativo de referência usa painel rápido, ícone de economia,
rolagem e seleção de Só relógio. Houve redução de navegação em Home, sem cronometragem ou
medição de autonomia. O usuário também adicionou tile e complicação e confirmou a abertura
da tela correta por ambos. Nenhuma ativação do modo ou permissão avançada. O ciclo de vida
completo continua pendente.

## Próxima evidência necessária

O APK já está instalado; abertura pelo app e os três acessos em uso normal foram comprovados no Watch8. A próxima etapa exige registrar o ciclo de vida e as medições do [roteiro físico](validation.md). Outros modelos e firmwares precisam de prova própria.

Após cada etapa, atualizar esta página e as notas da release com o resultado real. Fingerprints, seriais, rede, dumps e dados pessoais ficam fora deste repositório público.
