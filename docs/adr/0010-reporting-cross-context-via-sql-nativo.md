# ADR 0010 — Reporting como módulo read-only via SQL nativo cross-schema; perf via JUnit + Duration

- **Status:** Aceita
- **Data:** 2026-05-09
- **Contexto da decisão:** T08 reporting precisa agregar dados de catalog, inventory-core, operations e (eventualmente) alerts em consultas pesadas. Precisamos de uma estratégia consistente de acesso e um critério de performance pragmático para o MVP.

## Contexto

Reporting tem 6 use cases de leitura (posição, curva ABC, ruptura, vencimento, movimentação por período, divergência). As views materializadas e queries diretas tocam tabelas de **quatro bounded contexts** simultaneamente:
- `insumos`, `categorias_insumo`, `unidades_medida`, `insumos_filiais` (catalog)
- `lotes`, `saldos_lotes`, `movimentacoes`, `items_movimentacao` (inventory-core)
- `ajustes_estoque` (operations)
- `alertas_config` (alerts — referência indireta via thresholds)

Duas opções foram consideradas:

**Opção A — Importar repositórios Java dos outros módulos.** Reporting injeta `MovimentacaoRepository`, `LoteRepository`, etc. Faz `findAll(filtros)` e agrega em memória.
- Prós: respeita encapsulamento Java; ArchUnit valida fácil.
- Contras: agregação em memória de 10k+ registros é proibitiva; impossível usar views materializadas; impossível usar `GROUP BY`/`window functions` do banco; o "API JPA" dos repositórios não foi pensado para queries analíticas.

**Opção B — SQL nativo cross-schema, sem importar classes Java.** Reporting depende dos outros módulos via Maven só para arrastar as migrations (Flyway varre o classpath); o código Java acessa o banco via `NamedParameterJdbcTemplate` com SQL pleno.
- Prós: usa o motor analítico do Postgres (MV, índices, window functions); zero overhead de mapeamento; performance natural.
- Contras: acopla reporting às tabelas físicas dos outros módulos. Renomear coluna em `inventory-core` quebra reporting silenciosamente (sem garantia de compilação).

## Decisão

**Adotamos a Opção B com isolamento explícito:**

1. **Reporting acessa tabelas de outros bounded contexts via SQL nativo.** Implementação concentrada em `RelatorioQueriesJdbc` (port `RelatorioQueries`), única classe que escreve SQL multi-schema.
2. **Reporting NÃO importa classes Java de catalog/inventory-core/operations/alerts.** O `pom.xml` declara as dependências apenas para garantir que as migrations Flyway dessas dependências fiquem no classpath.
3. **Schema dedicado `reporting`** abriga as views materializadas (`mv_curva_abc`, `mv_ruptura_iminente`). Migrations criam o schema; CREATE/REFRESH ficam isolados ali.
4. **ArchUnit (T10) reforçará formalmente** que `com.nonnas.reporting.*` não importa de `com.nonnas.{catalog,inventory,operations,alerts}.*`. Até T10, a regra é convencional e validada em code review.

## Critério de performance — JUnit + Duration em vez de Gatling micro

O master doc (seção 11, T08) pede "teste com Gatling micro" para validar < 500ms em 10k movimentações. Adotamos **JUnit `Duration` puro** com warm-up explícito e `assertThat(elapsed).isLessThan(LIMITE)`. Razões:

- O critério é binário (passa/falha em 500ms), não exige histograma de latência nem percentis.
- Adicionar `gatling-charts-highcharts-bundle` ao pom de reporting traz Akka, Scala stdlib e ~30MB de deps que não usamos para mais nada.
- Os ITs já rodam contra Postgres real (Zonky), então a medida é representativa.
- Gatling permanece no roadmap para T15 (hardening final), quando faz sentido medir capacidade real do app sob carga distribuída.

Os testes de performance em T08 ficam em `ReportingPerformanceIT`:
- `posicaoEm1000Lotes_executa_em_menos_de_500ms` — critério explícito do master doc.
- `movimentacaoPorPeriodoEm10kMovs_executa_em_menos_de_500ms` — critério geral 10k movs.

## Consequências

**Positivas:**
- Performance previsível: o motor do banco faz o que sabe fazer melhor.
- Views materializadas viáveis (curva ABC, ruptura) com `REFRESH MATERIALIZED VIEW CONCURRENTLY` agendado a cada 30 min.
- Pom de reporting enxuto — sem Gatling, Akka, Scala.
- ITs rodam segundos, não minutos.

**Negativas:**
- Reporting acoplado à estrutura física do banco — refactor de coluna em outro módulo pode quebrar silenciosamente.
- Sem checagem de tipos em compile-time entre as queries SQL e as colunas reais.

**Mitigações:**
- ITs cobrem cada relatório com Zonky+Flyway carregando todas as migrations — refactor que renomeie coluna quebra os ITs imediatamente.
- ArchUnit (T10) bloqueia importação Java cross-context.
- Limite consciente: se reporting precisar de cálculo de domínio, ele pede via API REST do módulo dono, não via SQL. SQL aqui é estritamente para agregar fatos físicos.
- Em T15/T18, se o acoplamento se mostrar custoso, podemos extrair as views materializadas para uma DB-as-API com schema/views próprios e leitura por Reporting de views (não tabelas).

## Referências

- ADR 0001 — Modular Monolith (princípio: comunicação via contratos; este ADR registra o desvio consciente para queries analíticas).
- ADR 0008 — recipes depende de inventory-core via Maven (precedente de dependência cross-context — aqui generalizado para read-only).
- PROMPT_CLAUDE_CODE.md seção 11 T08 (escopo do reporting e critério de < 500ms).
