# ADR 0008 — `recipes` depende de `inventory-core` em escopo `compile`

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** T05 — `RegistrarVendaSimuladaUseCase` precisa baixar todos os insumos da ficha técnica via FEFO

## Contexto

T05 introduz `RegistrarVendaSimuladaUseCase` no módulo `recipes`. Esse use case recebe `produtoVendavelId` + `quantidadeVendida`, busca a ficha técnica vigente, e precisa gerar **uma única** `Movimentacao SAIDA_VENDA` com todos os insumos baixados via FEFO.

A operação é cross-module: `recipes` orquestra, mas a movimentação e o algoritmo FEFO vivem em `inventory-core`. O master doc exige (seção 1.3, princípio 1): "Acoplamento entre módulos só por contratos públicos (`api` package) ou eventos de aplicação".

Alternativas avaliadas:

1. **Eventos de aplicação:** `recipes` publica `VendaSimuladaSolicitada(produtoId, quantidade, filial)`, `inventory-core` escuta e gera a movimentação. Devolver o resultado da movimentação para o controller de venda exigiria padrão request/reply síncrono em cima de eventos — over-engineering.
2. **API contract package em `inventory-core`:** Expor um pacote `com.nonnas.inventory.api` com a interface do use case + DTOs de comando/resposta, e fazer `recipes` depender desse contrato. Adiciona uma camada de indireção que o MVP 1.0 não monetiza — todo o monolito é deployado junto.
3. **Maven dependency direta `recipes → inventory-core`:** `recipes/pom.xml` declara `<dependency>com.nonnas:inventory-core</dependency>` em escopo `compile`. `RegistrarVendaSimuladaUseCase` injeta e chama `RegistrarSaidaMultiItemUseCase` diretamente.

## Decisão

**Adotar a opção 3: dependência Maven direta `recipes → inventory-core` em escopo `compile`.**

Justificativa:
- O master doc (princípio 1) restringe acoplamento, mas não proíbe dependências Maven cross-module — proíbe acoplamento implícito (compartilhamento de tabelas, joins SQL cross-schema, etc.). Chamar um use case publicado é acoplamento explícito e contratado.
- A operação de venda **precisa** ser síncrona e atômica (mesma transação): se algum insumo não tem lote, a venda inteira rola back. Eventos síncronos resolveriam, mas o overhead conceitual não compensa.
- Mantemos `inventory-core` totalmente independente de `recipes` (nenhuma dependência reversa). A direção de acoplamento é unidirecional: `recipes ⇒ inventory-core ⇒ shared-kernel`.

Implementação:
- `recipes/pom.xml` adiciona `<dependency>com.nonnas:inventory-core</dependency>` (compile, sem `<scope>`).
- Cria-se um use case dedicado em `inventory-core`: `RegistrarSaidaMultiItemUseCase`. Recebe `List<ItemSaida(insumoId, unidadeLancamentoId, quantidadeBase)>` + dados da movimentação. Aplica FEFO insumo por insumo, consolida todos os `ItemMovimentacao` em uma única `Movimentacao`, publica um único `MovimentacaoCriadaEvent`. `@Transactional` garante atomicidade.
- O use case `RegistrarSaidaManualUseCase` original (saída manual de UI, 1 insumo por vez) **não é tocado** — clientes existentes seguem inalterados.

## Consequências

**Positivas:**
- Use case de venda é simples e legível: uma chamada cross-module síncrona.
- `RegistrarSaidaMultiItemUseCase` é reutilizável: T06 (transferência multi-insumo) e T08 (relatórios consolidados) podem usar o mesmo contrato sem duplicar lógica FEFO.
- Atomicidade transacional é trivial — uma única transação cobre busca da ficha + N alocações FEFO + criação da movimentação + atualização de saldo materializado.

**Negativas:**
- Estabelece o precedente de dependências cross-module via Maven. Próximos módulos precisam justificar caso a caso (não é livre-arbítrio).
- Mudanças em assinatura pública de `inventory-core` podem quebrar `recipes` (e qualquer futuro consumidor). Mitigação: o pacote `application.movimentacao` de `inventory-core` passa a ser API pública de fato; mudanças exigem versionamento responsável.
- Aumenta levemente o classpath de `recipes` em build standalone (irrelevante hoje, monolito deploy junto).

**Mitigações:**
- ArchUnit (T13) deve adicionar regra "nenhuma dependência reversa: `inventory-core` jamais importa de `recipes`".
- Quando T15 introduzir `api` packages explícitos por módulo, considerar refatoração para pacotes-contrato dedicados — esta ADR pode ser revisada.

## Referências

- PROMPT_CLAUDE_CODE.md seção 1.3 (princípios arquiteturais), seção 11 T05 (`RegistrarVendaSimuladaUseCase`).
- ADR 0001 — Modular Monolith.
- ADR 0004 — FEFO como estratégia de saída.
- ADR 0005 — Versionamento de receita via snapshot.
