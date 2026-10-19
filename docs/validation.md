# Validação física e autonomia

Este roteiro deve ser executado em cada combinação de modelo e firmware antes de declarar o atalho validado ou afirmar ganho de autonomia. O Galaxy Watch8 LTE de 40 mm continua a primeira referência, com teste físico adiado. A compilação e os testes locais cobrem lógica e contratos Android; não provam o comportamento do firmware Samsung. Veja os [alvos de compatibilidade](compatibility.md).

## Registrar o contexto

Guarde localmente data, versão do APK, versão do Android/Wear OS/One UI Watch, firmware, versão do pacote da activity candidata e mostrador. O pacote da tela de bateria pode diferir do pacote de configurações gerais; registre ambos quando necessário. Use o script de diagnóstico para a parte privada. No resumo público, registre somente versões técnicas revisadas, sem serial, IP, fingerprint ou dumps.

Antes de cada comparação, registre Bluetooth, Wi-Fi, LTE, economia de energia, AOD, gesto para acender, brilho, tempo de tela, medições de saúde, treino e sincronizações. A referência inicial do usuário mantém Bluetooth desligado e economia ativa; desligar novamente essas opções não pode ser contado como ganho adicional.

## Revisão funcional e de acabamento

Toda alteração de funcionalidade deve incluir revisão de acabamento na mesma entrega. Marque separadamente **inspecionado no código**, **observado em emulador** e **validado fisicamente**, com versão, modelo/tamanho e firmware. Um resultado de lint não substitui observação visual. A lista abaixo é um roteiro; não declara testes já executados na v0.2.0.

- [ ] **Telas pequenas:** conteúdo legível, sem cortes nas bordas circulares e com os botões finais alcançáveis no menor tamanho-alvo.
- [ ] **Acessibilidade:** escala de fonte normal e ampliada, contraste, alvos de toque confortáveis e descrições úteis para elementos acionáveis sem texto.
- [ ] **Voltar:** retorna de Validar atalho e Medição manual ao painel sem criar ciclos; saída coerente a partir do painel e dos atalhos.
- [ ] **Rolagem:** conteúdo completo acessível, sem prender o gesto; testar toque e coroa/bezel onde disponível.
- [ ] **Estados:** bateria e mensagens atualizadas ao voltar ao primeiro plano; ausência de destino, recurso indisponível e falhas recuperáveis com instruções claras.
- [ ] **Ícones e nomes:** WatchReserve consistente no app, tile e complicação; ícones distintos e legíveis, sem depender só de cor para comunicar estado.
- [ ] **Recursos opcionais:** LTE ausente sem `FEATURE_TELEPHONY`; Saúde atende aos pacotes Samsung suportados ou apresenta alternativa clara quando ausente.
- [ ] **Atalhos:** Home, tile e complicação usam o destino esperado e mantêm a confirmação Samsung.

Registre achados e correções antes de fechar o bug sweep. Mantenha pendente a verificação visual/física que ainda não ocorreu; uma inspeção de código não permite declarar um modelo validado.

## Atalhos e contagem de ações

Use o mesmo estado inicial em cada tentativa: relógio acordado no mostrador, sem menus abertos. A ação de acordar fica fora dessa métrica e deve ser testada separadamente se for relevante. Conte cada toque ou gesto individual: por exemplo, duplo clique em Home conta como duas pressões. Registre também tempo e confiabilidade, pois menos ações não garante menor demora.

| Caminho | Ações até a confirmação | Tempo | Tela final | Repetições/falhas |
|---|---:|---:|---|---|
| Melhor caminho nativo disponível | Pendente | Pendente | Pendente | Pendente |
| Duplo clique em Home | Pendente | Pendente | Pendente | Pendente |
| Tile | Pendente | Pendente | Pendente | Pendente |
| Complicação | Pendente | Pendente | Pendente | Pendente |

Faça pelo menos três tentativas por caminho. Inclua os gestos necessários para chegar ao tile. Use a complicação no mesmo mostrador inicial. Separe abrir a confirmação de ativar “Só relógio”; a ativação continua manual.

Aceite do atalho: ao menos um caminho reduz ações até a confirmação, comparado ao melhor acesso nativo comprovado. Abrir as configurações gerais sem reduzir ações é um fallback funcional, mas não atende esse objetivo. Disponibilidade no seletor de Home e compatibilidade do mostrador precisam ser registradas, sem prometer suporte universal.

## Ciclo de vida e falhas

Teste Home, tile e complicação em cada situação:

- Instalação nova e retorno a partir do painel de economia.
- App fora do primeiro plano; processo encerrado normalmente pelo sistema ou com `am kill` após sair do app.
- Reinício do relógio, sem abrir o painel antes de usar cada atalho.
- Ausência do destino de economia: erro recuperável, instrução clara e fallback para configurações gerais.
- Mudança de firmware ou versão do pacote candidato: qualquer destino Samsung previamente comprovado deve ser invalidado.
- Parada forçada como cenário separado, seguida de reabertura explícita do app e nova interação com a complicação e o tile.

Comandos opcionais de teste, com o dispositivo escolhido explicitamente:

```powershell
adb -s $watchSerial shell am kill dev.brenopoubel.watch8battery
adb -s $watchSerial shell am force-stop dev.brenopoubel.watch8battery
```

`am kill` pode não encerrar um processo ainda considerado necessário. Confirme o resultado antes de registrar o caso como executado. Parada forçada é diferente de morte normal do processo: no Android 15 ou superior, o sistema cancela os `PendingIntent`s da aplicação parada. Não prometa que uma complicação previamente renderizada continue funcionando antes da reabertura; depois de reabrir o app, confirme que os dados e o toque da complicação se recuperam. Se necessário, solicite atualização pela interface ou adicione novamente a complicação, registrando essa limitação. [Mudanças oficiais do Android 15](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state)

## Medição de autonomia

Separe três condições:

- **A — referência atual:** configuração atual do usuário, sem o APK instalado.
- **B — custo do app:** mesma configuração de A, com o APK instalado e sem ajustes adicionais.
- **C — ajustes adicionais:** mesma base de B, com um conjunto de alterações explicitamente registrado e já validado. Nesta prévia, C ainda não está disponível.

Compare A/B em três pares de janelas de 24 horas com rotinas semelhantes, alternando a ordem quando possível. Faça depois três pares B/C se controles adicionais forem liberados. Evite recarga, atualização de software, instalação de apps e mudança de mostrador dentro de uma janela. Registre treino, chamadas, sincronização e incidentes. Após instalar, atualizar ou recarregar, permita estabilização equivalente antes de iniciar cada condição.

Durante todas as janelas, desconecte o ADB e desligue a depuração no relógio. Não deixe serviços de medição ou polling contínuo alterarem o consumo que está sendo avaliado. Registre bateria no começo e no final por leitura manual.

```text
descarga (pontos percentuais/hora) = (bateria inicial - bateria final) / horas
```

Mantenha uma tabela local com condição, início/fim, percentuais, duração real, configuração e ocorrências. Publique somente o resumo revisado. Compare os pares e apresente a dispersão, sem extrapolar “dias de bateria” a partir de um intervalo curto. Se a diferença for semelhante à variação entre dias, reporte **inconclusivo**. Testes curtos verificam falhas evidentes, mas não substituem as janelas de 24 horas. [Orientações de energia do Wear OS](https://developer.android.com/training/wearables/apps/power)

## Controles futuros

Cada ajuste depende de teste independente de aplicação, leitura por API, efeito visual e comportamento. Reler a mesma chave escrita não basta. Economia padrão exige recarga e reinício. Restauração deve cobrir aplicação parcial, reaplicação, mudança manual posterior, processo encerrado e permissão revogada. Desinstalar não é uma estratégia de restauração.

Não habilite um perfil de ajustes antes dessas provas. Se uma configuração não puder ser controlada de forma confiável, mantenha apenas o atalho manual.
