package com.nonnas.reporting.interfaces.rest;

import com.nonnas.reporting.testsupport.AbstractReportingIntegrationTest;
import com.nonnas.reporting.testsupport.ReportingFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelatoriosControllerIT extends AbstractReportingIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ReportingFixtures fixtures;

    private UUID filial;
    private UUID kg;

    @BeforeEach
    void setUp() {
        fixtures.limparTudo();
        filial = UUID.randomUUID();
        kg = fixtures.idUnidadePadrao("KG");
    }

    @Test
    void getPosicao_retornaJsonComItensDoEstoque() throws Exception {
        UUID categoria = fixtures.criarCategoria("Categoria");
        UUID insumo = fixtures.criarInsumo(categoria, kg, "X-1", "Item X");
        UUID lote = fixtures.criarLote(insumo, "L1", LocalDate.now().plusMonths(2),
                new BigDecimal("10.00"));
        fixtures.criarSaldo(lote, filial, new BigDecimal("7"));

        mvc.perform(get("/api/v1/relatorios/posicao").param("filialId", filial.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].codigo").value("X-1"))
                .andExpect(jsonPath("$[0].saldoTotal").value(7));
    }

    @Test
    void getMovimentacoes_periodoInvalido_retorna400() throws Exception {
        mvc.perform(get("/api/v1/relatorios/movimentacoes")
                        .param("inicio", "2026-12-31T00:00:00Z")
                        .param("fim",    "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void postRefresh_retorna202() throws Exception {
        mvc.perform(post("/api/v1/relatorios/refresh"))
                .andExpect(status().isAccepted());
    }
}
