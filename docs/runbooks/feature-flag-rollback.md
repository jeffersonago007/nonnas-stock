# Runbook — Feature flag rollback

**Quando usar**: comportamento novo em produção está causando incidente. Mais rápido desligar via flag do que fazer rollback do deploy.

**Pré-requisito**: a feature precisa estar gated por flag (ver `FeatureFlagService` em identity). Hoje só `LGPD_EXCLUSAO_ATIVADA` está nesse padrão (POC T18).

## Listar flags atuais

```sql
SELECT chave, descricao, habilitada, rollout_pct, atualizada_em
  FROM identity.feature_flags
 ORDER BY chave;
```

## Desligar uma flag (kill-switch)

Ativação manual via SQL é o caminho mais rápido — UI de gerenciamento ainda não existe (gap pós-T18).

### 1. Confirmar que sabe o que está fazendo

- Qual feature exatamente vai cair?
- Quais usuários ficam sem acesso?
- O que retorna no lugar — 503? 403? Tela em branco?

Para `LGPD_EXCLUSAO_ATIVADA`:
- Endpoint `DELETE /api/v1/lgpd/exclusao` retorna 409 Conflict com mensagem "Exclusão LGPD temporariamente indisponível...".
- Outros endpoints LGPD (`/meus-dados`, `/correcao`) seguem funcionando.

### 2. Desligar

```sql
BEGIN;

UPDATE identity.feature_flags
   SET habilitada = false,
       atualizada_em = NOW()
 WHERE chave = '<chave-da-flag>';

-- Auditar
INSERT INTO identity.audit_log
    (event_type, actor_id, actor_email, target_kind, metadata)
VALUES
    ('FEATURE_FLAG_DESLIGADA', '<seu-id>', '<seu-email>', 'FEATURE_FLAG',
     '{"flag":"<chave>","motivo":"<incident-link-ou-descricao>"}');

COMMIT;
```

### 3. Validar

Próxima request que bate na feature deve retornar o erro esperado. Latência de propagação: imediata (cada chamada lê do banco — sem cache).

```bash
# Para LGPD_EXCLUSAO_ATIVADA, testa que está bloqueando:
curl -X DELETE http://localhost:8080/api/v1/lgpd/exclusao \
    -H "Authorization: Bearer $TOKEN" -i
# Esperado: 409 Conflict com mensagem amigável.
```

### 4. Comunicar

- Status interno (slack/email): "Feature X foi desligada às H:M devido a Y. Tempo estimado para volta: Z."
- Se afeta usuários externos: notificação interna para todos os ATIVOS via SQL:
  ```sql
  INSERT INTO identity.notificacoes_usuario
      (usuario_id, tipo, prioridade, titulo, mensagem, link_acao, canais_destino)
  SELECT id, 'AVISO_OPERACIONAL', 'AVISO',
         'Funcionalidade temporariamente indisponível',
         'A função X está fora do ar para manutenção. Tempo estimado: até Hh.',
         null, 'INTERNO'
    FROM identity.usuarios
   WHERE ativo = true;
  ```

## Religar a flag

Quando o incidente foi resolvido (deploy de correção, mitigação, etc):

```sql
BEGIN;

UPDATE identity.feature_flags
   SET habilitada = true,
       atualizada_em = NOW()
 WHERE chave = '<chave>';

INSERT INTO identity.audit_log (event_type, actor_id, target_kind, metadata)
VALUES ('FEATURE_FLAG_LIGADA', '<id>', 'FEATURE_FLAG',
        '{"flag":"<chave>","duracao_off":"<Xh Ym>"}');

COMMIT;
```

## Rollout progressivo (futuro)

Hoje a flag é binária (`habilitada=true/false`). Mas a coluna `rollout_pct` existe para gating progressivo:

```sql
-- Liga apenas para 10% das chamadas
UPDATE identity.feature_flags
   SET habilitada = true,
       rollout_pct = 10
 WHERE chave = '<chave>';
```

Hoje o `FeatureFlagService` decide aleatoriamente — chamadas sucessivas do mesmo usuário podem ver respostas inconsistentes. Para sticky-rollout (mesmo usuário sempre vê o mesmo resultado), futura evolução vai usar `hash(usuarioId) % 100 < rollout_pct`.

## Criar uma nova flag

Hoje via migration Flyway. Não dá para criar via UI/SQL ad-hoc (poderia, mas a inserção via migration cria o histórico de quando a flag entrou).

```sql
-- nova migration V0XX__add_feature_<chave>.sql
INSERT INTO feature_flags (chave, descricao, habilitada, rollout_pct)
VALUES ('<chave-em-kebab-case>',
        'Descrição clara: o que essa flag controla, quando ligar, quando desligar.',
        FALSE,
        0);
```

E no código, o checkpoint:

```java
if (!featureFlagService.isAtiva("<chave-em-kebab-case>")) {
    throw new BusinessRuleException(ErrorCode.CONFLICT,
        "Feature temporariamente indisponível");
}
```

## Pós-incidente

- Postmortem em `docs/post-mortems/`.
- Avaliar se a flag deve permanecer permanente (kill-switch operacional) ou ser removida quando o código maduro.
- Atualizar este runbook se cenário novo apareceu.

## Lista corrente de flags

| Chave                   | Descrição                                                                      | Default |
|-------------------------|--------------------------------------------------------------------------------|---------|
| `lgpd-exclusao-ativada` | Permite POST /api/v1/lgpd/exclusao executar anonimização. Off → 409 Conflict.  | ON      |
