package com.nonnas.operations.application.carga;

import com.nonnas.inventory.application.ports.SaldoLoteRepository;
import com.nonnas.operations.testsupport.AbstractOperationsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CargaInicialIdempotenciaIT extends AbstractOperationsIntegrationTest {

    @Autowired private ProcessarCargaInicialUseCase processar;
    @Autowired private SaldoLoteRepository saldoRepo;

    @Test
    void mesmoHash_segundaExecucao_retornaMesmaCargaSemReprocessar() {
        UUID filial = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        String hash = "a".repeat(64);

        var cmd = new ProcessarCargaInicialUseCase.Comando(
                filial, hash, "carga.xlsx", solicitante,
                List.of(new ProcessarCargaInicialUseCase.ItemEntrada(
                        insumo, kg, "L-INI-1", new BigDecimal("100"), new BigDecimal("20"),
                        null, LocalDate.parse("2027-01-01"))));

        var primeira = processar.execute(cmd);
        var segunda = processar.execute(cmd);

        assertThat(segunda.id()).isEqualTo(primeira.id());

        // Saldo deve refletir UMA carga apenas, mesmo após segunda chamada
        assertThat(saldoRepo.somarPorInsumoEFilial(insumo, filial)).isEqualByComparingTo("100");
    }

    @Test
    void cargaInicial_geraLotesEAtualizaSaldo() {
        UUID filial = UUID.randomUUID();
        UUID solicitante = UUID.randomUUID();
        UUID insumoA = UUID.randomUUID();
        UUID insumoB = UUID.randomUUID();
        UUID kg = UUID.randomUUID();
        String hash = "b".repeat(64);

        var cmd = new ProcessarCargaInicialUseCase.Comando(
                filial, hash, "abertura.xlsx", solicitante,
                List.of(
                        new ProcessarCargaInicialUseCase.ItemEntrada(
                                insumoA, kg, "L-A", new BigDecimal("50"), new BigDecimal("32"),
                                null, LocalDate.parse("2026-08-01")),
                        new ProcessarCargaInicialUseCase.ItemEntrada(
                                insumoB, kg, "L-B", new BigDecimal("25"), new BigDecimal("18"),
                                null, LocalDate.parse("2026-12-01"))));

        var carga = processar.execute(cmd);

        assertThat(carga.registrosProcessados()).isEqualTo(2);
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoA, filial)).isEqualByComparingTo("50");
        assertThat(saldoRepo.somarPorInsumoEFilial(insumoB, filial)).isEqualByComparingTo("25");
    }
}
