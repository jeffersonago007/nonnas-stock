# LGPD — Mapeamento de dados pessoais

Cada coluna que pode armazenar dado pessoal está listada aqui com classificação, criptografia (se aplica), e retenção. Atualize sempre que adicionar coluna nova ou tabela com PII.

| Tabela.coluna                         | Classificação        | Tratamento at-rest          | Retenção             | Acesso                        |
|---------------------------------------|----------------------|-----------------------------|----------------------|-------------------------------|
| `usuarios.nome`                       | Pessoal (identificação) | Texto plano               | 5 anos pós-anonimização | App (BCrypt-hash em senha)    |
| `usuarios.email`                      | Pessoal (identificação) | Texto plano (constraint UNIQUE) | 5 anos pós-anonimização | App                           |
| `usuarios.senha_hash`                 | Sensível              | BCrypt cost 12              | TTL conta             | App (verificação only)        |
| `usuarios.tentativas_falhas`          | Operacional           | Texto plano                 | TTL conta             | App                           |
| `usuarios.bloqueado_ate`              | Operacional           | Texto plano                 | TTL conta             | App                           |
| `historico_senhas.senha_hash`         | Sensível              | BCrypt cost 12              | 12 meses (sliding 5)  | App (anti-reuso)              |
| `refresh_tokens.jti`                  | Operacional           | Texto plano                 | TTL token + 90d       | App                           |
| `usuarios_2fa.secret_cifrado`         | Sensível              | **AES-256-GCM** (CryptoService) | TTL conta         | App (decrypt em verifyCode)   |
| `usuarios_2fa.backup_codes_hash`      | Sensível              | SHA-256                     | TTL conta             | App (one-shot use)            |
| `audit_log.actor_email`               | Pessoal               | Texto plano                 | 18 meses              | App + auditoria DPO           |
| `audit_log.request_ip`                | Pessoal (Marco Civil) | Texto plano                 | 18 meses              | App + auditoria DPO           |
| `audit_log.metadata`                  | Variável              | Texto plano (revisar antes) | 18 meses              | App + auditoria DPO           |
| `aceites_termos.request_ip`           | Pessoal               | Texto plano                 | Vida do contrato + 5 anos | App + auditoria              |
| `tokens_revogados.usuario_id`         | Pseudonimizado (UUID) | Texto plano                 | TTL token             | App                           |
| `empresas.cnpj`, `empresas.razao_social`, `filiais.cnpj` | Empresarial — fora do escopo PII pessoa física | Texto plano | Vida do contrato | App |

## CNPJ-PF (microempreendedor pessoa física)

Hoje o sistema **não armazena CNPJ-PF**. Empresa e filial usam o CNPJ corporativo da pessoa jurídica. Se um cadastro futuro suportar PF (LTDA-única, MEI), aplicar `@Convert(converter = CamposSensiveisConverter.class)` na coluna correspondente.

## CPF / RG / Telefone pessoal / Endereço residencial

**Não armazenados** no MVP. Se forem adicionados (ex.: cadastro de cliente para programa de fidelidade), seguir o checklist:

1. Adicionar coluna com `columnDefinition = "text"` (cifra cresce — 30 chars vira ~100).
2. `@Convert(converter = CamposSensiveisConverter.class)` no campo.
3. Atualizar este documento com classificação e retenção.
4. Atualizar `lgpd-ropa.md` se mudar a base legal ou os atores.
5. Adicionar masking no Sentry / logs (T17).

## Verificação periódica

Trimestralmente, executar e comparar:

```sql
-- Colunas que parecem PII por nome (ad-hoc)
SELECT table_schema, table_name, column_name
  FROM information_schema.columns
 WHERE column_name ~* '(cpf|rg|telefone|endereco|email|nome|cnpj_pf)'
   AND table_schema NOT IN ('pg_catalog', 'information_schema');
```

Toda nova entrada precisa virar uma linha aqui. Auditoria por amostragem deve provar que `secret_cifrado` está ilegível em `psql` direto.

```sql
-- Smoke check (deve aparecer base64 cifrado, não secret legível):
SELECT secret_cifrado FROM usuarios_2fa LIMIT 1;
```
