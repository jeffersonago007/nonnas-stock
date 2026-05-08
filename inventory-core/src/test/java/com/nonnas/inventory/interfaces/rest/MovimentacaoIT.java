package com.nonnas.inventory.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.inventory.testsupport.AbstractInventoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT cobrindo o fluxo end-to-end: entrada manual → saldo materializado
 * incrementa → saída via FEFO consome lotes na ordem certa → saldo desce.
 */
class MovimentacaoIT extends AbstractInventoryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void entrada_atualizaSaldoMaterializado() throws Exception {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidadeKg = UUID.randomUUID();

        String entrada = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s","numeroLote":"L-001",
                  "valorUnitario":25.0,"unidadeLancamentoId":"%s",
                  "quantidadeLancada":10,"quantidadeBase":10,"tipo":"ENTRADA_AJUSTE"
                }
                """.formatted(filial, usuario, insumo, unidadeKg);

        mvc.perform(post("/api/v1/movimentacoes/entrada-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA_AJUSTE"))
                .andExpect(jsonPath("$.itens[0].quantidadeBase").value(10));

        // Saldo materializado = 10
        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", insumo.toString())
                        .param("filialId", filial.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeBase").value(10));
    }

    @Test
    void duasEntradasESaida_FefoConsomeValidadeMaisProximaPrimeiro() throws Exception {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        // L1 vence em 2026-06-01 (mais próximo)
        criarEntrada(filial, usuario, insumo, unidade, "L-001", "2026-06-01", 5);
        // L2 vence em 2026-12-01 (mais distante)
        criarEntrada(filial, usuario, insumo, unidade, "L-002", "2026-12-01", 10);

        // Saída de 7 → consome 5 do L1 (vence primeiro) + 2 do L2
        String saida = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s",
                  "unidadeLancamentoId":"%s","quantidadeBase":7,"tipo":"SAIDA_VENDA"
                }
                """.formatted(filial, usuario, insumo, unidade);

        var res = mvc.perform(post("/api/v1/movimentacoes/saida-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(saida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].quantidadeBase").value(5))
                .andExpect(jsonPath("$.itens[1].quantidadeBase").value(2))
                .andExpect(jsonPath("$.gerouNegativo").value(false))
                .andReturn();

        // Saldo final = 15 - 7 = 8
        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", insumo.toString())
                        .param("filialId", filial.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeBase").value(8));
    }

    @Test
    void saidaSemLote_retorna422() throws Exception {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        String saida = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s",
                  "unidadeLancamentoId":"%s","quantidadeBase":1,"tipo":"SAIDA_VENDA"
                }
                """.formatted(filial, usuario, insumo, unidade);

        mvc.perform(post("/api/v1/movimentacoes/saida-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(saida))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void saidaMaiorQueSaldo_geraNegativoEFlag() throws Exception {
        UUID filial = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();

        criarEntrada(filial, usuario, insumo, unidade, "L-x", "2026-12-01", 3);

        String saida = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s",
                  "unidadeLancamentoId":"%s","quantidadeBase":10,"tipo":"SAIDA_VENDA"
                }
                """.formatted(filial, usuario, insumo, unidade);

        mvc.perform(post("/api/v1/movimentacoes/saida-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(saida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gerouNegativo").value(true));

        // Saldo agora = 3 - 10 = -7
        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", insumo.toString())
                        .param("filialId", filial.toString()))
                .andExpect(jsonPath("$.quantidadeBase").value(-7));
    }

    @Test
    void entradaInvalidaSemCamposObrigatorios_retorna400() throws Exception {
        mvc.perform(post("/api/v1/movimentacoes/entrada-manual")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void criarEntrada(UUID filial, UUID usuario, UUID insumo, UUID unidade,
                              String numeroLote, String validade, int qtd) throws Exception {
        String body = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s","numeroLote":"%s",
                  "dataValidade":"%s","valorUnitario":10.0,
                  "unidadeLancamentoId":"%s","quantidadeLancada":%d,"quantidadeBase":%d,
                  "tipo":"ENTRADA_AJUSTE"
                }
                """.formatted(filial, usuario, insumo, numeroLote, validade, unidade, qtd, qtd);
        mvc.perform(post("/api/v1/movimentacoes/entrada-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
