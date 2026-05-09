# LGPD — Conformidade do Nonnas Stock

Master doc 13.5. Este documento é fonte canônica do que o sistema **faz hoje** para atender aos titulares e à autoridade. Para ROPA (Registro de Operações), ver [`lgpd-ropa.md`](lgpd-ropa.md). Para mapeamento por tabela, ver [`lgpd-mapping.md`](lgpd-mapping.md).

## 1. Bases legais

| Tratamento                              | Base legal (Art. 7º LGPD)                  |
|-----------------------------------------|--------------------------------------------|
| Cadastro de usuário (operador/admin)    | Inciso V — execução de contrato (CLT)      |
| Login + auditoria de acesso             | Inciso II — cumprimento de obrigação legal (controle de acesso CLT/eSocial) e Inciso IX — interesse legítimo (segurança) |
| Histórico de senhas + brute force       | Inciso IX — interesse legítimo (segurança da informação) |
| 2FA TOTP                                | Inciso IX — interesse legítimo             |
| Trilha de movimentações                 | Inciso II — obrigação fiscal (Receita Federal) |

## 2. Direitos do titular implementados

Todos atendidos via `/api/v1/lgpd/*` (controller `LgpdController`):

| Direito (LGPD Art. 18)              | Endpoint                       | Status               |
|-------------------------------------|--------------------------------|----------------------|
| II — confirmação de tratamento      | `GET /lgpd/meus-dados`         | ✅ Implementado      |
| III — correção de dados             | `POST /lgpd/correcao`          | ✅ Implementado      |
| IV — portabilidade                  | `GET /lgpd/meus-dados`         | ✅ JSON exportável   |
| VI — eliminação                     | `DELETE /lgpd/exclusao`        | ✅ Anonimização imediata |
| VII — informação sobre compartilhamento | (este doc + ROPA)         | ✅ Documental        |
| VIII — informação sobre não-consentimento | (UI + termos)             | 🔶 Termos versionados em `aceites_termos`, UI ainda não bloqueia funções por dissentimento |
| IX — revogação do consentimento     | `DELETE /lgpd/exclusao`        | ✅ Cobertura via exclusão |

## 3. Anonimização vs. exclusão

`DELETE /lgpd/exclusao` faz **anonimização**, não exclusão física, porque:

- Movimentações de estoque, transferências e ajustes têm `usuario_id` em FK; remover quebra histórico fiscal/auditoria.
- Receita Federal exige guarda de movimentações por **5 anos** (Art. 195 CTN).

Anonimização aplicada:

```
nome  = "Usuário Anonimizado"
email = "anonimizado-<id-curto>@nonnas.local"   (não reaproveitável)
ativo = false                                    (impede login)
```

Após 5 anos, executar exclusão física via SQL ad-hoc + entrada no `audit_log` (TODO T18).

## 4. Retenção

| Dado                                | Retenção                          | Razão                                |
|-------------------------------------|-----------------------------------|--------------------------------------|
| `usuarios` (após anonimização)      | 5 anos                            | Vínculo com movimentações fiscais     |
| `historico_senhas`                  | 12 meses                          | Apenas as últimas 5 hash, sliding window |
| `refresh_tokens`                    | TTL natural (30 dias) + 90 dias post-revogação | Forense de incidente              |
| `tokens_revogados`                  | TTL do JWT (default 60min)        | Propósito do blacklist                |
| `audit_log`                         | 18 meses                          | Auditoria, suficiente para LGPD       |
| `aceites_termos`                    | Vida do contrato + 5 anos         | Prova de consentimento                |

Job mensal de purga: TODO T17 (`@Scheduled` que limpa registros expirados).

## 5. Compartilhamento com terceiros

Hoje: **nenhum**. Sistema é monolito on-prem. Nenhum dado pessoal sai do servidor da empresa.

Quando integrar com canais de venda (T17+) ou Sentry/Grafana, atualizar este documento e o ROPA com o operador (3rd party) e a base legal.

## 6. Operador e DPO

- **Controlador**: Nonnas Paola — Pizzaria SA (preencher CNPJ).
- **Operador**: a própria empresa (sistema on-prem).
- **DPO**: <preencher nome + email>.
- **Canal do titular**: <preencher email/telefone>.

## 7. Incidente

Se vazamento for confirmado:
1. Informar DPO em < 24h.
2. Postmortem técnico em < 72h.
3. Notificação à ANPD em < 72h se houver risco aos titulares (Art. 48).
4. Comunicação aos titulares se aplicável.

## 8. Audit trail

Todo evento LGPD vai para `audit_log`:

```sql
SELECT occurred_at, event_type, actor_email, metadata
  FROM audit_log
 WHERE event_type IN ('LGPD_DADOS_SOLICITADOS','LGPD_CORRECAO','LGPD_EXCLUSAO')
 ORDER BY occurred_at DESC LIMIT 100;
```

## 9. Assinatura

Documento revisado e aprovado por:

- _Jefferson Agostinho_ (BA/QA — controlador)
- _Ewerton Carreira_ (revisor sênior)

Data: 2026-05-09. Próxima revisão: 2026-11-09 (semestral).
