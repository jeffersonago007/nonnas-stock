# Adendo técnico — Lote opcional via flag `controla_validade`

> **Aplicável a:** sistema Nonnas Stock, MVP 1.x
> **Substitui parcialmente:** decisão original "saldo sempre por `(lote, filial)`" descrita em `PROMPT_CLAUDE_CODE.md` §1.3 (princípio 3) e tarefa T04
> **Não substitui:** nenhuma outra decisão arquitetural; estrutura modular, eventos e demais princípios continuam vigentes

---

## 1. Contexto

A descoberta veio com dado real de NF-e. A legislação brasileira só obriga preenchimento do grupo `rastro` (lote/validade) em famílias de produto específicas — medicamentos, alguns alimentos sob vigilância sanitária, produtos sujeitos a recall. Para a maior parte dos insumos comprados por um restaurante (sal, óleo, embalagens, descartáveis, refrigerantes em lata, condimentos), o XML simplesmente não traz o grupo `rastro`, e o importador trava ao tentar criar um `Lote` com `data_validade` obrigatória.

Isso expôs uma decisão arquitetural prematura: a granularidade `(lote, filial)` foi tratada como invariante do modelo, quando na verdade ela só faz sentido para insumos que efetivamente exigem rastreabilidade e FEFO. Para os demais, ela introduz fricção e dados sintéticos sem valor de domínio.

## 2. Decisão de design

Introduzir o flag `controla_validade` no cadastro de `Insumo`. Esse flag determina o regime de saldo daquele insumo em todo o sistema:

- `controla_validade = true` (perecíveis): comportamento atual preservado. Cada entrada gera um lote rastreado com validade obrigatória. Saldo materializado por `(insumo, filial, lote)`. Saída por FEFO. Alertas de vencimento próximo aplicam.
- `controla_validade = false` (não perecíveis): existe um único lote agregador por `(insumo, filial)`, criado preguiçosamente na primeira entrada. Toda entrada empilha saldo nele, `data_validade` fica `NULL`. Saída consome direto desse saldo agregado. Alertas de vencimento não aplicam; estoque mínimo e ruptura continuam aplicando.

**Invariantes preservadas:**

- `Movimentacao` e `ItemMovimentacao` mantêm referência obrigatória a `lote_id` — modelo de dados não bifurca.
- `SaldoLote` continua sendo a tabela materializada de saldo, agora com `lote_id` apontando ou para lote rastreado ou para lote agregador.
- Conciliação `saldo materializado vs soma de movimentações` continua funcionando para ambos os regimes.

**O que muda na superfície:**

- Cadastro de Insumo ganha o flag.
- Lote ganha campo `tipo` (`RASTREADO` ou `AGREGADOR`) e `data_validade` passa a ser nullable.
- Serviço de seleção de lotes para saída vira polimórfico: FEFO para insumos com validade, baixa direta do agregador para os demais.
- Importador NF-e deixa de exigir grupo `rastro`; quando ausente, decide por tipo de insumo.
- UI esconde o conceito de lote quando o insumo não controla validade.

## 3. Lista de tarefas

A numeração segue convenção `T-LOT-NN` para integrar com o STATUS.md sem renumerar tarefas existentes. Marcar todas como dependentes do estado atual do projeto. Antes de executar, rodar `mvn verify` baseline e anotar tempo e cobertura para comparar depois.

### T-LOT-01 — Migration de schema

**Objetivo:** ajustar tabelas `insumo` e `lote` para suportar o regime opcional, preservando todos os dados existentes.

**Pré-requisitos:** T04 concluída (schema base de inventário existe).

**Entregáveis:**

- Migration Flyway `V0XX__lote_opcional.sql` em `inventory-core/src/main/resources/db/migration/` (substituir XX pelo próximo número disponível).
- Conteúdo conforme §4 deste adendo.
- Migration idempotente em ambiente de desenvolvimento (rodar duas vezes não deve falhar — usar `IF NOT EXISTS` onde aplicável).

**Critérios de aceitação:**

- `mvn -pl inventory-core flyway:migrate` aplica sem erro.
- Todos os lotes existentes ficam com `tipo = 'RASTREADO'`.
- Todos os insumos existentes ficam com `controla_validade = FALSE`.
- Constraint `lote_data_validade_obrigatoria_quando_rastreado` valida: `CHECK (tipo = 'AGREGADOR' OR data_validade IS NOT NULL)`.
- Constraint `lote_agregador_unico_por_insumo_filial` valida: índice único parcial em `(insumo_id, filial_id) WHERE tipo = 'AGREGADOR'`.

### T-LOT-02 — Entidade Insumo e DTO

**Objetivo:** refletir o flag no domínio.

**Pré-requisitos:** T-LOT-01.

**Entregáveis:**

- Campo `controlaValidade: boolean` em `Insumo` (entidade JPA + record/DTO de criação e atualização).
- Default `false` no construtor e no DTO.
- Validação no use case `AtualizarInsumoUseCase`: não permitir desativar `controla_validade` se houver lotes rastreados com saldo > 0 (lançar `InsumoComLotesRastreadosException`). Permitir ativar livremente, mas avisar que entradas anteriores ficam no lote agregador.
- Endpoint `PATCH /api/v1/insumos/{id}/controle-validade` para alterar exclusivamente esse campo (separado do update genérico, porque a alteração tem efeitos colaterais e merece auditoria própria).
- Testes unitários cobrindo:
  - Criação com `controla_validade = true` e `false`.
  - Transição `false → true` permitida.
  - Transição `true → false` com saldo rastreado bloqueada.
  - Transição `true → false` sem saldo rastreado permitida.

**Critérios de aceitação:**

- `mvn -pl inventory-core verify` passa.
- Cobertura do use case > 90%.

### T-LOT-03 — Refatoração do serviço de seleção de lotes

**Objetivo:** transformar `SelecionarLotesPorFefoService` em serviço polimórfico baseado no flag.

**Pré-requisitos:** T-LOT-02.

**Entregáveis:**

- Renomear `SelecionarLotesPorFefoService` → `SelecionarLotesParaSaidaService`.
- Manter `SelecionarLotesPorFefoService` como deprecado por uma versão, anotando com `@Deprecated(since = "1.0.1", forRemoval = true)` e delegando para o novo.
- Nova lógica interna:

```java
public List<LoteParaBaixa> selecionar(InsumoId insumoId, FilialId filialId, QuantidadeBase qtd) {
    Insumo insumo = insumoRepo.findById(insumoId).orElseThrow();
    if (insumo.controlaValidade()) {
        return selecionarPorFefo(insumoId, filialId, qtd);
    }
    return baixarDoAgregador(insumoId, filialId, qtd);
}

private List<LoteParaBaixa> baixarDoAgregador(InsumoId insumoId, FilialId filialId, QuantidadeBase qtd) {
    Lote agregador = loteRepo.buscarAgregador(insumoId, filialId)
        .orElseThrow(() -> new SemSaldoException(insumoId, filialId));
    SaldoLote saldo = saldoLoteRepo.findByLote(agregador.id());
    if (saldo.quantidade().menorQue(qtd)) {
        throw new SaldoInsuficienteException(insumoId, filialId, qtd, saldo.quantidade());
    }
    return List.of(new LoteParaBaixa(agregador.id(), qtd));
}
```

- `BuscarOuCriarLoteAgregadorUseCase` para entradas: chamado pelo importador NF-e e pelo registro de entrada manual quando o insumo de destino não controla validade. Síncrono, idempotente, retorna sempre o mesmo lote por `(insumo, filial)`.
- Testes unitários cobrindo:
  - Insumo com validade, 1 lote: baixa total.
  - Insumo com validade, 2 lotes: FEFO escolhe o mais próximo do vencimento.
  - Insumo com validade, lote vencido em estoque: ignora vencido, lança alerta.
  - Insumo sem validade, agregador inexistente: lança `SemSaldoException`.
  - Insumo sem validade, agregador existente com saldo suficiente: baixa direta.
  - Insumo sem validade, agregador com saldo insuficiente: `SaldoInsuficienteException`.
  - Transição entre regimes não corrompe seleção (insumo migrado de `true → false` com lotes rastreados antigos: serviço deve preferir agregador mas tolerar baixa de lotes antigos se o agregador não tiver saldo — comportamento documentado, com flag `permitirFallbackParaLotesAntigos` no método para casos de migração).

**Critérios de aceitação:**

- `mvn -pl inventory-core verify` passa.
- 15+ novos testes só nesse serviço.
- Benchmark: baixa via agregador deve ser ≥2x mais rápida que FEFO multi-lote (medir com JMH em `inventory-core/src/jmh/`).

### T-LOT-04 — Importador NF-e (parser tolerante a ausência de `rastro`)

**Objetivo:** parser que decide o regime de entrada baseado no insumo de destino, não no XML.

**Pré-requisitos:** T-LOT-03, T-LOT-02, módulo de importação NF-e existente (criado fora do MVP 1.0 original, antecipado).

**Entregáveis:**

- `NfeRastroParser` retorna `Optional<DadosRastro>` em vez de lançar exceção quando o grupo está ausente.
- `ImportarItemNfeUseCase` decide:

```
1. Lookup de insumo via de-para fornecedor-produto.
2. Se de-para não encontrado: cria pendência tipo DEPARA_AUSENTE.
3. Insumo encontrado:
   a. Se controla_validade = true:
      - Se rastro presente: cria Lote(tipo=RASTREADO, validade=rastro.dVal).
      - Se rastro ausente: cria pendência tipo RASTRO_AUSENTE_PARA_PERECIVEL com bloqueio da entrada até resolução manual.
   b. Se controla_validade = false:
      - Ignora rastro mesmo se presente (não faz sentido rastrear sem necessidade — log warning e segue).
      - Chama BuscarOuCriarLoteAgregadorUseCase e empilha saldo.
```

- Nova tipo de pendência `RASTRO_AUSENTE_PARA_PERECIVEL` na fila de pendências, com ação possível: "marcar insumo como não-perecível e processar" ou "informar lote manualmente" ou "rejeitar item".
- Testes de integração com XML real (criar fixtures `src/test/resources/nfe/`):
  - NF-e com rastro completo para insumo perecível: entrada processada, lote rastreado criado.
  - NF-e sem rastro para insumo não-perecível: entrada processada, lote agregador usado.
  - NF-e sem rastro para insumo perecível: pendência criada, saldo não alterado.
  - NF-e com rastro para insumo não-perecível: warning logado, lote agregador usado, dados de rastro ignorados.
  - NF-e com múltiplos itens mistos (perecíveis com rastro + não-perecíveis sem rastro): cada item segue sua trilha.

**Critérios de aceitação:**

- 5+ testes de integração novos passando.
- Pendência aparece corretamente na tela de pendências do frontend.
- Log de warning quando rastro vem para insumo não-perecível (não é erro, é informação para o operador potencialmente reclassificar o insumo).

### T-LOT-05 — Frontend: cadastro de Insumo

**Objetivo:** UI do cadastro reflete o regime.

**Pré-requisitos:** T-LOT-02.

**Entregáveis:**

- Checkbox "Controlar validade?" no formulário de Insumo (novo e edição), default desmarcado.
- Quando marcado, revelam-se progressivamente os campos:
  - "Dias antes do vencimento para alertar" (number, default 7, min 1, max 90).
  - Texto explicativo: "Insumos com controle de validade são rastreados por lote, escolhidos por FEFO na saída."
- Quando desmarcado em insumo existente que tenha lotes rastreados com saldo: bloqueio visual com tooltip "Não é possível desativar — existem lotes com saldo. Esgote os lotes ou faça ajuste manual antes."
- Componente reutilizável `ControleValidadeToggle` em `frontend/src/components/insumo/`.
- Coluna "Validade?" na listagem de Insumos (badge verde "Sim" ou cinza "Não").
- Filtro na listagem: "Mostrar apenas insumos com controle de validade".

**Critérios de aceitação:**

- Cadastra insumo com e sem controle, persiste e exibe corretamente.
- Tentativa de desativar com saldo bloqueia e mostra erro vindo da API (não validação só do front).
- Testes Playwright cobrindo os dois fluxos.

### T-LOT-06 — Frontend: tela de Vendas (preview adaptativo)

**Objetivo:** preview de baixa muda comportamento por insumo conforme o regime.

**Pré-requisitos:** T-LOT-03, T-LOT-05, tela de Vendas (T-VENDAS-NN da reorganização anterior).

**Entregáveis:**

- Endpoint `POST /api/v1/vendas/preview` recebe `produtoVendavelId`, `quantidade`, `filialId` e retorna lista de `ItemBaixaPreview`, cada um com:
  ```json
  {
    "insumoId": "...",
    "insumoNome": "Mussarela",
    "quantidadeBase": 200,
    "unidadeBase": "g",
    "controlaValidade": true,
    "lotes": [
      { "loteId": "...", "numero": "MZ-2026-04-A", "validade": "2026-05-12", "quantidade": 200 }
    ],
    "saldoRestanteAposBaixa": 12100
  }
  ```
  Para `controlaValidade = false`, `lotes` retorna lista com um único item sem campos `numero` e `validade`.
- No frontend, renderização condicional na lateral do formulário de venda:
  - Insumo com validade: lista detalhada `Mussarela 200g — Lote MZ-2026-04-A (vence em 2 dias)`.
  - Insumo sem validade: linha simples `Sal 10g — saldo após: 4.990g`.
- Mesmo agrupamento visual, sem treatment diferente que destaque um ou outro como "menos importante".

**Critérios de aceitação:**

- Demo de venda de Pizza Margherita exibe mussarela e manjericão com lote, e azeite e sal sem lote, na mesma tela, na mesma renderização.
- Teste E2E Playwright cobrindo a venda mista.

### T-LOT-07 — Frontend: tela de Estoque

**Objetivo:** modal de detalhe de insumo respeita o regime.

**Pré-requisitos:** T-LOT-05.

**Entregáveis:**

- Ao clicar em um insumo na tela de Estoque por filial:
  - Insumo com validade: modal exibe tabela de lotes com colunas (Lote, Validade, Saldo, Dias para vencer, Status), igual ao comportamento atual.
  - Insumo sem validade: modal exibe card simples com Saldo Total + histórico das últimas 10 entradas (data, NF-e ou ajuste manual, quantidade entrada) e últimas 10 saídas (data, venda ou ajuste, quantidade baixada). Sem coluna de lote, sem coluna de validade.
- Ícone na linha do insumo na lista geral: relógio amarelo para "controla validade" (visível só com filtro avançado, para não poluir).

**Critérios de aceitação:**

- Modal renderiza corretamente para os dois tipos sem flicker e sem requisições redundantes.
- Lighthouse pontuação ≥ 90 na tela.

### T-LOT-08 — Atualização do seed da demo

**Objetivo:** demo passa a refletir a coexistência dos dois regimes, fortalecendo a narrativa.

**Pré-requisitos:** T-LOT-01 a T-LOT-07. Tarefa T-DEMO em andamento.

**Entregáveis:**

- Atualizar migration de seed (`db/migration-demo/`) marcando `controla_validade = true` para os insumos perecíveis e `false` para os não-perecíveis:

  **Com controle de validade (≈ 15 itens):** Mussarela, Calabresa, Picanha (peça), Linguiça toscana, Costela suína, Frango desfiado, Presunto, Tomate fresco, Manjericão, Rúcula, Cebola, Massa de pizza pré-assada, Molho de tomate caseiro, Iogurte natural, Mascarpone.

  **Sem controle de validade (≈ 30 itens):** Sal refinado, Açúcar cristal, Farinha de trigo, Fermento biológico seco, Óleo de soja, Azeite extra virgem (decisão: marcar como SEM controle — entra em garrafa fechada, validade longa, troca por consumo é mais relevante que validade), Pimenta-do-reino, Orégano, Vinagre balsâmico, Caixa de pizza 35cm, Caixa de pizza 40cm, Guardanapo de papel, Copo descartável 300ml, Talher descartável kit, Sachê de molho de pimenta, Sachê de azeite, Refrigerante 2L (Coca-Cola, Guaraná, Sprite, Fanta) — 4 itens distintos, Suco em lata 350ml (laranja, uva, maracujá) — 3 itens, Água mineral 500ml com gás, Água mineral 500ml sem gás, Cerveja long neck (3 marcas), Carvão (saco de 5kg), Acendedor.

- Para insumos sem controle, criar lote agregador pré-populado já com saldo realista em cada filial via script.
- Para insumos com controle, manter os 2-3 lotes por insumo por filial conforme seed original, com validades escalonadas.
- Atualizar `DemoMovimentacoesGenerator` para que vendas baixem corretamente dos dois regimes (a maioria dos pedidos terá insumos mistos — uma pizza usa mussarela com lote + caixa de pizza + sachê sem lote).
- Validar que o gráfico de movimentação no dashboard continua coerente.

**Critérios de aceitação:**

- `docker compose -f docker-compose.demo.yml up -d` sobe ambiente limpo e seed completo em < 3 minutos.
- Roteiro de apresentação executado de ponta a ponta sem erro.
- Demo de venda mostra explicitamente: insumo perecível com lote escolhido por FEFO + insumo não-perecível baixando direto. O apresentador deve poder dizer: "Vejam que mussarela é rastreada por lote — vence em 2 dias, sai primeiro — e a caixa de pizza, que não tem validade relevante, baixa direto do saldo. O sistema entende a diferença sem que o operador precise pensar nisso."

### T-LOT-09 — Testes de regressão e ArchUnit

**Objetivo:** garantir que as mudanças não quebraram comportamento existente e que a arquitetura não foi violada.

**Pré-requisitos:** T-LOT-01 a T-LOT-08.

**Entregáveis:**

- Suíte de regressão executada inteira: `mvn verify -P all-tests`.
- Regra ArchUnit nova: `lote_agregador_so_acessado_via_use_case` — repositórios não devem expor método público que crie lote agregador; criação só pode acontecer via `BuscarOuCriarLoteAgregadorUseCase`.
- Smoke E2E manual: cadastrar insumo, importar NF-e mista, registrar venda mista, transferir, ajustar, conferir saldos.
- Documentação atualizada: `docs/decisoes/ADR-XXX-lote-opcional.md` com o registro arquitetural completo, no formato ADR padrão (contexto, decisão, alternativas consideradas, consequências).

**Critérios de aceitação:**

- Build verde em CI completo, incluindo PIT (mutation testing > 70%).
- Cobertura JaCoCo geral não regrediu em mais de 1 ponto percentual.
- ADR commitado, lincado no README do módulo.

---

## 4. Migration SQL completa

Caminho: `inventory-core/src/main/resources/db/migration/V0XX__lote_opcional.sql` (substituir XX pelo próximo número disponível na sequência Flyway).

```sql
-- ============================================================
-- V0XX: Lote opcional via flag controla_validade no Insumo
-- ============================================================

-- 1. Adicionar flag no Insumo
ALTER TABLE insumo
    ADD COLUMN controla_validade BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN dias_alerta_vencimento INTEGER NULL
        CHECK (dias_alerta_vencimento IS NULL OR (dias_alerta_vencimento BETWEEN 1 AND 90));

COMMENT ON COLUMN insumo.controla_validade IS
    'Quando true, entradas exigem lote rastreado com validade e saída segue FEFO.
     Quando false, existe lote agregador único por (insumo, filial) e saída é direta.';
COMMENT ON COLUMN insumo.dias_alerta_vencimento IS
    'Dias antes do vencimento para disparar alerta. Só faz sentido com controla_validade = true.';

-- 2. Adicionar tipo no Lote
ALTER TABLE lote
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RASTREADO'
        CHECK (tipo IN ('RASTREADO', 'AGREGADOR'));

COMMENT ON COLUMN lote.tipo IS
    'RASTREADO: lote real com numero e validade, fluxo FEFO.
     AGREGADOR: lote sintético único por (insumo, filial) para insumos sem controle de validade.';

-- 3. Tornar data_validade nullable
ALTER TABLE lote
    ALTER COLUMN data_validade DROP NOT NULL;

-- 4. Constraint: lote rastreado precisa de validade; agregador não precisa
ALTER TABLE lote
    ADD CONSTRAINT lote_validade_obrigatoria_quando_rastreado
        CHECK (
            (tipo = 'RASTREADO' AND data_validade IS NOT NULL)
            OR (tipo = 'AGREGADOR' AND data_validade IS NULL)
        );

-- 5. Constraint: número de lote obrigatório só para RASTREADO
ALTER TABLE lote
    ALTER COLUMN numero DROP NOT NULL,
    ADD CONSTRAINT lote_numero_obrigatorio_quando_rastreado
        CHECK (
            (tipo = 'RASTREADO' AND numero IS NOT NULL AND length(trim(numero)) > 0)
            OR (tipo = 'AGREGADOR' AND numero IS NULL)
        );

-- 6. Índice único parcial: só pode existir um lote AGREGADOR por (insumo, filial)
CREATE UNIQUE INDEX uq_lote_agregador_por_insumo_filial
    ON lote (insumo_id, filial_id)
    WHERE tipo = 'AGREGADOR';

-- 7. Índice para acelerar busca de agregador (uso constante no path quente)
CREATE INDEX idx_lote_agregador_lookup
    ON lote (insumo_id, filial_id, tipo)
    WHERE tipo = 'AGREGADOR';

-- 8. Backfill: insumos existentes ficam com controla_validade = false por padrão.
--    Time decide depois quais marcar como true via UI (operação suportada por T-LOT-02).
--    Não fazer backfill automático de controla_validade = true mesmo para insumos com
--    lotes existentes, porque pode haver lotes criados experimentalmente em insumos que
--    o cliente quer reclassificar.

-- 9. Para cada (insumo, filial) que NÃO tenha lote nenhum ainda, criar agregador vazio.
--    Isso simplifica a lógica do use case downstream (não precisa criar sob demanda).
INSERT INTO lote (id, insumo_id, filial_id, tipo, data_validade, numero, criado_em)
SELECT
    gen_random_uuid(),
    i.id,
    f.id,
    'AGREGADOR',
    NULL,
    NULL,
    now()
FROM insumo i
CROSS JOIN filial f
WHERE NOT EXISTS (
    SELECT 1 FROM lote l
    WHERE l.insumo_id = i.id AND l.filial_id = f.id AND l.tipo = 'AGREGADOR'
);

-- 10. Inicializar SaldoLote zero para os agregadores recém-criados, para evitar nulls
INSERT INTO saldo_lote (lote_id, quantidade_base, atualizado_em)
SELECT l.id, 0, now()
FROM lote l
WHERE l.tipo = 'AGREGADOR'
  AND NOT EXISTS (SELECT 1 FROM saldo_lote sl WHERE sl.lote_id = l.id);
```

**Observações sobre a migration:**

- O passo 9 cria agregadores vazios proativamente para evitar `EmptyResultDataAccessException` no path quente do serviço de seleção. Custo de espaço é desprezível (registros pequenos, sem saldo).
- O passo 10 garante saldo zero, eliminando outro caso de borda.
- A migration é forward-only. Reverter (voltar a `data_validade NOT NULL`) exige migração inversa custosa porque já haverá lotes agregadores com `NULL`. Aceitar como decisão irreversível.

---

## 5. Apêndice A — Casos de borda

### A.1 Transferência entre filiais com insumos mistos

Filial A transfere para Filial B uma quantidade que mistura mussarela (com validade) e sal (sem validade).

- Para mussarela: a transferência preserva o lote rastreado (mesmo `lote_id` em ambas as filiais não, pois `lote_id` carrega `filial_id` na chave funcional — na verdade o que se preserva é `numero` e `data_validade`; cria-se um novo `Lote` em Filial B com mesmos atributos e `transferencia_origem_id` apontando para o lote de origem para rastreabilidade).
- Para sal: a transferência simplesmente reduz o `SaldoLote` do agregador da Filial A e aumenta o do agregador da Filial B. Nenhum lote novo é criado.

O `RegistrarRecebimentoTransferenciaUseCase` precisa de uma adaptação para tratar os dois casos. Cobrir com teste de integração com transferência mista.

### A.2 Ajuste manual

A tela de Ajuste continua aceitando qualquer insumo. Para insumos sem validade, o ajuste opera direto no agregador. Para insumos com validade, o operador precisa escolher o lote (ou o sistema escolhe o de validade mais próxima se for ajuste negativo, com confirmação).

### A.3 Carga inicial de filial

Carga inicial é tratada como múltiplas entradas. Para insumos sem validade, alimenta o agregador. Para insumos com validade, exige número de lote e validade na importação CSV (campos `numero_lote`, `data_validade` ficam opcionais no template, mas obrigatórios condicionalmente conforme o insumo).

### A.4 Curva ABC e relatórios

Sem mudança. A curva ABC opera sobre consumo por insumo (somando todos os lotes), indiferente ao regime. Relatório de ruptura também. Relatório de "vencimento próximo" ignora silenciosamente insumos sem validade.

### A.5 Alertas

- `ESTOQUE_MINIMO_PERCENTUAL`, `ESTOQUE_MINIMO_ABSOLUTO`, `RUPTURA`: aplicam aos dois regimes.
- `VENCIMENTO_PROXIMO_DIAS`: aplica só a insumos com `controla_validade = true`. Avaliador deve filtrar.

### A.6 Insumo reclassificado

Operador cadastra Azeite como "sem controle de validade". Após 6 meses, decide ativar controle (entendeu que precisa rastrear lotes para qualidade). Comportamento:

- Saldo existente fica no agregador (não é tocado).
- Novas entradas a partir da reclassificação criam lotes rastreados.
- Saídas seguem prioridade: lotes rastreados via FEFO; se esgotados, cai para o agregador (com flag `permitirFallbackParaLotesAntigos` no serviço — comportamento documentado).
- Após o saldo do agregador zerar, operador pode optar por arquivar o agregador (não é deletado, fica como histórico).

### A.7 Insumo desclassificado

Operador desativa controle de validade em insumo que tinha lotes rastreados. Bloqueio na API se houver saldo rastreado. Se quiser forçar, precisa primeiro fazer ajuste manual movendo o saldo dos lotes rastreados para o agregador, ou consumir os lotes rastreados via vendas/perda. Comportamento conservador deliberado — evita perder rastreabilidade por acidente.

---

## 6. Checklist de validação final

Marcar cada item antes de declarar T-LOT concluída:

- [ ] Migration aplica em banco limpo e em banco com dados existentes.
- [ ] `controla_validade` editável via UI no cadastro de Insumo.
- [ ] Importação NF-e processa item sem `rastro` para insumo não-perecível.
- [ ] Importação NF-e gera pendência para item sem `rastro` em insumo perecível.
- [ ] Venda de produto cuja ficha técnica tem insumos mistos baixa corretamente os dois regimes.
- [ ] Transferência entre filiais com insumos mistos preserva lote rastreado e ajusta agregador.
- [ ] Ajuste manual funciona para os dois regimes.
- [ ] Alertas de vencimento ignoram insumos sem validade.
- [ ] Curva ABC, ruptura e estoque mínimo aplicam aos dois regimes.
- [ ] Tela de estoque renderiza modal apropriado por regime.
- [ ] Roteiro de demo executa sem erro e exibe os dois comportamentos lado a lado.
- [ ] ArchUnit verde.
- [ ] Cobertura JaCoCo não regrediu.
- [ ] ADR-XXX commitado.

---

**Fim do adendo.**
