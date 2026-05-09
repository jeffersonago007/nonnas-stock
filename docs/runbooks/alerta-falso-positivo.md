# Runbook — Alerta falso positivo

**Sintoma**: usuário reporta que recebeu alerta de RUPTURA / VENCIMENTO / ESTOQUE_MINIMO mas o saldo na UI não bate com a alegação.

**Detecção**: ticket de suporte, mensagem na operação, ou usuário marcando alerta como "resolvido manualmente" repetidamente sem ação.

## Diagnóstico — 5 minutos

### 1. Pegar o ID do alerta disparado

Via UI: clicar no alerta → URL contém `?disparadoId=<uuid>`.

Via SQL:

```sql
SELECT id, tipo, insumo_id, filial_id, saldo_no_disparo, data_disparo, status
  FROM alerts.alerta_disparado
 WHERE filial_id = '<filial-suspeita>'
 ORDER BY data_disparo DESC
 LIMIT 5;
```

### 2. Comparar saldo no disparo vs saldo atual

```sql
-- Saldo atual
SELECT i.codigo, i.nome,
       sum(s.quantidade_base) AS saldo_atual
  FROM inventory_core.saldo_lote s
  JOIN catalog.insumo i ON i.id = s.insumo_id
 WHERE s.insumo_id = '<insumo-id>'
   AND s.filial_id = '<filial-id>'
 GROUP BY i.codigo, i.nome;
```

Comparar com `saldo_no_disparo` do passo 1.

| Saldo no disparo | Saldo agora | Diagnóstico                                     |
|------------------|-------------|-------------------------------------------------|
| 0 / negativo     | > 0         | Alerta era válido na hora; entrada após resolveu |
| > 0 (mas baixo)  | > 0 (igual) | Bug no `AvaliadorAlertasService` — investigar    |
| > 0 grande       | > 0 grande  | Threshold mal configurado; revisar config        |

### 3. Conferir a config do alerta

```sql
SELECT *
  FROM alerts.alerta_configuracao
 WHERE id = (SELECT config_id FROM alerts.alerta_disparado WHERE id = '<disparado-id>');
```

- `tipo`: bate com o esperado?
- `threshold`: faz sentido (ex.: ESTOQUE_MINIMO_ABSOLUTO com threshold=10 dispara quando saldo cai abaixo de 10).
- `escopo` (insumo_id + filial_id): match com o esperado?
- `prioridade`: ALTA/CRITICA pra coisa que deveria ser INFO inflaciona percepção.

## Diagnósticos comuns

### A) Threshold inflado errado

**Cenário**: config criada com threshold percentual (ex.: 80%) mas o operador queria absoluto. Sistema dispara alertas demais porque qualquer saldo abaixo de 80% do estoque máximo conta.

**Solução**:
1. Ajustar config:
   ```sql
   UPDATE alerts.alerta_configuracao
      SET tipo = 'ESTOQUE_MINIMO_ABSOLUTO',
          threshold = 10
    WHERE id = '<config-id>';
   ```
2. Auto-resolver os falsos positivos abertos:
   ```sql
   UPDATE alerts.alerta_disparado
      SET status = 'RESOLVIDO_AUTO',
          data_resolucao = NOW()
    WHERE config_id = '<config-id>'
      AND status = 'ATIVO';
   ```
3. Audita:
   ```sql
   INSERT INTO identity.audit_log (event_type, actor_id, target_kind, metadata)
   VALUES ('MIGRACAO_MANUAL', '<id>', 'ALERTA_CONFIGURACAO',
           '{"acao":"reconfigurar_threshold","de":"80%","para":"10 absoluto"}');
   ```

### B) Saldo materializado stale

**Cenário**: movimentação foi registrada mas o saldo materializado (`saldo_lote` ou MVs do reporting) não atualizou. AvaliadorAlertasService usa o saldo stale e dispara alerta de algo que já foi resolvido.

**Diagnóstico**: comparar a soma direta de movimentações vs `saldo_lote`:

```sql
SELECT sum(CASE WHEN m.tipo LIKE 'ENTRADA_%' THEN it.quantidade_base
                ELSE -it.quantidade_base END) AS saldo_calculado
  FROM operations.movimentacao m
  JOIN operations.item_movimentacao it ON it.movimentacao_id = m.id
 WHERE it.insumo_id = '<insumo-id>'
   AND m.filial_id = '<filial-id>';

-- Comparar com saldo_lote agregado (da query do passo 2).
```

Se diverge, há bug no event listener (`MovimentacaoAlertaListener` ou similar) que não atualizou o saldo materializado.

**Solução**:
1. Issue urgente no backlog (esse bug é grave).
2. Workaround imediato: refresh manual da MV:
   ```bash
   curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
       https://app.nonnas.com.br/api/v1/relatorios/refresh
   ```

### C) Auto-resolução que não dispara

**Cenário**: saldo subiu acima do threshold mas o alerta continua ATIVO.

**Diagnóstico**: o `AvaliadorAlertasService` só re-avalia o (insumo, filial) quando uma nova movimentação acontece (via `MovimentacaoCriadaEvent`). Se o saldo subiu via meio externo (importação manual, restore), o avaliador não é acionado.

**Solução**:
1. Forçar reavaliação criando uma movimentação dummy de ajuste 0:
   - Não recomendado — bagunça auditoria.
2. **Workaround manual**:
   ```sql
   UPDATE alerts.alerta_disparado
      SET status = 'RESOLVIDO_AUTO',
          data_resolucao = NOW()
    WHERE id = '<disparado-id>';
   ```
3. Backlog: criar endpoint POST `/api/v1/alertas/reavaliar` para acionar manualmente.

### D) Notificação duplicada

**Cenário**: usuário recebe a mesma notificação várias vezes para o mesmo alerta.

**Diagnóstico**: o `AlertaDisparadoListener` em identity (T17) escuta o evento e cria notificação. Se o evento é publicado mais de uma vez (transação re-tentativa, listener re-disparado), há duplicação.

**Solução**:
1. Verificar quantas notificações existem para o mesmo `disparadoId`:
   ```sql
   SELECT count(*) FROM identity.notificacoes_usuario
    WHERE metadata::jsonb->>'disparadoId' = '<disparado-id>';
   ```
2. Se > 1 por usuário, há bug. Backlog para implementar idempotência por (usuario_id, metadata->disparadoId).
3. **Workaround**: arquivar duplicatas:
   ```sql
   UPDATE identity.notificacoes_usuario
      SET arquivada_em = NOW()
    WHERE metadata::jsonb->>'disparadoId' = '<disparado-id>'
      AND id NOT IN (
          SELECT MIN(id) FROM identity.notificacoes_usuario
           WHERE metadata::jsonb->>'disparadoId' = '<disparado-id>'
           GROUP BY usuario_id);
   ```

## Pós-incidente

- Atualizar este runbook com novo padrão de falso positivo se for novo.
- Backlog: investigar causa raiz se foi bug.
- Comunicar a operação (pode ter sido a Maria reclamando que o sistema "tá louco" — vale dar feedback que entendemos e agimos).
