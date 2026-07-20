
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.boot.api.configuration;

import br.com.sawcunhaos.organization.boot.infrastructure.ScosOrganizationTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração full-stack do agregado Configuration (shape C — só leitura + update).
 * Não há create nem delete (o delegate não os expõe). A chave é textual (o próprio nome do
 * enum {@code ConfigurationKey}); o {@code value} é sempre omitido/mascarado (null) nas leituras.
 *
 * <p>Seed (após reseed): EMPLOYEE_MIN_AGE(INTEGER), COMPANY_HIERARCHY_MAX_DEPTH(INTEGER),
 * LOGIN_INACTIVITY_TIMEOUT_DAYS(INTEGER), EMPLOYEE_EMAIL_DOMAIN(STRING), DEFAULT_COMPANY_ID(INTEGER).
 *
 * <p>404: chave que não corresponde a nenhum {@code ConfigurationKey} → SCOS_CONFIGURATION_001.
 */
public class ConfigurationControllerTest extends ScosOrganizationTestUtil {

    private static final String CONFIGURATIONS_URI = "/api/v1/configurations";
    private static final String KEYS_URI = "/api/v1/configurations/keys";
    private static final String SEEDED_KEY = "EMPLOYEE_MIN_AGE";
    private static final String INVALID_KEY = "INVALID_KEY_XYZ";

    private static final String DETAIL_NOT_FOUND = "A Key informada não existe no sistema.";
    private static final String TITLE_NOT_FOUND = "Recurso não encontrado";

    private static final String CODE_NOT_FOUND = "SCOS_CONFIGURATION_001";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET /v1/configurations/keys — catálogo de chaves
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/configurations/keys — token válido lista as chaves conhecidas (200)")
    void getKeys_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(KEYS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == '" + SEEDED_KEY + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/configurations/keys — sem token retorna 401")
    void getKeys_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(KEYS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/configurations/keys — token inválido (fora do JWKS) retorna 401")
    void getKeys_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get(KEYS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_INVALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/configurations/keys — sem permissão retorna 403 no padrão RFC 9457")
    void getKeys_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(KEYS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    // =====================================================================================
    // GET /v1/configurations — todas as configurações
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/configurations — token válido lista as configurações semeadas (200)")
    void getAll_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(CONFIGURATIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == '" + SEEDED_KEY + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/configurations — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(CONFIGURATIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/configurations — sem permissão retorna 403")
    void getAll_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(CONFIGURATIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // GET /v1/configurations/{id} — consulta por chave
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/configurations/{id} — chave existente retorna a configuração (200)")
    void getById_existingKey_returns200() throws Exception {
        mockMvc.perform(get(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_KEY))
                .andExpect(jsonPath("$.data.type").value("INTEGER"));
    }

    @Test
    @DisplayName("GET /v1/configurations/{id} — chave inexistente retorna 404 SCOS_CONFIGURATION_001")
    void getById_invalidKey_returns404() throws Exception {
        mockMvc.perform(get(CONFIGURATIONS_URI + "/{id}", INVALID_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/configurations/{id} — sem token retorna 401")
    void getById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/configurations/{id} — sem permissão retorna 403")
    void getById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/configurations/{id} — atualização de valor
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/configurations/{id} — valor válido atualiza a configuração (204)")
    void update_existingKey_returns204() throws Exception {
        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("21")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_KEY));
    }

    @Test
    @DisplayName("PUT /v1/configurations/{id} — chave inexistente retorna 404 SCOS_CONFIGURATION_001")
    void update_invalidKey_returns404() throws Exception {
        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", INVALID_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("10")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/configurations/{id} — sem value retorna 400 de validação")
    void update_missingValue_returns400() throws Exception {
        String body = """
                {
                }
                """;

        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/configurations/{id} — value vazio retorna 400 de validação")
    void update_blankValue_returns400() throws Exception {
        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/configurations/{id} — sem token retorna 401")
    void update_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("30")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/configurations/{id} — sem permissão retorna 403")
    void update_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(CONFIGURATIONS_URI + "/{id}", SEEDED_KEY)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("30")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String body(String value) {
        return """
                {
                  "value": "%s"
                }
                """.formatted(value);
    }
}
