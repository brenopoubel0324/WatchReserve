# Watch8-Battery

[![Android](https://github.com/brenopoubel0324/Watch8-Battery/actions/workflows/android.yml/badge.svg)](https://github.com/brenopoubel0324/Watch8-Battery/actions/workflows/android.yml)

App pessoal para Wear OS, criado para facilitar o acesso à economia de bateria no Galaxy Watch8 LTE de 40 mm. O projeto usa Kotlin e Compose para Wear OS, funciona sem celular e não depende de backend.

**Esta é uma prévia técnica. O acesso direto ao modo Samsung “Só relógio” ainda não foi comprovado em um Watch8 físico. Nenhum ganho de autonomia foi medido.** O app usa configurações públicas como alternativa e não aplica ajustes globais nem solicita `WRITE_SECURE_SETTINGS`.

| Entrega | Estado e limite |
|---|---|
| Entrada “Só relógio” no launcher, tile e complicação | Implementados; precisam ser testados no relógio e no mostrador escolhido. |
| Duplo clique em Home | Depende da seleção do app nas configurações Samsung e do teste físico. |
| Abertura direta da confirmação de “Só relógio” | Pendente de investigação no firmware do aparelho. Nenhuma activity Samsung foi presumida. |
| Central de economia | Atalhos manuais para configurações; a disponibilidade depende do firmware. |
| Economia adicional de bateria | Não medida. A referência informada é de três dias e algumas horas, com Bluetooth desligado e economia ativa. |
| Controles por ADB | Fora desta prévia; somente poderão entrar após prova de funcionamento e restauração. |

O app não precisa permanecer aberto e não usa monitoramento contínuo, sensores ou serviços permanentes. O modo “Só relógio” continua sob responsabilidade da Samsung, incluindo a confirmação de entrada e o atraso para mostrar a hora.

## Instalar e usar

Baixe um APK em [Releases](https://github.com/brenopoubel0324/Watch8-Battery/releases), confira as notas da versão e siga o [guia de instalação](docs/adb.md): pelo celular com Bugjaeger ou pelo PC com ADB. A instalação e a conexão física foram adiadas a pedido do usuário; não são necessárias para baixar e guardar a prévia. As notas distinguem uma versão compilada de uma versão testada fisicamente e informam o firmware quando houver validação.

Depois da instalação:

1. Configure o duplo clique em Home para abrir “Só relógio”, identificado como Watch8 Battery em alguns seletores Samsung, caso a opção apareça no seu firmware.
2. Adicione o tile do app à lista de tiles do relógio.
3. Em um mostrador compatível, escolha a complicação do Watch8-Battery.
4. Use o painel do tile para os atalhos de economia. A confirmação do modo Samsung não é automatizada.

O acesso só será considerado melhor quando reduzir a quantidade de ações até a confirmação de “Só relógio”, comparado ao melhor caminho nativo, a partir do mesmo estado inicial. O fallback pode abrir configurações mais gerais e ainda exigir navegação manual.

## Desenvolver

Requisitos: Windows com PowerShell, JDK 17 e Android SDK com API 36 e Platform Tools. `ANDROID_HOME` ou `local.properties` deve apontar para o SDK local. Não versione `local.properties`.

- Application ID: `dev.brenopoubel.watch8battery`.
- Android mínimo: API 30; alvo: API 36.
- Branch principal: `main`.
- Repositório: [brenopoubel0324/Watch8-Battery](https://github.com/brenopoubel0324/Watch8-Battery).
- Verificações automatizadas: [GitHub Actions](https://github.com/brenopoubel0324/Watch8-Battery/actions).

Na raiz do projeto:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

O diagnóstico PowerShell tem testes independentes, com ADB simulado e sem conectar um relógio. Execute no Windows com `pwsh` ou Windows PowerShell 5.1:

```powershell
.\scripts\tests\Test-Diagnostics.ps1
```

Os testes usam uma pasta temporária exclusiva e isolam `LOCALAPPDATA` somente no subprocesso. Cobrem seleção de dispositivo, privacidade padrão, opt-in e proteção de acesso em coletas repetidas. Foram executados com sucesso no PowerShell 7.6 e 5.1.

O APK de desenvolvimento fica em `app/build/outputs/apk/debug/app-debug.apk`. Ele usa a chave de debug e não deve ser confundido com uma release assinada pela chave persistente do projeto.

## Documentação e validação

- [Plano e progresso](docs/planos/2026-10-19-watch8-battery.md).
- [ADB, instalação e coleta privada de diagnóstico](docs/adb.md).
- [Roteiro de teste físico e medição de autonomia](docs/validation.md).
- [Resultados técnicos e limitações conhecidas](docs/results.md).
- [Investigação futura de controles por ADB](docs/experiments.md).
- [Assinatura e publicação](docs/releases.md).

Resultados públicos devem usar somente informações técnicas revisadas. Seriais, endereços de rede, fingerprints, dumps, capturas pessoais, credenciais e chaves não pertencem ao Git. O script de diagnóstico grava os dados fora do repositório e não os envia à rede.
