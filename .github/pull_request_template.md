## Objetivo

<!-- O que essa PR resolve? Referencie a tarefa: T00, T01, etc. -->

## Mudanças

<!-- Lista breve das mudanças principais (módulos afetados, novos endpoints, migrations). -->

-
-

## Testes adicionados

<!-- Que testes cobrem essa mudança? Unitários, integração, E2E? -->

- [ ] Unitários
- [ ] Integração (Postgres embarcado via Zonky — ADR 0007)
- [ ] ArchUnit (se aplicável)
- [ ] E2E (se aplicável)

## Como validar

<!-- Passo a passo para o revisor reproduzir e validar localmente. -->

1.
2.
3.

## Checklist de qualidade

- [ ] `mvn verify` verde local
- [ ] Cobertura mínima respeitada (85% domain, 75% application)
- [ ] Commits no padrão Conventional (`feat(modulo): ...`)
- [ ] `STATUS.md` atualizado se for entrega de tarefa
- [ ] Documentação atualizada (se aplicável)
- [ ] Sem `// TODO` ou `// FIXME` sem ticket associado

## Checklist de produção (T18 / master doc 14.6)

Marcar conforme aplicável. Itens "N/A" são aceitáveis com justificativa em comentário.

### Banco / dados

- [ ] Migration Flyway nova, sequencial (sem renomear/editar antiga já aplicada)
- [ ] Migration não destrutiva, OU plano de rollback documentado
- [ ] Índices novos com `CONCURRENTLY` em prod se tabela > 1k linhas
- [ ] Sem query nova em hot path com `Seq Scan` em tabela grande (validei via EXPLAIN)
- [ ] Sem dado pessoal novo sem `CamposSensiveisConverter` (LGPD — ver `lgpd-mapping.md`)

### Segurança

- [ ] Endpoint novo respeita `@PreAuthorize` apropriado (ADMIN/GERENTE/OPERADOR)
- [ ] Input validado com Bean Validation no DTO
- [ ] Sem string literal de query SQL com concatenação de input do usuário
- [ ] Sem secret/token/senha em log ou em response payload
- [ ] Headers de segurança não foram afrouxados (HSTS, CSP, X-Frame-Options)

### Observabilidade

- [ ] Erros novos têm `BusinessRuleException` com `ErrorCode` adequado
- [ ] Métricas custom em endpoints com SLO (TODO se não há ainda — backlog T17+)
- [ ] Logs estruturados (kv args via StructuredArguments) em mudanças de estado
- [ ] `audit_log` registrado em operações sensíveis (mudança de perfil, exclusão LGPD)

### Operação

- [ ] Sem feature flag nova sem entrada na lista do `runbooks/feature-flag-rollback.md`
- [ ] Documentação de troubleshooting atualizada (`runbooks/`) se introduziu novo modo de falha
- [ ] Variáveis de ambiente novas documentadas em `docs/secrets-management.md` ou `docs/deployment.md`

### Performance / risco

- [ ] Endpoint novo testado com volume realista (ou estimativa documentada)
- [ ] Sem chamada N+1 introduzida em listagem
- [ ] Cache invalidation considerada se afeta MVs do reporting

## Risco e rollout

<!-- Marque um. -->

- [ ] **Baixo** — refactor interno, sem mudança de contrato externo, fácil rollback.
- [ ] **Médio** — afeta endpoint público OU schema do banco; rollback exige nova migration ou redeploy.
- [ ] **Alto** — mudança em fluxo crítico (login, movimentação, transferência); revisar com Ewerton antes de mergear.

## Notas para o revisor

<!-- Algo específico que precise atenção? Trade-off não-óbvio? Plan B se não der? -->
