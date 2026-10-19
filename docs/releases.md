# Assinatura e publicação

As releases são publicadas no repositório público [WatchReserve](https://github.com/brenopoubel0324/WatchReserve/releases), associadas a uma tag. O código, o Gradle Wrapper e a CI ficam versionados; as chaves permanecem privadas.

## Migração de nome na v0.2.0

O projeto passa de Watch8-Battery para **WatchReserve**, com pasta local `C:\Users\breno\code\GitHub\WatchReserve`, repositório `brenopoubel0324/WatchReserve` e APK `WatchReserve-0.2.0.apk`. O foco passa a abranger Galaxy Watch com Wear OS/API 30 ou superior; veja a [matriz de compatibilidade](compatibility.md).

Application ID `dev.brenopoubel.watch8battery`, nomes dos componentes Android, alias e certificado de assinatura permanecem iguais intencionalmente. Não renomeie a chave para gerar uma nova identidade: preservar a assinatura permite atualizar a instalação anterior com `install -r`. As pastas privadas de assinatura e backup também preservam os nomes legados descritos abaixo.

O histórico da v0.1.0 permanece intacto, incluindo tag, commit, bytes, hash e nome `Watch8-Battery-0.1.0.apk`; somente os links passam a usar a URL canônica do repositório renomeado. Novas coletas de diagnóstico vão para `%LOCALAPPDATA%\WatchReserveDiagnostics` com prefixo `watchreserve-`. Relatórios privados antigos não são migrados automaticamente.

## Chave persistente

Use uma chave de assinatura persistente para que uma nova release possa atualizar o APK instalado. Guarde keystore, senhas e alias fora do repositório, com backup privado em local separado e recuperação testada. Não copie a chave para artefatos de CI, issues, documentação ou assets da release. Se ela for perdida, não será possível assinar uma atualização compatível com os APKs anteriores.

O script local `scripts/Build-Release.ps1` mantém a assinatura em `%LOCALAPPDATA%\Watch8BatterySigning` e uma cópia privada em `%USERPROFILE%\.watch8-battery-signing-backup`, com ACL restrita. Nenhuma senha ou chave é versionada. Execute-o na raiz do projeto conforme a ajuda do próprio script:

```powershell
.\scripts\Build-Release.ps1
```

A saída assinada fica na pasta `app/build/outputs/apk/release/`, que não é versionada. A cópia em outro caminho do mesmo PC protege contra perda acidental do arquivo, mas não contra perda do computador; mantenha também um backup privado em outro meio.

O build de debug usa sua própria chave de desenvolvimento. Ele não substitui uma release assinada e pode exigir um fluxo de instalação diferente quando o aparelho já tiver uma versão com outra assinatura.

O arquivo público `signing-certificate.sha256` fixa a identidade do certificado da primeira release. O script rejeita uma chave diferente e não gera outra se esse arquivo já existir. O hash é público; a chave e a senha continuam privadas. O APK final é verificado com `apksigner` e alinhamento de páginas de 16 KiB antes da publicação.

## Antes de publicar

- Executar build, lint, testes focados e bug sweep; revisar Git, diff e contratos.
- Conferir permissões do APK final e a ausência de credenciais, diagnósticos privados e dados do relógio.
- Verificar a assinatura do APK de release e gerar seu SHA-256.
- Criar commit com título e corpo descritivo em PT-BR, respeitando Conventional Commits e as regras locais.
- Criar tag para o commit exato e publicar o APK, o checksum e notas objetivas.

O workflow de verificação deve usar permissões de leitura. Uma release pode ser publicada manualmente a partir do ambiente local autenticado; não é necessário dar permissão de escrita ao workflow de build nem fornecer a chave de assinatura ao GitHub.

## Notas mínimas de cada versão

Informe versão/tag, commit, verificações executadas, SHA-256 do APK, revisão funcional e de acabamento, estado de teste físico e limitações conhecidas. Antes de testes físicos, use claramente:

> Prévia técnica. Nenhum modelo Galaxy Watch foi validado fisicamente. Acesso direto ao modo Samsung “Só relógio” e ganho de autonomia ainda não comprovados.

Quando houver teste físico, informe modelo e versões técnicas revisadas, sem serial nem fingerprint. Diferencie abertura de configurações, abertura da confirmação e redução real de ações.

Nesta prévia o app não altera ajustes globais. Se controles forem adicionados no futuro, documente como restaurá-los antes da remoção: desinstalar não restaura configurações do sistema.
