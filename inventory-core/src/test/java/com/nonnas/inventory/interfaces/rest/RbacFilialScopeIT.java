package com.nonnas.inventory.interfaces.rest;

import com.nonnas.inventory.testsupport.AbstractInventoryIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que os endpoints de inventory rejeitam (HTTP 403) chamadas em que
 * o usuário não-ADMIN tenta operar em filial diferente da sua. ADMIN segue
 * com visão global.
 *
 * <p>O {@code TestPrincipalFilter} (web-commons, perfil {@code test}) lê os
 * headers {@code X-Test-Perfil} / {@code X-Test-FilialId} e popula o
 * {@code SecurityContextHolder} antes do controller — isso permite simular
 * perfis sem precisar do JWT do identity.
 */
class RbacFilialScopeIT extends AbstractInventoryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private static final String H_PERFIL = "X-Test-Perfil";
    private static final String H_FILIAL = "X-Test-FilialId";

    @Test
    void saldo_naoAdmin_filialDiferente_retorna403() throws Exception {
        UUID minhaFilial = UUID.randomUUID();
        UUID outraFilial = UUID.randomUUID();

        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", UUID.randomUUID().toString())
                        .param("filialId", outraFilial.toString())
                        .header(H_PERFIL, "GERENTE")
                        .header(H_FILIAL, minhaFilial.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void saldo_naoAdmin_propriaFilial_passa() throws Exception {
        UUID minhaFilial = UUID.randomUUID();
        // insumo inexistente — esperamos 200 com saldo 0, não 403.
        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", UUID.randomUUID().toString())
                        .param("filialId", minhaFilial.toString())
                        .header(H_PERFIL, "OPERADOR")
                        .header(H_FILIAL, minhaFilial.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void saldo_admin_qualquerFilial_passa() throws Exception {
        // Admin sem filial vinculada deve enxergar qualquer filial.
        mvc.perform(get("/api/v1/saldos")
                        .param("insumoId", UUID.randomUUID().toString())
                        .param("filialId", UUID.randomUUID().toString())
                        .header(H_PERFIL, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void entradaManual_naoAdmin_filialDiferente_retorna403() throws Exception {
        UUID minhaFilial = UUID.randomUUID();
        UUID outraFilial = UUID.randomUUID();

        String body = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s","numeroLote":"L-RBAC",
                  "valorUnitario":1.0,"unidadeLancamentoId":"%s",
                  "quantidadeLancada":1,"quantidadeBase":1,"tipo":"ENTRADA_AJUSTE"
                }
                """.formatted(outraFilial, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/api/v1/movimentacoes/entrada-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(H_PERFIL, "OPERADOR")
                        .header(H_FILIAL, minhaFilial.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void saidaManual_naoAdmin_filialDiferente_retorna403() throws Exception {
        UUID minhaFilial = UUID.randomUUID();
        UUID outraFilial = UUID.randomUUID();

        String body = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s",
                  "unidadeLancamentoId":"%s","quantidadeBase":1,"tipo":"SAIDA_AJUSTE"
                }
                """.formatted(outraFilial, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/api/v1/movimentacoes/saida-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(H_PERFIL, "GERENTE")
                        .header(H_FILIAL, minhaFilial.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void consulta_tentaEntrada_retorna403_porRoleNaoPermitida() throws Exception {
        // CONSULTA não está em hasAnyRole('ADMIN','GERENTE','OPERADOR') do @PreAuthorize.
        // Mas method security só fica ativa nos ITs do identity, então aqui o bloqueio
        // vem do próprio assertCanAccess que valida o pertencimento — para mostrar
        // que CONSULTA precisa estar vinculado a UMA filial e não pode operar fora.
        UUID minhaFilial = UUID.randomUUID();
        UUID outraFilial = UUID.randomUUID();

        String body = """
                {
                  "filialId":"%s","usuarioId":"%s","insumoId":"%s","numeroLote":"L-RBAC-C",
                  "valorUnitario":1.0,"unidadeLancamentoId":"%s",
                  "quantidadeLancada":1,"quantidadeBase":1,"tipo":"ENTRADA_AJUSTE"
                }
                """.formatted(outraFilial, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/api/v1/movimentacoes/entrada-manual")
                        .contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(H_PERFIL, "CONSULTA")
                        .header(H_FILIAL, minhaFilial.toString()))
                .andExpect(status().isForbidden());
    }
}
