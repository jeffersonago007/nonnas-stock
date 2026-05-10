# ADR 0013 — Estratégia de Importação de NF-e

- **Status:** Aceita
- **Data:** 2026-05-10
- **Contexto da decisão:** T20 do master doc precisa permitir lançamento de notas fiscais (manual ou via XML NF-e modelo 55) que cria fornecedor + insumo + entrada de estoque numa única operação. Quatro pontos de design exigiam decisão antes de codar; ficaram registrados aqui.

## Contexto

O fluxo "operador chega com a nota e lança em estoque" não existia no MVP 1.0. Hoje há Movimentação de Entrada item-a-item, mas sem o conceito de Nota Fiscal como entidade. NF-e XML traz fornecedor (CNPJ + razão), itens (código + descrição + unidade comercial + quantidade + valor), mas **não traz** categoria do insumo nem políticas internas (controla_lote, controla_validade). Também precisa decidir como matchear `cProd` da nota com o `codigo` do insumo no catálogo, já que cada fornecedor usa seu próprio código.

Avaliamos as decisões em quatro frentes.

### Decisão 1 — Modelagem da Nota Fiscal

Opções:
- **A.** Não criar entidade — usar Movimentação com `documento_origem_id` apontando para id externo arbitrário.
- **B.** Criar entidade `NotaFiscal` própria (operations) com tabela e relacionamento direto com Movimentação. **Escolhida.**

**Por quê B:** rastreabilidade (lista de notas por fornecedor/período), idempotência por chave NFe (`UNIQUE chave_nfe` bloqueia reentrada da mesma NF-e), prepara cadastro fiscal pra integrações futuras. Custo extra mínimo (1 tabela + 1 tabela de itens).

### Decisão 2 — Categoria do insumo novo

Opções:
- **A.** Categoria fixa "A classificar" criada por seed; usuário ajusta depois em /admin/categorias. **Escolhida.**
- B. Modal interativo no preview obriga classificar cada item antes de lançar.

**Por quê A:** não trava o fluxo operacional do dia-a-dia. Operador pode lançar 10 notas seguidas sem precisar parar pra classificar. Categoria é metadado de relatório/alerta — pode ser organizado em batch na própria tela admin posteriormente. Categoria fixa tem UUID determinístico (`00000000-0000-0000-0000-000000000001`) para o use case referenciar via constante, semeada em `catalog/V015`.

### Decisão 3 — `controla_lote` e `controla_validade` ao criar insumo

Opções:
- **A.** Default `true` em ambos. **Escolhida.**
- B. Default `false`.
- C. Pergunta no preview pra cada item novo.

**Por quê A:** restaurante/pizzaria opera com perecíveis. Default conservador (lote+validade obrigatórios) força disciplina FEFO desde o primeiro lançamento. Se operador depois identificar insumo não-perecível (ex.: temperos secos), edita em /insumos. O custo de "false default" seria insumos nascendo sem rastreabilidade — risco sanitário.

### Decisão 4 — Matching de insumo já existente

Opções:
- A. Por `cProd` exato com `codigo` do insumo — frágil; cada fornecedor tem cProd próprio.
- B. Por `xProd` (descrição) similarity — ambíguo, falsos positivos.
- **C.** Tabela de **de-para fornecedor + cProd → insumoId** com aprendizado automático ao confirmar lançamento. **Escolhida.**

**Por quê C:** primeira nota de um fornecedor sugere "novo insumo" no preview. Operador escolhe insumo existente OU aceita criar novo. Ao confirmar, sistema persiste o par (fornecedor, cProd) → insumoId. Próxima nota do mesmo fornecedor com mesmo cProd já vem com matching automático no preview. Ao longo do tempo o de-para fica completo e o operador deixa de re-trabalhar matching. É o padrão da indústria (NF-e import da Bling, Linx, TOTVS funcionam assim). Custo: 1 tabela extra + 1 lookup por item ao processar — desprezível.

## Implicações

- **Backend novo:** módulo `nfe-importer` ganha conteúdo (parser XML + orquestrador). `operations` ganha entidade `NotaFiscal`, `ItemNotaFiscal` e tabela `fornecedor_insumo_depara`. `nfe-importer` pode importar `catalog`, `inventory-core`, `operations` (regra ArchUnit estendida — única exceção, documentada).
- **Migrations:** `catalog/V015` (seed categoria "A classificar"), `operations/V016` (schema notas_fiscais + itens + de-para). Numerações altas para evitar colisão com migrations de outros módulos (V005/V008 estavam ocupadas).
- **Idempotência:** chave NFe é UNIQUE. Reentrada da mesma nota retorna 409 sem efeito colateral. Lançamento manual (sem chave) não dispara essa proteção — responsabilidade do operador.
- **Não-escopo registrado para ondas futuras:** import de PDF/DANFE (alto custo, baixa cobertura), validação de assinatura digital SEFAZ, importação em lote (.zip), edição de nota lançada (cancelamento via estorno sim; edição não), UI dedicada para gerenciar de-para (aprendizado automático cobre 95% do uso).

## Status

Aceita 2026-05-10 com a entrega do T20.
