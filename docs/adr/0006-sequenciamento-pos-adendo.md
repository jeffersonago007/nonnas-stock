# ADR 0006 — Sequenciamento de execução pós-adendo (T16–T18)

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Integração do adendo de profissionalização (commit `ba2aa88`)

## Contexto

A v1.0 do master document (PROMPT_CLAUDE_CODE.md) definia 16 tarefas (T00–T15). O adendo entregue em 2026-05-08 acrescentou três tarefas (T16 hardening de segurança e LGPD, T17 observabilidade e notificações, T18 backup e DR) e três seções de spec (13. Segurança e LGPD, 14. Confiabilidade, 15. Observabilidade). Roadmap: 19 tarefas, ~840h, R$ 168k.

O adendo deixou quatro pontos abertos que o desenvolvimento sênior precisava decidir antes de prosseguir:

- **A.** Conflito de boundary v1.0.0 — T15 e T18 ambos mencionam "Tag v1.0.0".
- **B.** Sobreposição de entregáveis — T15 lista "audit completo de segurança", "Trivy scan", "backup automático", todos cobertos por T16/T18.
- **C.** Ordem de execução — T16 declara prereq T09+T10, então pode rodar antes do frontend (T12+).
- **D.** Custos de retrofit — T16 inclui refresh token rotation, brute force, política de senha. Implementar isso só em T16 retrabalha JWT e, por consequência, frontend (T12–T14) que já consome a API.

A pergunta ao Product Owner foi resolvida com "decida você baseado no maior nível de inteligência como se fosse um analista de desenvolvimento sênior especializados em sistemas ERP". Esta ADR registra as decisões.

## Decisão

**A. Boundary v1.0.0 fica em T18.**
- T15 produz `v1.0.0-rc.1` (release candidate). Este é o marco onde a UI está completa e E2E passa.
- T18 produz `v1.0.0` (release final). Este é o marco onde LGPD, audit log, backup off-site testado, observabilidade e simulação de DR estão prontos. **Antes disso, o sistema não está pronto para go-live.**
- Justificativa: ERP que toca dados pessoais de funcionários e clientes não é go-live sem LGPD compliance verificado; sem backup testado é loteria; sem audit log é auditoria impossível.

**B. T15 deduplicada quando executada.**
- Itens "audit completo de segurança", "Trivy scan", "backup automático configurado" e "performance < 2s no dashboard" saem de T15 — os três primeiros são redundantes com T16/T18, o quarto realoca para T17 (onde a métrica é instrumentada e validável).
- T15 final: suíte E2E Playwright completa, deployment guide para o cliente seguir, runbook operacional inicial, onboarding guide para devs novos. Tag `v1.0.0-rc.1` ao fim.

**C. Ordem de execução: original mantida.**
- T11 → T12 → T13 → T14 → T15 → T16 → T17 → T18.
- Justificativa econômica: cliente paga por entregáveis visíveis. UI funcionando convence; "2FA configurado" não convence. Intercalar T16 antes do frontend atrasa demo crítica para tracking comercial.
- Justificativa técnica: a decisão D (abaixo) elimina o retrabalho de auth básico, que era o principal motivo para considerar reordenação.

**D. Itens de auth básica antecipados de T16 para T02.**
- Move-se para T02 (identity): JWT com refresh token rotation + detecção de replay, BruteForceProtectionFilter com bloqueio progressivo, PoliticaSenhaValidator com Bean Validation. Custo adicional em T02: ~10h.
- Permanecem em T16: 2FA TOTP (com setup de QR code, backup codes), criptografia de campos sensíveis via JPA AttributeConverter, audit log estruturado com Hibernate Envers, endpoints LGPD, OWASP Java Encoder, OWASP Dependency Check, headers de segurança Spring Security, CSP no frontend.
- Justificativa: refresh rotation altera o `JwtAuthenticationFilter` e o contrato de `/auth/login`/`/auth/refresh`. Frontend (T12) consome esse contrato. Retrofitar em T16 = reescrever frontend de auth + atualizar todos os mocks de teste. Antecipar custa ~10h em T02; retrofit custaria ~20h em T16 + retrabalho frontend + risco de regressão. Trade-off claro.
- Os itens antecipados são ortogonais ao 2FA, audit log e criptografia — esses ficam em T16 onde a definição mais ampla está naturalmente posicionada.

## Consequências

**Positivas:**
- v1.0.0 final é signal honesto de "pronto para produção", não cosmético.
- T15 fica enxuta e focada (não duplica trabalho de T16/T18), reduzindo risco de "tarefa que parece pronta mas tem furos".
- Frontend (T12+) constrói contra API de auth final; sem retrabalho.
- Cliente vê demo (T14) cedo o suficiente para tracking comercial; hardening (T16) chega antes do go-live formal.

**Negativas:**
- T02 cresce em escopo (~10h adicionais). Risco gerenciável dado o tamanho da tarefa.
- A leitura linear do master doc (T16 inclui refresh rotation) divergirá da implementação (refresh rotation em T02). Mitigação: ADR 0003 documenta a divergência; checklist de T02 em STATUS.md cita esta ADR.

**Decisões adjacentes:**
- Quando T15 for executada, edição cosmética do master doc (movendo `v1.0.0` para `v1.0.0-rc.1` e removendo itens duplicados) deve ser proposta como PR à parte e revisada — conforme regra invariante "atualizações vêm via PR e revisão humana".

## Referências

- PROMPT_CLAUDE_CODE_ADENDO.md (entrega do adendo).
- ADR 0003 — JWT com refresh token rotation (implementação detalhada).
- PROMPT_CLAUDE_CODE.md seções 13–15 (novas spec) e T16–T18 (novas tarefas).
