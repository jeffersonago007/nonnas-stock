# ADR 0011 — AbstractIntegrationTest distribuído via shared-kernel test-jar

- **Status:** Aceita
- **Data:** 2026-05-09
- **Contexto da decisão:** T10 pede uma "configuração de Testcontainers reusável (`AbstractIntegrationTest` com Postgres compartilhado entre testes)". Precisamos compatibilizar isso com (a) o ADR 0007 que adotou Zonky em vez de Testcontainers e (b) o contrato do shared-kernel "zero deps Spring/JPA/Lombok no main".

## Contexto

Cada bounded context tinha sua `Abstract<Modulo>IntegrationTest` (~17 linhas, anotações idênticas):

```java
@SpringBootTest(webEnvironment = MOCK, classes = ModuloTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
public abstract class AbstractModuloIntegrationTest { }
```

Sete cópias com a mesma trinca `@AutoConfigureMockMvc + @ActiveProfiles + @AutoConfigureEmbeddedDatabase`. O master doc T10 pede consolidação. Três opções foram avaliadas:

**A. Novo módulo Maven `test-commons`** com Spring Boot test e Zonky em scope compile, consumido em scope test pelos demais.
- Prós: limpo; encapsulado.
- Contras: mais um módulo no reactor; ADR 0001 prega contenção do número de módulos.

**B. Shared-kernel principal absorvendo Spring Boot test e Zonky em scope compile.**
- Contras: viola "zero deps Spring/JPA/Lombok no main" — restrição explícita do `shared-kernel/pom.xml`.

**C. Shared-kernel publicando test-jar** (`<type>test-jar</type>`), com Spring Boot test + Zonky em scope **test** apenas.
- Prós: respeita o contrato do main do shared-kernel (test scope não vaza pra runtime); zero módulos novos; alinhado com a redação literal do master doc ("Test fixtures compartilhados em `shared-kernel/src/test/java/.../testsupport/`").
- Contras: Maven boilerplate adicional (`<type>test-jar</type>`) em cada módulo consumidor; uma cópia das deps test em cada consumidor (Zonky etc.).

## Decisão

**Adotamos a Opção C.** O shared-kernel passa a publicar um test-jar via `maven-jar-plugin:test-jar`. Os 7 módulos com ITs declaram:

```xml
<dependency>
  <groupId>com.nonnas</groupId>
  <artifactId>shared-kernel</artifactId>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

`AbstractIntegrationTest` (em `com.nonnas.sharedkernel.testsupport`) traz a trinca de anotações Zonky/MockMvc/profile. Cada `Abstract<Modulo>IntegrationTest` herda dela e adiciona apenas `@SpringBootTest(classes = <Modulo>TestApplication.class)`. Essa composição funciona porque Spring Test mescla anotações da hierarquia ao montar o contexto.

### Sobre "Testcontainers" no master doc

O texto do master doc fala "Testcontainers reusável", mas o ADR 0007 já aceitou Zonky como substituto (no Linux dev local não havia Docker). Essa ADR 0011 **reafirma** essa decisão e clarifica: o que o master doc pede operacionalmente — "Postgres reusável compartilhado entre testes" — é satisfeito por Zonky com prefetch global do binário Postgres em `~/.embedpostgresql/`. Cada IT instancia um banco efêmero rápido (~1s) sem precisar de Docker.

Se em produção/CI exigirem Testcontainers no futuro (por ex. testar extensões nativas Postgres não suportadas pelo binário Zonky), a troca é localizada: substituir o `@AutoConfigureEmbeddedDatabase` no `AbstractIntegrationTest` por `@Testcontainers + @Container` e adicionar a dep correspondente no test-jar do shared-kernel. Os ITs dos módulos não precisam mudar.

## Consequências

**Positivas:**
- Sete `Abstract<Modulo>IntegrationTest` reduzem-se de ~17 linhas para 5–10 linhas cada (só `@SpringBootTest(classes = ...)` específico).
- Configuração de teste tem fonte única — mudanças (ex. `@DirtiesContext` global) tocam apenas o shared-kernel.
- Builders comuns (`Builders.brl`, `Builders.quantity`, `Builders.newId`) ficam disponíveis pra qualquer módulo importar.
- Preserva o contrato `shared-kernel main = zero deps Spring`.

**Negativas:**
- Cada módulo precisa repetir o boilerplate `<dependency><type>test-jar</type></dependency>`.
- Consumidores precisam declarar Zonky em test scope mesmo assim (test-jar não faz fan-out de deps de outros consumidores).

**Mitigações:**
- ArchUnit em `quality-tests` valida que `shared-kernel.*` (main) não importa Spring/JPA/Lombok — quebra o build se alguém adicionar dep equivocada.
- O boilerplate por módulo é cosmético; futuras consolidações (ADR posterior, T11+) podem mover deps comuns para o `<dependencyManagement>` do parent.

## Referências

- ADR 0001 — Modular Monolith (princípio: minimizar número de módulos Maven).
- ADR 0007 — Embedded Postgres em vez de Testcontainers.
- PROMPT_CLAUDE_CODE.md seção 7 (estratégia de testes), seção 11 T10 (entregáveis).
