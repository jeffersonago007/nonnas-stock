package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.infrastructure.security.TotpService;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre a jornada 2FA TOTP (master doc T16): setup → confirmar com código
 * TOTP válido → recebe backup codes em texto puro uma única vez. O fluxo
 * de gating (ADMIN sem 2FA não acessa endpoints sensíveis) fica para T17
 * — aqui validamos que a infra básica funciona ponta-a-ponta.
 */
class TwoFaIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private TotpService totp;

    @Test
    void setupEConfirmarGeram8BackupCodesAtivamSegundoFator() throws Exception {
        String token = loginComoAdmin();

        // Setup — gera secret + URI otpauth
        MvcResult setupResp = mvc.perform(post("/api/v1/auth/2fa/setup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretBase32").exists())
                .andExpect(jsonPath("$.otpauthUri").exists())
                .andReturn();
        JsonNode setup = json.readTree(setupResp.getResponse().getContentAsString());
        String secret = setup.get("secretBase32").asText();
        String uri = setup.get("otpauthUri").asText();
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("issuer=Nonnas+Stock");

        // Confirma com código TOTP calculado a partir do secret
        String codigo = computarCodigoAtual(secret);
        mvc.perform(post("/api/v1/auth/2fa/confirmar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backupCodes.length()").value(8));
    }

    @Test
    void confirmarComCodigoInvalidoRetorna401() throws Exception {
        String token = loginComoAdmin();

        mvc.perform(post("/api/v1/auth/2fa/setup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/2fa/confirmar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String computarCodigoAtual(String secret) {
        return totp.computeCurrentCode(secret);
    }

    private String loginComoAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
