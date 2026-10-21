# Compatibilidade do WatchReserve

O alvo do projeto é a linha Galaxy Watch com **Wear OS e Android API 30 ou superior**. A Samsung adotou Wear OS a partir da família Watch4; os modelos Tizen anteriores usam outra plataforma e ficam fora deste APK. [Comunicado Samsung](https://developer.samsung.com/galaxy-watch-tizen/notice.html), [tabela oficial dos modelos Tizen](https://developer.samsung.com/galaxy-watch-tizen/device-compatibility.html)

Compatibilidade prevista significa que o app foi desenvolvido para essa plataforma e seus recursos. **Abertura pelo APK, Home, tile e complicação foram comprovados em uso normal em uma combinação de Watch8/firmware.** Isso não valida o ciclo de vida completo, outros modelos ou ganho de autonomia. Resolver um intent ou passar no build também não substitui a abertura pelo app.

## Matriz de alvos

| Família ou variante | Requisito | Estado físico |
|---|---|---|
| Galaxy Watch8 LTE de 40 mm | Wear OS/API 30+; primeira referência do projeto | SM-L325F, Android 16/API 36, firmware L325FXXS3AZF4: confirmação aberta pelo UID comum do APK v0.2.2; Home, tile e complicação confirmados pelo usuário em uso normal em 21/10/2026. Ciclo de vida completo e autonomia pendentes. |
| Famílias Galaxy Watch4 e posteriores | Wear OS/API 30+ no aparelho | Alvo; não testado. |
| Variantes Classic, Pro, FE e Ultra | Unidade com Wear OS/API 30+ | Alvo; não testado. |
| Galaxy Watch, Active, Active2, Watch3 e Gear com Tizen | Plataforma Tizen | Fora do escopo; o APK Android não se aplica. |

As linhas de alvos se sobrepõem intencionalmente: cobrem famílias e variantes, sem usar o nome do modelo como regra de execução. Não há promessa de suporte para outros fabricantes de Wear OS nesta etapa.

## Adaptação por recursos

- **LTE:** o atalho é oferecido quando `PackageManager.hasSystemFeature(FEATURE_TELEPHONY)` informa rádio de telefonia. Não usar uma lista de nomes de modelos. O recurso não comprova eSIM, plano ativo ou conexão LTE; o app não desliga o rádio automaticamente. [Contrato de `FEATURE_TELEPHONY`](https://developer.android.com/reference/android/content/pm/PackageManager#FEATURE_TELEPHONY)
- **Configurações:** destinos públicos dependem de atividades disponíveis no firmware; manter fallback e mensagem quando indisponíveis.
- **Saúde:** procurar os pacotes Samsung Health suportados; ausência não impede os demais atalhos e precisa de orientação clara.
- **Home:** depende de o seletor Samsung permitir o app para duplo clique. **Complicação:** depende dos formatos aceitos pelo mostrador. **Tile:** depende da adição pelo usuário.
- **Tela e interação:** revisar tamanho circular, fonte ampliada, rolagem e uso de bezel/coroa onde existir, sem assumir o mesmo espaço ou hardware entre variantes.

## Prova específica da integração Samsung

A confirmação de “Só relógio” é investigada por aparelho. Uma candidata precisa ser testada pelo próprio APK com suas permissões comuns, pertencer a um pacote do sistema associado às configurações gerais ou de bateria e preservar a confirmação nativa. O sucesso pelo shell ADB não basta.

A prova fica vinculada ao fingerprint do firmware e à versão do pacote candidato. Mudança em qualquer um invalida o destino específico até nova validação; a atividade de bateria pode pertencer a um pacote diferente das configurações gerais. Uma prova no Watch8 não valida outro Galaxy Watch ou firmware automaticamente. Fingerprint e dumps ficam privados; publique somente versões técnicas revisadas e resultados.

Use o [roteiro funcional e de acabamento](validation.md) para registrar evidências. A matriz só muda para validado após esses testes; redução de ações e ganho de autonomia são resultados separados.
