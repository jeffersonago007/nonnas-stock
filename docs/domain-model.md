# Modelo de Domínio — Nonnas Stock

Documento incremental que descreve as entidades, value objects e serviços
de domínio dos bounded contexts do sistema. Atualizado a cada nova tarefa.

> **Nota:** o objetivo deste documento é dar uma visão de uma página por
> contexto, suficiente para um dev novo entender o modelo sem ler o código.
> Para detalhes de campos, consulte os arquivos `*.java` do pacote
> `domain` de cada módulo.

## Bounded contexts entregues

| Módulo | Tarefa | Status |
|---|---|---|
| shared-kernel | T01 | ✅ Concluído |
| identity | T02 | ✅ Concluído |
| catalog | T03 | ✅ Concluído |
| inventory-core | T04 | ✅ Concluído |

---

## shared-kernel (T01)

Tipos transversais sem domínio específico.

- **`Money(BigDecimal amount, Currency currency)`** — record imutável. Escala 4 (operações), método `asTotal()` arredonda para 2 (display). Recusa aritmética entre moedas distintas.
- **`Quantity(BigDecimal value, UUID unidadeMedidaId)`** — record imutável. Escala 4. Conversão entre unidades é responsabilidade do `ConversorUnidadeService` (catalog).
- **`EntityId<T>`** — interface marker para IDs tipados. Cada módulo declara seus records concretos (ex.: `record InsumoId(UUID value) implements EntityId<Insumo>`).
- **`Result<T>`** — sealed interface com `Success<T>(T value)` e `Failure<T>(ErrorCode code, String message)`. Pattern-match-friendly. Para casos previstos onde exception seria pesada.
- **Hierarquia de exceções**: `DomainException` (sealed) → `ValidationException`, `BusinessRuleException`, `NotFoundException` (todas non-sealed para subclassing por módulos).
- **`ErrorCode`** enum: `VALIDATION_FAILED`, `BUSINESS_RULE_VIOLATED`, `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `CONFLICT`, `UNEXPECTED`.

---

## identity (T02)

Empresa, Filial, Usuário e fluxo de autenticação JWT.

### Entidades

```
Empresa
  └─ id: EmpresaId
     razaoSocial: RazaoSocial (1-255 chars)
     cnpj: Cnpj (14 dígitos com check digit)
     ativa: boolean
     createdAt/updatedAt: Instant

Filial
  └─ id: FilialId
     empresaId: EmpresaId
     nome: String (1-255 chars)
     cnpj: Cnpj
     endereco: Optional<String>
     ativa: boolean
     createdAt/updatedAt: Instant

Usuario  (entidade rica com brute-force inline)
  └─ id: UsuarioId
     filialId: Optional<FilialId>  (null para ADMIN)
     nome: String
     email: Email (lowercase, regex-validado)
     senhaHash: SenhaHash (BCrypt; toString mascara)
     perfil: Perfil { ADMIN | GERENTE | OPERADOR | CONSULTA }
     ativo: boolean
     tentativasFalhas: int
     bloqueadoAte: Optional<Instant>
     travada: boolean
```

### Regras de negócio

- **Brute-force progressivo** (master doc 13.3): 3 falhas → bloqueio 15min, 5 falhas → 1h, 10 falhas → conta travada (libera só por ADMIN).
- **JWT refresh rotation com replay detection** (ADR 0003): cada uso de refresh emite novo par e invalida o anterior; reuso de refresh já consumido revoga toda a família.
- **Política de senha** (`@SenhaValida`, antecipada de T16 por ADR 0006 D): mínimo 10 chars + ≥1 letra + ≥1 número + ≥1 especial.

### Tabelas auxiliares

- `refresh_tokens(jti, family_id, parent_jti, usuario_id, expires_at, revoked_at)` — trilha da rotação por família.
- `historico_senhas(usuario_id, senha_hash, criada_em)` — janela das últimas 5 senhas (rejeitadas em troca; endpoint de troca virá em T16).

---

## catalog (T03)

Catálogo de insumos, unidades de medida e conversões.

### Entidades

```
CategoriaInsumo  (auto-relação opcional)
  └─ id: CategoriaInsumoId
     categoriaPaiId: Optional<CategoriaInsumoId>
     nome: String
     ativa: boolean

UnidadeMedida
  └─ id: UnidadeMedidaId
     codigo: String (UPPERCASE, único, ex.: "KG")
     nome: String
     tipo: UnidadeMedidaTipo { PESO | VOLUME | UNIDADE }
     ativa: boolean

ConversaoUnidade  (record imutável)
  └─ id: UUID
     origemId, destinoId: UnidadeMedidaId
     fator: BigDecimal (positivo)
     insumoId: Optional<InsumoId>  (null = global)
     createdAt: Instant

Fornecedor
  └─ id: FornecedorId
     razaoSocial: String
     cnpj: Cnpj
     ativo: boolean

Insumo
  └─ id: InsumoId
     codigo: String (único, ex.: "INS-001")
     nome: String
     categoriaId: CategoriaInsumoId
     unidadeBaseId: UnidadeMedidaId   ← unidade canônica para math interna
     controlaLote: boolean
     controlaValidade: boolean
     ativo: boolean

InsumoFilial
  └─ id: InsumoFilialId
     insumoId: InsumoId
     filialId: UUID  ← FK lógica para identity.filiais; FK física em T09
     estoqueMinimo: BigDecimal (≥ 0)
     estoqueMaximo: Optional<BigDecimal>
     pontoPedido: Optional<BigDecimal>
     ativo: boolean
```

### `ConversorUnidadeService` — coração do módulo

Domain service puro (sem Spring), recebe `ConversaoUnidadeRepository` por
construtor. Algoritmo de cascata exigido pelo master doc seção 5.1:

1. `origem == destino` → fator 1, sem lookup.
2. Conversão **específica do insumo, direta** (`insumo_id, origem, destino`).
3. Conversão **específica do insumo, inversa** (`insumo_id, destino, origem`) → derivada como `1 / fator` em escala 10 com HALF_EVEN.
4. Conversão **global, direta** (`insumo_id IS NULL, origem, destino`).
5. Conversão **global, inversa** → derivada.
6. Caso contrário, lança `UnidadeNaoConversivelException` (estende `BusinessRuleException` com `ErrorCode.BUSINESS_RULE_VIOLATED`, mapeada para HTTP 422).

**Bidirecionalidade automática:** basta cadastrar uma direção (ex.: KG→G fator 1000); o serviço deriva G→KG (1/1000) sob demanda. Reduz duplicação.

**Sem transitividade:** KG→G→MG não é deduzido. Cadastrar conversão direta KG→MG se necessário. Decisão deliberada para manter o algoritmo previsível.

### Seed (V004)

Unidades padrão: G, KG, ML, L, UN, CX, PORCAO.
Conversões globais: KG → G (1000) e L → ML (1000).

### Cross-module reference

`InsumoFilial.filial_id` é UUID sem FK física para `identity.filiais` no T03.
A FK é adicionada em T09 (app/) quando as migrations de todos os módulos
são executadas pelo mesmo Flyway. Catalog não importa types de identity
para preservar isolamento entre bounded contexts.

---

## inventory-core (T04)

Núcleo do sistema. Lote, saldo materializado por (lote, filial), movimentação imutável com itens, FEFO.

### Entidades

```
Lote (record imutável)
  └─ id: LoteId
     insumoId: UUID  (FK lógica catalog.insumos)
     fornecedorId: Optional<UUID>
     notaFiscalId: Optional<UUID>  (NF-e em onda 1.1)
     numeroLote: String
     dataFabricacao: Optional<LocalDate>
     dataValidade: Optional<LocalDate>
     valorUnitario: BigDecimal (≥ 0)

SaldoLote (record imutável; chave composta lote_id + filial_id)
  └─ loteId: LoteId
     filialId: UUID
     quantidadeBase: BigDecimal  (pode ser negativa — vide regra master doc 5.2)
     atualizadoEm: Instant

Movimentacao (record imutável — auditoria é trilha, nunca estado mutável)
  └─ id: MovimentacaoId
     filialId, usuarioId: UUID
     tipo: TipoMovimentacao { ENTRADA_NF | ENTRADA_AJUSTE | ... | SAIDA_VENDA | ... | SAIDA_VENCIMENTO }
     dataMovimentacao: Instant
     documentoOrigemTipo/Id: Optional
     observacao: Optional<String>
     gerouNegativo: boolean  (true quando saída excedeu saldo disponível)
     itens: List<ItemMovimentacao>  (defensive copy, unmodifiable)

ItemMovimentacao (record)
  └─ id: UUID
     insumoId: UUID
     loteId: LoteId
     unidadeLancamentoId: UUID  (catalog.unidades_medida)
     quantidadeLancada: BigDecimal  (na unidade que o operador escolheu)
     quantidadeBase: BigDecimal  (após conversão pela ConversorUnidadeService)
     valorUnitario: BigDecimal
```

### `SelecionarLotesPorFefoService` — algoritmo FEFO

Domain service puro (sem Spring), recebe `SaldoLoteRepository` por construtor.

1. Query lotes do insumo na filial com saldo > 0, ordenados por
   `data_validade NULLS LAST, lote.id ASC`. Lock pessimista
   (`PESSIMISTIC_WRITE`) serializa saídas concorrentes no mesmo lote.
2. Itera consumindo até completar a quantidade. Pode produzir múltiplas
   alocações (uma por lote consumido).
3. Se a soma dos saldos não basta, o restante é alocado no último lote —
   saldo desse lote ficará negativo. Flag `gerouNegativo = true` é
   anexada à movimentação. Restaurante prefere registrar a venda mesmo
   sem estoque a bloquear o pedido (regra invariante master doc 5.2).
4. Sem lote algum disponível: `Resultado.semLotes() == true`. Use case
   lança 422 — não há nem onde registrar a saída.

### Saldo materializado via `@EventListener`

`SaldoLoteListener` escuta `MovimentacaoCriadaEvent` (publicado pelo use
case na mesma transação) e atualiza `saldos_lotes` somando ou subtraindo
quantidade base por item. `Propagation.MANDATORY` garante que se a
transação da movimentação aborta, o saldo também aborta.

Saldo é **projeção**, não fonte da verdade — a verdade é a soma das
movimentações. Job de conciliação (T08+) compara projeção com soma e
alerta divergências.

### Cross-module references

- `lote.insumo_id` → catalog.insumos
- `lote.fornecedor_id` → catalog.fornecedores
- `movimentacao.filial_id` → identity.filiais
- `movimentacao.usuario_id` → identity.usuarios
- `item_movimentacao.unidade_lancamento_id` → catalog.unidades_medida

Todos UUIDs sem FK física — consolidação em T09.

---

## Próximos contextos (não entregues ainda)

- **recipes (T05):** ProdutoVendavel, FichaTecnica versionada, ItemFichaTecnica.
- **operations (T06):** Transferência (state machine), Carga inicial, Ajuste.
- **alerts (T07):** AlertaConfig, AlertaDisparado, AvaliadorAlertasService.
- **reporting (T08):** views materializadas, queries de dashboards.

Cada nova tarefa estende este documento com sua seção própria.
