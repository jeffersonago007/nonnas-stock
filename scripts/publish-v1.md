# Publicação de `v1.0.0` no GitHub

A tag local `v1.0.0` foi criada em `7cffa33`. Os passos de publicação ficam para Jefferson/Ewerton — exigem credenciais GitHub (push remoto) e decisão final de "estamos prontos" pós-revisão.

## Pré-requisitos

- Permissão de push em `git@github.com:jeffersonago007/nonnas-stock`.
- `gh` CLI autenticado (`gh auth status`).
- Build do JAR finalizado (`./mvnw -pl app package -DskipTests`).
- Ewerton aprovou via revisão de código + leitura do `CHANGELOG.md` e dos 6 runbooks.

## 1. Conferir a tag

```bash
git tag -l 'v1.0.0'
git show v1.0.0 --stat
```

A mensagem deve cobrir T01-T18 com pendentes pós-1.0 explícitos. Se quiser ajustar, exclua e recrie:

```bash
git tag -d v1.0.0
git tag -a v1.0.0 -m "..."
```

## 2. Push

```bash
git push origin main
git push origin v1.0.0
```

O workflow `main.yml` dispara em push de `main` (não de tag) — o build de imagem Docker e push para GHCR já vai acontecer no commit do T18.

## 3. Build do binário pra anexar

```bash
./mvnw -pl app package -DskipTests
ls -lh app/target/app.jar
```

Esperado: ~80-100 MB (Spring Boot fat jar com tudo).

## 4. Build do frontend (opcional, pra anexar)

```bash
cd frontend
npm ci
npm run build
cd dist && tar czf ../../frontend-dist.tar.gz . && cd ../..
ls -lh frontend-dist.tar.gz
```

## 5. Criar o GitHub Release

```bash
gh release create v1.0.0 \
    app/target/app.jar \
    frontend-dist.tar.gz \
    --title "Nonnas Stock v1.0.0" \
    --notes-file CHANGELOG.md
```

Sem flag `--prerelease` (essa é a 1.0 final).

Alternativa: edição via web em https://github.com/jeffersonago007/nonnas-stock/releases/new se preferir fechar pela UI.

## 6. Smoke pós-release

- Pull da imagem `ghcr.io/jeffersonago007/nonnas-stock:v1.0.0` (criada pelo workflow main em push de tag — pode levar 5-10min).
- Subir em staging seguindo `docs/deployment.md`.
- Smoke manual: login admin, dashboard, criar uma filial, lançar uma entrada, conferir alerta dispara, abrir notificação.
- Restaurar último backup em ambiente isolado pra validar o pipeline fim-a-fim.

Se passar, comunicar internamente: produto está GA.

## 7. Pós-1.0 imediato

Itens listados em `STATUS.md` "Conhecidas limitações pós-1.0.0" ficam no backlog:

1. Simulação DR completa com Ewerton (roteiro em `docs/post-mortems/simulacao-dr-pre-golive.md`).
2. Hibernate Envers nas entidades.
3. Métricas custom de negócio.
4. Source maps Sentry no workflow main.
5. WAL archiving para reduzir RPO.

Cada um vira issue tagged `pos-1.0` no GitHub. Priorização em retro do golive.
