# Simulação DR — Pré-go-live (template a ser executado)

> **Status**: agendado, **NÃO** executado ainda. Master doc T18 pede simulação real de 2-3h com Jeff e Ewerton antes do go-live `v1.0.0`. Este documento é o roteiro + esqueleto que será preenchido durante o drill.

## Pré-drill

- **Quando**: a definir (sugestão: 1-2 semanas antes do go-live, idealmente fim de semana ou madrugada).
- **Quem**: Jefferson (controlador do incidente), Ewerton (executor técnico). Idealmente Luiza ou outro como observador silencioso.
- **Onde**: ambiente de staging (não prod). Banco com volume sintético (50 filiais, 5k insumos, 10k movimentações simuladas).
- **Duração**: 2-3h reservadas. Cronometrar do "primeiro alerta simulado" até "sistema validado em staging".

## Cenário escolhido

A definir entre os 5 do `disaster-recovery.md`. Sugestão para o primeiro drill: **Cenário 2 — banco corrompido**, porque exercita backup + restore + validação manual + comunicação. Cobre o pipeline T18 inteiro.

## Roteiro (preencher no dia)

### 1. Setup (30 min antes)

- [ ] Confirmar que staging está com dados representativos.
- [ ] Confirmar último backup da prod simulada existe e está cifrado.
- [ ] Confirmar chave privada GPG no laptop do executor.
- [ ] Iniciar timer.

### 2. Injeção do desastre

A definir como introduzir corrupção controlada. Ex.:

```sql
-- Em staging, simular DELETE acidental
DELETE FROM operations.movimentacao WHERE filial_id = '<staging-filial-uuid>';
```

Anotar timestamp do "antes" e "depois" pra medir RPO.

### 3. Detecção

- [ ] Quanto tempo até alguém perceber o problema? (Imagina-se que um alerta operacional ou ticket de usuário gere a detecção — em staging, simular via ticket mock.)

### 4. Resposta

Seguir `runbooks/restore-backup.md`.

- [ ] Stop da app: `docker compose stop app`.
- [ ] Backup do estado atual (mesmo corrompido) — cobrir caso queiramos forense.
- [ ] Importar chave GPG privada na máquina de trabalho.
- [ ] Identificar último backup bom (último anterior ao DELETE simulado).
- [ ] Restore em banco temporário (`nonnas_recovery_check`).
- [ ] Smoke checks no banco temporário — counts batem com o esperado?
- [ ] Decisão: total ou parcial?
- [ ] Restore em staging "produção" simulada.
- [ ] Subir app.
- [ ] Smoke via UI: login + dashboard + listar movimentações.

### 5. Comunicação

- [ ] Status interno (Slack thread ou similar).
- [ ] Notificação aos usuários afetados — em staging, ID dummy.
- [ ] Atualização final quando recuperação terminou.

### 6. Validação final

- [ ] App online + responsável atestou OK.
- [ ] Latência de queries normalizada.
- [ ] Logs limpos por 5 minutos seguidos sem erros novos.

### 7. Encerramento

- [ ] Parar timer. Anotar tempo total.
- [ ] Comparar com RTO alvo (4h). Resultado: <preencher>.
- [ ] Voltar staging para estado original (truncar tabela ou restore-restore).

## A preencher após o drill

### Tempo total

- Detecção: \_\_ min (alvo: < 15 min)
- Resposta + restore: \_\_ min
- Validação: \_\_ min
- **Total**: \_\_ min (alvo: < 240 min = 4h)

### O que funcionou

- ...

### O que não funcionou

- ...

### Action items

| Ação                                                | Owner   | Prazo       |
|-----------------------------------------------------|---------|-------------|
| ...                                                 | ...     | ...         |

### Aprendizados

- ...

## Próximo drill

Master doc pede semestral. Próxima data sugerida: \_\_\_\_ (6 meses após este).
