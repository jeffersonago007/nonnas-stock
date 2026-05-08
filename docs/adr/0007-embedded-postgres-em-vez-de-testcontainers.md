# ADR 0007 — Embedded Postgres (Zonky) em vez de Testcontainers, no MVP 1.0

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Bloqueio de instalação de Docker na máquina dev do PO

## Contexto

A regra invariante da seção 0 do master document obriga: "Nunca usar H2 em testes que tocam banco. Sempre Postgres via Testcontainers." Testcontainers requer Docker rodando localmente.

A estação de trabalho do Product Owner não comporta a instalação do Docker Desktop (motivo não-técnico — restrição de ambiente local). O time não pode bloquear o início de T02 (que depende de testes contra Postgres real) por causa disso, e a janela de execução de hoje é curta (~5h até 19h).

Alternativas avaliadas:
- **Manter Testcontainers e suspender T02** até instalação do Docker: bloqueia o roadmap.
- **Cair para H2:** explicitamente proibido pelo master doc (a regra existe porque H2 diverge do Postgres em conversões de tipos, JSON, sintaxe de funções, comportamento de NULL em índices). Inaceitável.
- **Postgres nativo instalado no Windows:** funcionaria, mas exige instalação manual do Postgres + criação de DB de teste + sincronia entre devs. Cria fricção de onboarding e CI/local divergem.
- **Embedded Postgres da Zonky** (`io.zonky.test:embedded-postgres`): biblioteca Java pura que extrai o binário oficial do Postgres do classpath, executa como subprocesso e expõe o JDBC URL. **Postgres real, sem Docker.** Funciona em Windows, Mac, Linux. Suporta Spring Boot via `embedded-database-spring-test` com a anotação `@AutoConfigureEmbeddedDatabase`. Compatível com Flyway, Hibernate, JPA — comportamento idêntico ao Postgres "de verdade" porque é o mesmo binário.

## Decisão

**No MVP 1.0, testes de integração usam embedded-postgres da Zonky em vez de Testcontainers.** A regra invariante "Postgres real, nunca H2" é mantida. O que muda é o mecanismo de provisionamento: subprocesso Java em vez de container Docker.

Implementação:
- Dependências em `identity/pom.xml` (test scope): `io.zonky.test:embedded-postgres:2.0.7` e `io.zonky.test:embedded-database-spring-test:2.5.1`.
- `AbstractIntegrationTest` (em `src/test/java/com/nonnas/identity/testsupport/`) usa `@AutoConfigureEmbeddedDatabase(provider = ZONKY)` e estabelece o ciclo de vida do Postgres compartilhado entre testes via `DatabaseProvider.ZONKY`.
- Flyway roda contra o embedded Postgres exatamente como contra o Postgres real.

Migração futura para Testcontainers:
- Quando o PO instalar Docker Desktop (ou alternativas Docker-API-compatíveis: Podman Desktop, Rancher Desktop, WSL2 + Docker Engine), a migração é trivial: troca de dependência + troca da anotação base. Estimativa: 30 minutos.
- T10 (infra de testes consolidada) reavalia a escolha. Se Docker estiver disponível em todo o time + CI estiver com Testcontainers configurado, retornamos.

CI:
- GitHub Actions runners têm Docker disponível, então em CI poderíamos usar Testcontainers. Para evitar divergência local/CI no curto prazo, **CI também usa embedded-postgres** até T10.

## Consequências

**Positivas:**
- Desbloqueia T02 imediatamente sem instalar Docker.
- Mantém Postgres real (regra invariante respeitada).
- Onboarding de devs simplificado: clonou, rodou `./mvnw verify`, funciona.
- Tempo de boot do Postgres embedded ≈ 1–2s — comparável ao container reutilizado do Testcontainers.

**Negativas:**
- Primeira execução de cada dev/CI baixa o binário do Postgres (~25MB, cacheado em `~/.embedpostgresql`).
- Versão do Postgres definida pela versão da lib Zonky, não pela imagem Docker — menos controle granular.
- Recursos de Postgres opcionais (extensões além de `pgcrypto`/`uuid-ossp`) podem requerer custom binaries futuramente; trataremos quando aparecer.

**Mitigações:**
- Documentar em README que primeira `./mvnw verify` baixa ~25MB.
- ADR explícita garante que a decisão é revisada em T10.
- Versão do Postgres usada por Zonky 2.0.7 = Postgres 16.x, alinhada à imagem produtiva.

## Referências

- PROMPT_CLAUDE_CODE.md seção 0 (regra invariante "Postgres real, nunca H2"), seção 7.2 (testes de integração).
- ADR 0006 — Sequenciamento pós-adendo (T02 implementa auth, prereq de tudo que vem depois).
- Zonky embedded-postgres: https://github.com/zonkyio/embedded-postgres
- Spring integration: https://github.com/zonkyio/embedded-database-spring-test
