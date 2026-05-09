# Nonnas Stock — E2E (Playwright Java)

Smoke tests automatizados do master doc (seção 7.3). Cobre 8 cenários ponta-a-ponta — login → cadastrar filial → carga inicial → insumo+ficha → entrada/saída → transferência → alerta → posição.

## Pré-requisitos para rodar localmente

1. **Backend** rodando em `http://localhost:8080`:
   ```bash
   ./mvnw -pl app spring-boot:run
   ```

2. **Frontend** rodando em `http://localhost:5173`:
   ```bash
   cd frontend && npm run dev
   ```

3. **Postgres** acessível ao backend (configurado via `app/src/main/resources/application.yml`).

4. **Browsers Playwright** instalados (uma vez):
   ```bash
   ./mvnw -pl e2e exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
   ```

## Executar

```bash
./mvnw -pl e2e test -Pe2e
```

Headless por default. Para abrir browser visível durante debug:

```bash
./mvnw -pl e2e test -Pe2e -De2e.headless=false
```

Screenshots de cada teste ficam em `e2e/target/e2e-screenshots/`.

## Variáveis de configuração

| Property             | Default                       | Uso                           |
|----------------------|-------------------------------|-------------------------------|
| `e2e.base.url`       | `http://localhost:5173`       | URL do frontend (Vite dev)    |
| `e2e.api.url`        | `http://localhost:8080`       | URL da API backend            |
| `e2e.headless`       | `true`                        | Browser visível ou não        |

## Por que os testes pilham estado entre si

Os 8 cenários rodam em ordem (`@TestMethodOrder(OrderAnnotation.class)`) e compartilham fixtures via campos `static`. Um cenário cria filial; o próximo usa essa filial. Tradeoff consciente:

- **Prós**: simula um "primeiro uso" real do sistema (deploy fresco), executa rápido (~3 min total), reflete fluxo do operador.
- **Contras**: falha em cenário N pode mascarar problemas em cenário N+1. Aceitável para smoke; testes de regressão profunda ficam para a suíte unitária + ITs por bounded context.

CI (workflow `.github/workflows/e2e.yml`) sobe Postgres + backend + frontend em containers e roda esta suíte uma vez por pipeline.
