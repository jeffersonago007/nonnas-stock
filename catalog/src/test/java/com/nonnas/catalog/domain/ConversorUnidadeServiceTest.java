package com.nonnas.catalog.domain;

import com.nonnas.catalog.application.ports.ConversaoUnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre os 9 cenários exigidos pelo critério de aceitação T03 (mínimo 8):
 * <ol>
 *   <li>Origem == destino → fator 1.</li>
 *   <li>Conversão global direta (KG → G).</li>
 *   <li>Conversão global inversa (G → KG, derivada como 1/1000).</li>
 *   <li>Conversão específica do insumo (CX → KG, mussarela).</li>
 *   <li>Específica precede global mesmo quando ambas aplicáveis.</li>
 *   <li>Conversão específica inversa (KG → CX, mussarela, derivada).</li>
 *   <li>Sem conversão definida (KG → ML, sem insumo) → exception.</li>
 *   <li>Insumo passado mas sem conversão específica nem global → exception.</li>
 *   <li>Multiplicação correta no método converter (não apenas resolverFator).</li>
 * </ol>
 */
class ConversorUnidadeServiceTest {

    private static final UnidadeMedidaId KG = UnidadeMedidaId.generate();
    private static final UnidadeMedidaId G  = UnidadeMedidaId.generate();
    private static final UnidadeMedidaId ML = UnidadeMedidaId.generate();
    private static final UnidadeMedidaId L  = UnidadeMedidaId.generate();
    private static final UnidadeMedidaId CX = UnidadeMedidaId.generate();

    private static final InsumoId MUSSARELA = InsumoId.generate();
    private static final InsumoId BACALHAU  = InsumoId.generate();

    private InMemoryConversaoUnidadeRepository repo;
    private ConversorUnidadeService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryConversaoUnidadeRepository();
        service = new ConversorUnidadeService(repo);

        // Conversão global: KG → G (factor 1000)
        repo.insertGlobal(KG, G, new BigDecimal("1000"));
        // Conversão global: L → ML (factor 1000)
        repo.insertGlobal(L, ML, new BigDecimal("1000"));
        // Conversão específica: caixa de mussarela = 5 KG (CX → KG factor 5)
        repo.insertPorInsumo(CX, KG, new BigDecimal("5"), MUSSARELA);
    }

    @Nested
    class Cenario1_OrigemIgualDestino {
        @Test
        void retornaFator1() {
            BigDecimal f = service.resolverFator(KG, KG, null);
            assertThat(f).isEqualByComparingTo(BigDecimal.ONE);
        }
    }

    @Nested
    class Cenario2_GlobalDireta {
        @Test
        void kgParaG_fator1000() {
            BigDecimal valorEmG = service.converterGlobal(new BigDecimal("2"), KG, G);
            assertThat(valorEmG).isEqualByComparingTo("2000");
        }
    }

    @Nested
    class Cenario3_GlobalInversa {
        @Test
        void gParaKg_fatorDerivado() {
            BigDecimal valorEmKg = service.converterGlobal(new BigDecimal("500"), G, KG);
            assertThat(valorEmKg).isEqualByComparingTo("0.5");
        }

        @Test
        void mlParaL_fatorDerivado() {
            BigDecimal valorEmL = service.converterGlobal(new BigDecimal("250"), ML, L);
            assertThat(valorEmL).isEqualByComparingTo("0.25");
        }
    }

    @Nested
    class Cenario4_EspecificaPorInsumo {
        @Test
        void caixaDeMussarelaPara5kg() {
            BigDecimal valorEmKg = service.converter(BigDecimal.ONE, CX, KG, MUSSARELA);
            assertThat(valorEmKg).isEqualByComparingTo("5");
        }
    }

    @Nested
    class Cenario5_EspecificaPrecedeGlobal {
        @Test
        void mesmoComGlobalDefinidaUsaEspecifica() {
            // Adiciona uma conversão GLOBAL CX→KG factor 99 que NÃO deve ser usada
            // para Mussarela porque há conversão específica (factor 5).
            repo.insertGlobal(CX, KG, new BigDecimal("99"));

            BigDecimal valorMussarela = service.converter(BigDecimal.ONE, CX, KG, MUSSARELA);
            assertThat(valorMussarela).isEqualByComparingTo("5"); // específica wins

            // Sem o insumo (ou com insumo diferente), cai na global → 99
            BigDecimal valorGlobal = service.converter(BigDecimal.ONE, CX, KG, BACALHAU);
            assertThat(valorGlobal).isEqualByComparingTo("99"); // global é fallback
        }
    }

    @Nested
    class Cenario6_EspecificaInversa {
        @Test
        void mussarelaKgParaCxDerivada() {
            // CX → KG = 5; KG → CX = 1/5 = 0.2
            BigDecimal cxs = service.converter(new BigDecimal("10"), KG, CX, MUSSARELA);
            assertThat(cxs).isEqualByComparingTo("2.0000000000"); // 10 / 5 = 2
        }
    }

    @Nested
    class Cenario7_SemConversao {
        @Test
        void kgParaMl_lancaExcecao() {
            assertThatThrownBy(() -> service.converterGlobal(BigDecimal.ONE, KG, ML))
                    .isInstanceOf(UnidadeNaoConversivelException.class)
                    .satisfies(ex -> {
                        UnidadeNaoConversivelException u = (UnidadeNaoConversivelException) ex;
                        assertThat(u.origemId()).isEqualTo(KG);
                        assertThat(u.destinoId()).isEqualTo(ML);
                        assertThat(u.insumoId()).isEmpty();
                    });
        }
    }

    @Nested
    class Cenario8_InsumoSemConversao {
        @Test
        void insumoSemEspecificaENemGlobalFallback() {
            // Bacalhau não tem CX→ML, e não há global CX→ML.
            assertThatThrownBy(() -> service.converter(BigDecimal.ONE, CX, ML, BACALHAU))
                    .isInstanceOf(UnidadeNaoConversivelException.class)
                    .satisfies(ex -> {
                        UnidadeNaoConversivelException u = (UnidadeNaoConversivelException) ex;
                        assertThat(u.insumoId()).contains(BACALHAU);
                    });
        }
    }

    @Nested
    class Cenario9_MultiplicacaoCorreta {
        @Test
        void converterMultiplicaPorFator() {
            BigDecimal r = service.converter(new BigDecimal("3"), KG, G, null);
            assertThat(r).isEqualByComparingTo("3000");
        }

        @Test
        void converterValorZeroRetornaZero() {
            BigDecimal r = service.converter(BigDecimal.ZERO, KG, G, null);
            assertThat(r).isEqualByComparingTo("0");
        }
    }

    /** In-memory repository fake. Suficiente porque todos os testes do conversor são unitários. */
    private static final class InMemoryConversaoUnidadeRepository implements ConversaoUnidadeRepository {

        private final Map<UUID, ConversaoUnidade> store = new HashMap<>();

        void insertGlobal(UnidadeMedidaId origem, UnidadeMedidaId destino, BigDecimal fator) {
            ConversaoUnidade c = ConversaoUnidade.global(origem, destino, fator, Instant.EPOCH);
            store.put(c.id(), c);
        }

        void insertPorInsumo(UnidadeMedidaId origem, UnidadeMedidaId destino, BigDecimal fator, InsumoId insumoId) {
            ConversaoUnidade c = ConversaoUnidade.porInsumo(origem, destino, fator, insumoId, Instant.EPOCH);
            store.put(c.id(), c);
        }

        @Override public ConversaoUnidade save(ConversaoUnidade c) {
            store.put(c.id(), c);
            return c;
        }

        @Override public Optional<ConversaoUnidade> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override public Optional<ConversaoUnidade> findByInsumoEOrigemDestino(InsumoId insumoId,
                                                                               UnidadeMedidaId origem,
                                                                               UnidadeMedidaId destino) {
            return store.values().stream()
                    .filter(c -> Objects.equals(c.insumoIdOpt().orElse(null), insumoId))
                    .filter(c -> c.origemId().equals(origem) && c.destinoId().equals(destino))
                    .findFirst();
        }

        @Override public Optional<ConversaoUnidade> findGlobalPorOrigemDestino(UnidadeMedidaId origem,
                                                                                UnidadeMedidaId destino) {
            return store.values().stream()
                    .filter(ConversaoUnidade::isGlobal)
                    .filter(c -> c.origemId().equals(origem) && c.destinoId().equals(destino))
                    .findFirst();
        }

        @Override public List<ConversaoUnidade> findAll(int page, int size) {
            return List.copyOf(store.values());
        }
    }
}
