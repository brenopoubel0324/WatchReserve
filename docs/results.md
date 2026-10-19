# Resultados técnicos

Última revisão: 19/10/2026.

## Evidência disponível

| Item | Resultado |
|---|---|
| Dispositivo-alvo informado | Galaxy Watch8 LTE de 40 mm; versão exata do firmware ainda não registrada. |
| Autonomia informada | Três dias e algumas horas, com Bluetooth desligado e economia ativa; sem protocolo controlado. |
| Conexão ADB com Watch8 nesta sessão | Adiada pelo usuário; não necessária para publicar a prévia. |
| Destino Samsung de “Só relógio” acessível pelo APK | Não comprovado. Não foi presumido um componente privado. |
| Atalho nativo mais curto | Ainda não medido. |
| Home, tile e complicação no aparelho | Pendente. |
| Firmware e versão do pacote da activity candidata | Pendente de coleta privada; o pacote pode diferir das configurações gerais. |
| Economia adicional | Não medida; nenhum ganho declarado. |
| Controles globais / `WRITE_SECURE_SETTINGS` | Não habilitados nesta prévia. |

## Verificações do diagnóstico local

O script `Collect-WatchDiagnostics.ps1` passou por análise sintática e execução com ADB simulado no PowerShell 7.6 e Windows PowerShell 5.1. Foram verificados: nenhum aparelho, múltiplos aparelhos exigindo seleção, seleção desconectada, ausência de identificadores e dumps por padrão, opt-in para dados privados, repetição da coleta com ACL restrita e uso exclusivo de consultas de leitura. Esses testes não acessaram um relógio real.

O teste reproduzível está em [`scripts/tests/Test-Diagnostics.ps1`](../scripts/tests/Test-Diagnostics.ps1). Ele executa um subprocesso com `LOCALAPPDATA` temporário, mantém o ambiente do processo pai e remove somente a fixture temporária validada.

Links locais da documentação foram verificados. Os resultados de build, lint, testes do app e assinatura devem ser registrados nas notas de cada release após execução; não são substituídos pela validação do script.

## Estados que não devem ser confundidos

- **Compilado:** o APK passou pelo build; isso não confirma o comportamento Samsung.
- **Intent resolvido:** o sistema declara um destino; a tela e o número de ações ainda precisam ser observados.
- **Tela acessível pelo APK:** o app abriu a tela com suas próprias permissões; chamadas feitas pelo shell ADB não bastam.
- **Atalho útil:** houve redução de ações até a confirmação, comparando o mesmo estado inicial com o melhor caminho nativo.
- **Economia medida:** comparações repetidas e equivalentes sustentam uma diferença além da variação observada.

## Verificações locais desta prévia

- APK de desenvolvimento compilado com `assembleDebug`.
- 17 testes JVM passaram: prova de destino, seleção de provedores do sistema e integridade das medições.
- `lintDebug`: zero erros e dez avisos de versões mais recentes de dependências; versões fixadas compatíveis com API 36.
- APK de release otimizado com R8 e `lintVitalRelease` concluídos.
- APK assinado com certificado persistente; `apksigner verify` e `zipalign -c -P 16 4` passaram.
- O certificado do APK corresponde ao hash público em `signing-certificate.sha256`.
- O APK final não é depurável e declara somente a permissão de assinatura interna adicionada pelo AndroidX; não solicita internet, sensores ou escrita de configurações.
- Diagnóstico simulado passou em PowerShell 7.6 e Windows PowerShell 5.1, incluindo privacidade, seleção de aparelho e reaplicação de ACL.
- Revisão independente do código e dos scripts sem achados bloqueantes ou altos após as correções.

O [workflow Android](https://github.com/brenopoubel0324/Watch8-Battery/actions/workflows/android.yml) verifica o app no Linux e o diagnóstico nas duas versões de PowerShell no Windows. Seu histórico público registra o resultado de cada execução.

## Próxima evidência necessária

Quando o usuário decidir instalar, usar Bugjaeger no celular ou ADB no PC. A investigação posterior exige coletar versões de forma privada, executar a descoberta somente leitura e testar a abertura pelo próprio APK. Depois registrar os três atalhos, o ciclo de vida e as medições do [roteiro físico](validation.md). Nenhuma conexão física é exigida para esta entrega da prévia.

Após cada etapa, atualizar esta página e as notas da release com o resultado real. Fingerprints, seriais, rede, dumps e dados pessoais ficam fora deste repositório público.
