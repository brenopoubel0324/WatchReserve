---
card_key: watch8-battery
title: App pessoal de economia e atalhos para o Watch8
priority: p1
complexity: média
depends_on: []
labels:
  - wear-os
  - android
---

# Watch8-Battery — plano e progresso

## Resumo Trello

Criar um app pessoal para o Galaxy Watch8 LTE de 40 mm com Home, tile e complicação para facilitar o acesso ao modo Samsung “Só relógio”, além de uma central de ajustes manuais. Publicar código, instruções, verificações e APKs assinados no repositório público `brenopoubel0324/Watch8-Battery`.

O usuário autorizou o projeto independente em `C:\Users\breno\code\GitHub\Watch8-Battery`, incluindo a documentação neste repositório; o backend permanece fora do escopo. O plano aprovado é executado em etapas para não afirmar funcionamento Samsung nem economia sem prova física.

## Critérios de Aceite

- [x] Repositório público em `main`, com código, wrapper, documentação e CI de leitura.
- [x] Build local, lint, 17 testes JVM, diagnóstico simulado e bug sweep concluídos.
- [x] APK de prévia assinado com chave persistente e publicado com tag e limitações explícitas.
- [ ] Home, tile e complicação comprovados no Watch8, inclusive após reinício e morte normal do processo.
- [ ] Firmware e versão do pacote da activity candidata registrados de forma privada; resumo técnico público revisado.
- [ ] Ao menos um atalho reduz ações até a confirmação de “Só relógio”, contra o melhor caminho nativo.
- [ ] Nenhuma activity Samsung habilitada sem teste pelo próprio APK com permissões comuns.
- [ ] Ganho de autonomia medido em janelas equivalentes; resultado inconclusivo aceito.

## Etapas de implementação

1. Preparar projeto Kotlin/Compose para Wear OS, com API mínima 30 e alvo 36; validar build e testes.
2. Implementar uma entrada “Só relógio” compartilhada por launcher/Home, tile e complicação, com fallback por intents públicos. Alguns seletores podem usar o nome Watch8 Battery. Manter confirmação Samsung.
3. Entregar a central com bateria atual e atalhos para configurações. Não depender de permissões de escrita.
4. Publicar prévia técnica assinada, documentação e resultados locais verificáveis.
5. Conectar o Watch8 por ADB e investigar a tela de “Só relógio”. Provar acesso no APK antes de habilitar um componente Samsung; vincular a validação ao firmware e à versão do pacote candidato e invalidá-la após mudanças. Aceitar somente pacote do sistema associado às configurações gerais ou de economia.
6. Executar testes físicos e protocolo de autonomia. Publicar somente resumos sem identificadores pessoais.
7. Investigar ajustes adicionais por ADB em etapa separada; liberar apenas após prova de aplicação e restauração.

## Controles futuros e reversibilidade

Economia padrão, localização, brilho e tempo de tela são investigações separadas. Escrever e reler uma chave não prova o efeito no relógio; confirmar por API independente, interface e comportamento. Para economia padrão, incluir recarga e reinício.

Uma eventual implementação de controles deve manter uma única sessão persistente dos valores anteriores e dos resultados confirmados. Reaplicar não sobrescreve o registro original; restauração preserva conflitos com mudanças posteriores do usuário. Permissões revogadas e falhas parciais devem aparecer no resultado. O registro fica excluído do backup, com recuperação manual documentada. Desinstalar não restaura configurações do sistema.

Esta prévia não aplica esses controles nem solicita `WRITE_SECURE_SETTINGS`.

## Progresso em 19/10/2026

**Estado: prévia v0.1.0 publicada; validação física adiada.** Código, wrapper, CI, documentação e diagnóstico estão no GitHub público. Os builds debug/release, os 17 testes JVM e o lint passaram localmente. O APK está assinado, com certificado fixado e alinhamento verificado; seu download público também foi conferido. A assinatura foi validada com os arquivos do backup privado. A primeira execução do GitHub Actions ainda aguardava runners na última consulta; consulte o histórico público para o estado atual.

O usuário adiou a conexão física e poderá instalar depois, pelo celular com Bugjaeger ou pelo PC com ADB. Esta entrega pode publicar uma prévia sem depender do relógio; nenhum novo pareamento é exigido agora. Continuam pendentes a identificação do firmware, a abertura da confirmação Samsung pelo APK, a seleção em Home, os testes em mostrador e as medições de autonomia. Nenhum resultado de economia está confirmado.

## Premissas

Uso pessoal, repositório público, instalação por APK, ADB aceito e sem root. Acionamento manual e máxima autonomia; LTE desligado é uma preferência do usuário. A autonomia informada de três dias e algumas horas com Bluetooth desligado e economia ativa é uma referência inicial, não uma medição controlada. Mostrador próprio e alteração do atraso de “Só relógio” ficam fora desta versão.
