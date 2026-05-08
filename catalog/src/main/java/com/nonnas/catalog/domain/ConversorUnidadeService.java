package com.nonnas.catalog.domain;

import com.nonnas.catalog.application.ports.ConversaoUnidadeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolve conversões entre unidades de medida. Implementa o algoritmo
 * descrito em PROMPT_CLAUDE_CODE.md seção 5.1:
 *
 * <ol>
 *   <li>Se origem == destino, fator = 1 (sem conversão).</li>
 *   <li>Conversão específica do insumo (direta).</li>
 *   <li>Conversão específica do insumo (inversa, derivada como 1/fator).</li>
 *   <li>Conversão global (direta).</li>
 *   <li>Conversão global (inversa, derivada).</li>
 *   <li>Caso contrário, lança {@link UnidadeNaoConversivelException}.</li>
 * </ol>
 *
 * <p>Domain service: <em>sem dependência de Spring</em>. Recebe o port
 * {@link ConversaoUnidadeRepository} via construtor. A injeção como
 * {@code @Bean} acontece numa configuração de infrastructure.
 */
public final class ConversorUnidadeService {

    /**
     * Escala usada quando derivamos a conversão inversa (1/fator). Suficiente
     * para preservar precisão em fatores fracionários comuns no domínio.
     */
    public static final int SCALE_INVERSO = 10;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final ConversaoUnidadeRepository repository;

    public ConversorUnidadeService(ConversaoUnidadeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Converte o {@code valor} de {@code origem} para {@code destino},
     * usando a conversão específica do insumo se {@code insumoId != null},
     * caindo de volta para conversão global.
     *
     * @throws UnidadeNaoConversivelException se nenhuma conversão estiver definida
     */
    public BigDecimal converter(BigDecimal valor, UnidadeMedidaId origem, UnidadeMedidaId destino,
                                InsumoId insumoId) {
        Objects.requireNonNull(valor, "valor");
        Objects.requireNonNull(origem, "origem");
        Objects.requireNonNull(destino, "destino");

        BigDecimal fator = resolverFator(origem, destino, insumoId);
        return valor.multiply(fator);
    }

    /**
     * Versão sem insumo: usa apenas conversão global. Equivalente a
     * {@code converter(valor, origem, destino, null)}.
     */
    public BigDecimal converterGlobal(BigDecimal valor, UnidadeMedidaId origem, UnidadeMedidaId destino) {
        return converter(valor, origem, destino, null);
    }

    /**
     * @return fator pelo qual multiplicar o valor em {@code origem} para obter o equivalente em {@code destino}.
     */
    public BigDecimal resolverFator(UnidadeMedidaId origem, UnidadeMedidaId destino, InsumoId insumoId) {
        if (origem.equals(destino)) {
            return BigDecimal.ONE;
        }

        // 1. Conversão específica do insumo, direta
        if (insumoId != null) {
            Optional<ConversaoUnidade> direta = repository.findByInsumoEOrigemDestino(insumoId, origem, destino);
            if (direta.isPresent()) {
                return direta.get().fator();
            }
            // 2. Conversão específica do insumo, inversa
            Optional<ConversaoUnidade> inversa = repository.findByInsumoEOrigemDestino(insumoId, destino, origem);
            if (inversa.isPresent()) {
                return inverter(inversa.get().fator());
            }
        }

        // 3. Conversão global, direta
        Optional<ConversaoUnidade> globalDireta = repository.findGlobalPorOrigemDestino(origem, destino);
        if (globalDireta.isPresent()) {
            return globalDireta.get().fator();
        }

        // 4. Conversão global, inversa
        Optional<ConversaoUnidade> globalInversa = repository.findGlobalPorOrigemDestino(destino, origem);
        if (globalInversa.isPresent()) {
            return inverter(globalInversa.get().fator());
        }

        throw new UnidadeNaoConversivelException(origem, destino, insumoId);
    }

    private static BigDecimal inverter(BigDecimal fator) {
        return BigDecimal.ONE.divide(fator, SCALE_INVERSO, ROUNDING);
    }
}
