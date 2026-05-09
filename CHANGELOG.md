# Changelog

Notas de release do Nonnas Stock. Formato baseado em [Keep a Changelog](https://keepachangelog.com/), versionamento [SemVer](https://semver.org/).

A primeira versão estável será `1.0.0` (entrega T18). `1.0.0-rc.1` (T15) é o snapshot pré-release com smoke E2E completo, docs operacionais e auditoria de segurança inicial.

---

## [1.0.0-rc.1] — 2026-05-09

### Highlights

Primeiro release candidate público. Sistema funcional ponta-a-ponta cobrindo cadastros, operações, alertas, relatórios, dashboard e smoke E2E. Pendentes para `v1.0.0`: hardening de segurança/LGPD (T16), observabilidade externa (T17) e DR/runbooks ampliados (T18).

### Adicionado — backend

- **Identity**: empresa, filial, usuário, auth JWT com refresh rotation, brute force protection, troca obrigatória no 1º login (T02).
- **Catalog**: insumos, fornecedores, categorias, unidades de medida, ConversorUnidadeService (T03).
- **Inventory-core**: lotes, saldo materializado por filial+insumo+lote, movimentações imutáveis, política FEFO (T04).
- **Recipes**: produto vendável, ficha técnica versionada (uma vigente por produto), simulação de venda com snapshot (T05).
- **Operations**: state machine de transferências (Solicitada→Aprovada→Em trânsito→Recebida), AjusteEstoque com threshold, CargaInicial idempotente, parser CSV/XLSX (T06).
- **Alerts**: configurações com escopo flexível (insumo/filial/rede), 4 tipos (RUPTURA, ESTOQUE_MINIMO_PERCENTUAL, ESTOQUE_MINIMO_ABSOLUTO, VENCIMENTO_PROXIMO_DIAS), auto-resolução, prioridades (T07).
- **Reporting**: 6 relatórios via SQL nativo cross-context (Posição, Curva ABC, Ruptura, Vencimento, Movimentação por Período, Divergência), MVs com refresh agendado (T08).
- **App**: agregação dos 7 bounded contexts, GlobalExceptionHandler RFC 7807 em web-commons, OpenAPI/Springdoc com 7 tags + JWT bearer, rate limit Bucket4j 100 req/min/IP, 4 perfis de profile (defaults/dev/test/prod) (T09).
- **CRUDs T13**: GET /{id}, PUT, PATCH ativar/desativar e filtros server-side em Filial/Insumo/Fornecedor/Produto. Ficha técnica histórico de versões. Carga inicial preview separado da confirmação.
- **Listar transferências com filtros** por filialId (origem ou destino) e status (T14).

### Adicionado — frontend

- **Stack**: Vite 5.4 + React 18.3 + TypeScript 5.6 strict + Tailwind 3.4 + shadcn/ui paleta Nonnas (`brand.red #D62828`, `brand.green #2A9D3F`) (T12).
- **Auth**: LoginPage com react-hook-form + zod, Zustand store, axios interceptor JWT + 401 redirect (T12).
- **Cadastros**: Filiais (com link para carga inicial), Insumos (filtros categoria/ativo/busca), Fornecedores (ativo/busca), Produtos (categoria/ativo/busca), Fichas técnicas com editor + histórico (T13).
- **Carga inicial bimodal**: upload xlsx/csv com preview separado e linha-a-linha gerando CSV em memória (T13).
- **Operações**: Estoque (posição cruzada com ruptura/vencimento), Movimentações (tabs entrada/saída/histórico), Transferências (5 estados + ações por status), Alertas (tabs configurações + disparados) (T14).
- **Dashboard**: 4 cards (filiais ativas, alertas, transferências em trânsito, ruptura) + BarChart Recharts top 10 ruptura + lista em-trânsito agregado (T14).
- **Filtro global de filial** no header via Zustand+sessionStorage afeta todas as views (T14).

### Adicionado — qualidade

- **ArchUnit** 13 regras (5 layered + 8 isolamento entre bounded contexts) (T10).
- **JaCoCo aggregate** + threshold 85% domain / 75% application (T10).
- **PIT mutation testing** profile `nightly` com threshold 70% (T10).
- **AbstractIntegrationTest** distribuído via shared-kernel test-jar (ADR 0011) (T10).
- **Trivy filesystem scan** no PR — falha em CRITICAL/HIGH com fix disponível (T15).
- **Smoke E2E Playwright** com 8 cenários ponta-a-ponta (login, filial+carga, insumo+ficha, entrada, saída, transferência, alerta, posição) (T15).

### Adicionado — operações & docs

- **CI/CD**: workflows `pr.yml` (verify + JaCoCo + Trivy), `main.yml` (Docker → GHCR + semantic-release), `nightly.yml` (PIT sexta + Gatling sábado stub), `e2e.yml` (Postgres + backend + frontend + Playwright) (T11+T15).
- **Docker**: multi-stage `eclipse-temurin:21-jre-alpine`, usuário não-root, healthcheck via `/actuator/health/liveness` (T11).
- **semantic-release** com regras Conventional Commits (T11).
- **Backup**: `scripts/backup-postgres.sh` + `scripts/restore-validate.sh` para validação mensal de recuperabilidade (T15).
- **Docs**: `docs/deployment.md`, `docs/operations-runbook.md`, `docs/onboarding.md` (T15).
- **ADRs**: 0001 modular monolith, 0007 embedded Postgres, 0010 reporting cross-context SQL nativo, 0011 AbstractIntegrationTest via test-jar.

### Métricas

- **Reactor `mvn verify`**: 13/13 SUCCESS em ~04:15 (alvo era 5 min).
- **Frontend bundle**: 1008 KB / 295 KB gzip (Recharts é ~40% disso; code-split planejado pra próximas releases).
- **Cobertura**: > 75% application, > 85% domain por bounded context.
- **Smoke E2E**: 8 cenários cobrindo o fluxo crítico do operador.

### Conhecidas limitações

- Sem 2FA, audit log Envers, Hibernate Envers, política de senha custom — chega em T16.
- Sem Sentry, Prometheus, Grafana — chega em T17.
- Sem DR off-site automatizado — chega em T18.
- ArchUnit tem 2 regras suspensas com TODO linkando T16/T17 (auth ports e ImportarPlanilhaUseCase). Veja `quality-tests/.../LayeredArchitectureTest.java`.
- E2E roda contra `vite preview` em CI; em produção a entrega de assets é via Nginx (ver `docs/deployment.md`).
