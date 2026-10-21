# WatchReserve

[![Android](https://github.com/brenopoubel0324/WatchReserve/actions/workflows/android.yml/badge.svg)](https://github.com/brenopoubel0324/WatchReserve/actions/workflows/android.yml)

App pessoal de atalhos e economia para a linha Galaxy Watch com Wear OS, da família Watch4 em diante, incluindo variantes Classic, Pro, FE e Ultra com Android API 30 ou superior. Modelos com Tizen ficam fora do APK. O projeto usa Kotlin e Compose para Wear OS, funciona sem celular e não depende de backend. Veja a [matriz de compatibilidade](docs/compatibility.md).

**Esta é uma prévia técnica. Nenhum modelo foi validado fisicamente e nenhum ganho de autonomia foi medido.** O Watch8 LTE de 40 mm continua a primeira referência. O acesso direto à confirmação Samsung de “Só relógio” depende de prova no firmware do aparelho; o app usa configurações públicas como alternativa e não aplica ajustes globais nem solicita `WRITE_SECURE_SETTINGS`.

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

**[Baixar APK da prévia v0.2.1](https://github.com/brenopoubel0324/WatchReserve/releases/download/v0.2.1/WatchReserve-0.2.1.apk)** · [Checksum SHA-256](https://github.com/brenopoubel0324/WatchReserve/releases/download/v0.2.1/WatchReserve-0.2.1.apk.sha256)

Baixe um APK em [Releases](https://github.com/brenopoubel0324/WatchReserve/releases), confira as notas da versão e siga o [guia de instalação](docs/adb.md): pelo celular com Bugjaeger ou pelo PC com ADB. A instalação e a conexão física foram adiadas a pedido do usuário; não são necessárias para baixar e guardar a prévia. As notas distinguem uma versão compilada de uma versão testada fisicamente e informam o firmware quando houver validação.

Depois da instalação:

1. Configure o duplo clique em Home para abrir “Só relógio”, identificado como WatchReserve em alguns seletores Samsung, caso a opção apareça no seu firmware.
2. Adicione o tile do app à lista de tiles do relógio.
3. Em um mostrador compatível, escolha a complicação do WatchReserve.
4. Use o painel do tile para os atalhos de economia. A confirmação do modo Samsung não é automatizada.

O acesso só será considerado melhor quando reduzir a quantidade de ações até a confirmação de “Só relógio”, comparado ao melhor caminho nativo, a partir do mesmo estado inicial. O fallback pode abrir configurações mais gerais e ainda exigir navegação manual.

Quem já instalou a v0.1.0 pode atualizar pelo APK: o application ID e o certificado continuam iguais. O nome antigo permanece nos artefatos e registros da [primeira release](https://github.com/brenopoubel0324/WatchReserve/releases/tag/v0.1.0), que não foi reescrita. Os detalhes da migração estão no [guia de releases](docs/releases.md).

## Desenvolver

Requisitos: Windows com PowerShell, JDK 17 e Android SDK com API 36 e Platform Tools. `ANDROID_HOME` ou `local.properties` deve apontar para o SDK local. Não versione `local.properties`.

- Application ID preservado: `dev.brenopoubel.watch8battery`.
- Android mínimo: API 30; alvo: API 36.
- Branch principal: `main`.
- Repositório: [brenopoubel0324/WatchReserve](https://github.com/brenopoubel0324/WatchReserve).
- Verificações automatizadas: [GitHub Actions](https://github.com/brenopoubel0324/WatchReserve/actions).

Na raiz do projeto:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

O diagnóstico PowerShell tem testes independentes, com ADB simulado e sem conectar um relógio. Execute no Windows com `pwsh` ou Windows PowerShell 5.1:

```powershell
.\scripts\tests\Test-Diagnostics.ps1
```

Os testes usam uma pasta temporária exclusiva e isolam `LOCALAPPDATA` somente no subprocesso. Cobrem seleção de dispositivo, privacidade padrão, opt-in e proteção de acesso em coletas repetidas. O [registro de resultados](docs/results.md) distingue as verificações históricas da validação de cada versão.

O APK de desenvolvimento fica em `app/build/outputs/apk/debug/app-debug.apk`. Ele usa a chave de debug e não deve ser confundido com uma release assinada pela chave persistente do projeto.

## Documentação e validação

- [Plano e progresso](docs/planos/2026-10-19-watchreserve.md).
- [Compatibilidade por modelo e recursos](docs/compatibility.md).
- [ADB, instalação e coleta privada de diagnóstico](docs/adb.md).
- [Roteiro de teste físico e medição de autonomia](docs/validation.md).
- [Testes autônomos em emulador Wear OS](docs/emulator.md).
- [Auditoria de execução e revisão visual da v0.2.1](docs/auditorias/2026-10-19-execucao.md).
- [Resultados técnicos e limitações conhecidas](docs/results.md).
- [Investigação futura de controles por ADB](docs/experiments.md).
- [Pesquisa: permissões avançadas e “Só relógio”](docs/referencias/permissoes-e-so-relogio.md).
- [Laboratório isolado de controles e restauração](docs/laboratory.md).
- [Assinatura e publicação](docs/releases.md).
- [Estudo do Flashpay: atalhos, interface e limites](docs/referencias/flashpay-wearos.md).

Resultados públicos devem usar somente informações técnicas revisadas. Seriais, endereços de rede, fingerprints, dumps, capturas pessoais, credenciais e chaves não pertencem ao Git. O script de diagnóstico grava os dados fora do repositório e não os envia à rede.

Cada entrega exige revisão funcional e de acabamento em conjunto: navegação, telas pequenas, acessibilidade, rolagem, estados e ícones. O [roteiro de validação](docs/validation.md) separa inspeção de código de teste visual no aparelho.
