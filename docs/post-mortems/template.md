# Postmortem — [Título do incidente]

**Data**: YYYY-MM-DD
**Duração**: ~Xh (HH:MM → HH:MM)
**Severidade**: P1 | P2 | P3 (P1 = produção fora; P2 = funcionalidade crítica afetada; P3 = degradação parcial)
**Autor**: <nome>
**Aprovado por**: <Jeff/Ewerton>

## Resumo (2-3 frases)

O que aconteceu, quem foi afetado, qual foi o impacto observável.

## Linha do tempo

Hora em UTC (ou BRT, mas seja consistente). Eventos importantes — primeiro alerta, primeira ação, mitigação aplicada, validação de recuperação, comunicação.

| Hora UTC | Evento                                                  |
|----------|---------------------------------------------------------|
| 13:42    | Sentry alerta: spike de 500 em `/api/v1/movimentacoes`  |
| 13:45    | <pessoa> abre incidente; começa investigação            |
| 13:51    | Causa identificada: connection pool exausto             |
| 14:03    | Pool aumentado de 10 para 30 + restart                  |
| 14:05    | Latência volta ao normal                                |
| 14:30    | Comunicação aos usuários encerrada                      |

## Impacto

- Quantos usuários afetados (estimar via logs).
- Tempo total de degradação.
- Operações perdidas (movimentações que falharam, alertas perdidos, etc).
- Custo financeiro estimado se mensurável.
- Custo reputacional — alguém crítico (cliente externo) viu?

## O que aconteceu (cronologia técnica)

Detalhes técnicos: stack trace, query culpada, decisão arquitetural ruim que apareceu agora.

```
<exemplo de stack trace ou snippet relevante>
```

## Causa raiz

Não confunda com causa imediata. Causa raiz é o "por quê?" repetido 5 vezes.

- **Causa imediata**: connection pool exausto.
- **Por quê 1?** Tinha mais requisições simultâneas do que conexões.
- **Por quê 2?** Endpoint X demorou muito por causa de query lenta.
- **Por quê 3?** Faltava índice em `tabela_y(coluna_z)`.
- **Por quê 4?** Esse índice nunca foi criado porque a tabela cresceu além do esperado quando a feature W foi adicionada.
- **Por quê 5?** A revisão da PR de W não pegou que precisava de índice.

Causa raiz: **falta de checklist de revisão para mudanças que alteram padrões de query**.

## O que funcionou

O que ajudou a detectar/mitigar rápido. Reforçar isso.

- Sentry pegou o spike antes do usuário reclamar.
- Runbook `postgres-lento.md` apontou os comandos certos sem precisar improvisar.
- Pool size foi parametrizado em config — mudança de número não exigiu deploy.

## O que não funcionou

Onde foi devagar ou furou.

- Demoramos 6 minutos para abrir o incidente — alerta foi ignorado inicialmente como "ruído".
- Não tinha permissão de SSH no servidor de banco — Ewerton precisou intervir.
- O dashboard `Banco` não mostrava connection pool — adicionado durante o incidente.

## Action items

Concretos, com responsável e data. Sem "vamos pensar em melhorar X" — é "criar issue Y, dono Z, deadline W".

| Ação                                                          | Owner   | Prazo       | Status      |
|---------------------------------------------------------------|---------|-------------|-------------|
| Adicionar índice `idx_tabela_y_coluna_z`                      | Jeff    | 2026-05-12  | Pendente    |
| Atualizar PR template com checklist de query patterns          | Ewerton | 2026-05-15  | Pendente    |
| Provisionar SSH compartilhado para o banco                     | Ops     | 2026-05-20  | Pendente    |
| Aumentar `maximum-pool-size` permanentemente em prod (10→25)   | Jeff    | feito       | ✅ no PR #X  |

## Aprendizados

Reflexões livres — o que mudou na nossa cabeça depois desse incidente.

- ...

## Comunicação aos stakeholders

- Status interno: <link slack ou email>
- Comunicação externa: <link>
- ANPD (se aplica): <data e protocolo>
