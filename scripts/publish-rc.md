# Publicação manual do release `v1.0.0-rc.1`

A tag local `v1.0.0-rc.1` foi criada (e aponta para o commit que entregou T15). Esta nota documenta o que falta fazer no GitHub — passos que dependem de decisão humana (Ewerton + Jefferson) e credenciais que não vivem na máquina do agente.

## Pré-requisitos

- Permissão de push em `git@github.com:jeffersonago007/nonnas-stock`.
- `gh` CLI autenticado (`gh auth status`).

## 1. Conferir a tag local

```bash
git tag -l 'v1.0.0-rc.1'
git show v1.0.0-rc.1 --stat
```

A mensagem de tag deve resumir T01–T15. Se quiser editar, exclua e recrie:

```bash
git tag -d v1.0.0-rc.1
git tag -a v1.0.0-rc.1 -m "Release candidate 1: T01-T15 entregues, smoke E2E green"
```

## 2. Push da tag (depois de aprovação do Ewerton)

```bash
git push origin v1.0.0-rc.1
```

O workflow `main.yml` **não** dispara automaticamente em push de tag (ele responde a push em `main`). Tags são manuais por design — a primeira semantic-release depois de v1.0.0-rc.1 vai gerar v1.0.0 final.

## 3. Criar GitHub Release com binários

```bash
# Build do JAR (se ainda não estiver no target/)
./mvnw -pl app package -DskipTests
ls app/target/app.jar

# Cria release a partir da tag, anexa o JAR, abre release notes
gh release create v1.0.0-rc.1 \
  app/target/app.jar \
  --title "Nonnas Stock v1.0.0-rc.1" \
  --notes-file CHANGELOG.md \
  --prerelease
```

`--prerelease` mantém o release marcado como "Pre-release" no GitHub — sinaliza que ainda não é o `v1.0.0` final (esse vem em T18).

## 4. Smoke pós-release

- Pull da imagem Docker recém-publicada (vai como `sha-<hash>` e `latest` pelo `main.yml`).
- Subir em staging (se já existir) e rodar suíte E2E manualmente:
  ```bash
  ./mvnw -pl e2e test -Pe2e \
    -De2e.base.url=https://staging.nonnas.com.br \
    -De2e.api.url=https://staging.nonnas.com.br
  ```
- Se passar, comunicar Jefferson e Ewerton; se falhar, reverter release no GitHub e abrir issue.

## 5. Próximos releases

Após v1.0.0-rc.1, semantic-release pode assumir o ritmo:

- `feat:` → minor (v1.1.0)
- `fix:` → patch (v1.0.1)
- `chore:`, `test:`, `docs:` → no release

A tag `v1.0.0` (sem -rc) só sai depois de T18 (DR + runbooks completos), com release notes consolidadas e GA.
