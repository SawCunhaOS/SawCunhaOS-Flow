
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

package br.com.sawcunhaos.organization.boot.api.reason;

import br.com.sawcunhaos.organization.boot.infrastructure.ScosOrganizationTestUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração full-stack do agregado ReasonDisable (shape A — enable/disable).
 * Unicidade por {@code (code, entityType)}; entityType ∈ COMPANY/EMPLOYEE/LOGIN.
 *
 * <p>Seed (após reseed): id=1 COMPANY/UNDER_AUDIT, id=2 EMPLOYEE/UNDER_AUDIT,
 * id=3 LOGIN/INVALID_LOGIN_ATTEMPTS (todos ativos).
 */
public class ReasonDisableControllerTest extends ScosOrganizationTestUtil {

    private static final String URI = "/api/v1/reason-disable";
    private static final long SEEDED_ID = 1L;
    private static final String SEEDED_CODE = "UNDER_AUDIT";
    private static final String SEEDED_ENTITY_TYPE = "COMPANY";
    // Registro semeado JÁ INATIVO (ARCHIVED_REASON/COMPANY) — ver setsup_database.sql.
    private static final long SEEDED_INACTIVE_ID = 4L;
    private static final long NONEXISTENT_ID = 999_999L;

    private static final String DETAIL_NOT_FOUND = "O motivo de bloqueio informado não existe.";
    private static final String DETAIL_CODE_CONFLICT = "Já existe um motivo de bloqueio cadastrado com esse código.";
    private static final String DETAIL_ALREADY_ACTIVE = "O motivo de bloqueio informado já está ativo.";
    private static final String DETAIL_ALREADY_INACTIVE = "O motivo de bloqueio informado já está inativo.";

    private static final String TITLE_NOT_FOUND = "Recurso não encontrado";
    private static final String TITLE_CONFLICT = "Conflito de dados";
    private static final String TITLE_BUSINESS_RULE = "Regra de negócio violada";

    private static final String CODE_NOT_FOUND = "SCOS_REASON_DISABLE_001";
    private static final String CODE_CONFLICT = "SCOS_REASON_DISABLE_002";
    private static final String CODE_ALREADY_ACTIVE = "SCOS_REASON_DISABLE_003";
    private static final String CODE_ALREADY_INACTIVE = "SCOS_REASON_DISABLE_004";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET — listagem
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/reason-disable — token válido lista os motivos semeados (200)")
    void getAll_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/reason-disable — filtro entityType=EMPLOYEE não retorna os de COMPANY (200)")
    void getAll_filterEntityType_returnsOnlyMatching() throws Exception {
        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("entityType", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.entityType == 'COMPANY')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/reason-disable — filtro active=false não retorna o seed ativo (200)")
    void getAll_filterActiveFalse_doesNotReturnActive() throws Exception {
        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/reason-disable — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/reason-disable — token inválido (fora do JWKS) retorna 401")
    void getAll_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_INVALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/reason-disable — sem permissão retorna 403 no padrão RFC 9457")
    void getAll_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    // =====================================================================================
    // GET /{id}
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/reason-disable/{id} — retorna o motivo do seed (200)")
    void getById_withValidToken_returnsSeeded() throws Exception {
        mockMvc.perform(get(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_ID))
                .andExpect(jsonPath("$.data.code").value(SEEDED_CODE))
                .andExpect(jsonPath("$.data.entityType").value(SEEDED_ENTITY_TYPE))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /v1/reason-disable/{id} — id inexistente retorna 404 SCOS_REASON_DISABLE_001")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/reason-disable/{id} — sem token retorna 401")
    void getById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/reason-disable/{id} — sem permissão retorna 403")
    void getById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/reason-disable — payload válido cria e retorna o id (201)")
    void create_withValidToken_returns201() throws Exception {
        mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NEW1", "Novo motivo", "COMPANY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/reason-disable — code/entityType já existente retorna 409")
    void create_duplicateCode_returns409() throws Exception {
        mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(SEEDED_CODE, "Duplicado", SEEDED_ENTITY_TYPE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.title").value(TITLE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/reason-disable — sem code retorna 400 de validação")
    void create_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "entityType": "COMPANY"
                }
                """;

        mockMvc.perform(post(URI)
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
    @DisplayName("POST /v1/reason-disable — code vazio retorna 400 de validação")
    void create_blankCode_returns400() throws Exception {
        mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "Code vazio", "COMPANY")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/reason-disable — sem token retorna 401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NOTOK", "Sem token", "COMPANY")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/reason-disable — sem permissão retorna 403")
    void create_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NOPERM", "Sem permissão", "COMPANY")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("POST /v1/reason-disable — duas chamadas idênticas são idempotentes (Redis)")
    void create_sameRequestTwice_isIdempotent() throws Exception {
        String body = body("RDIS_IDEMP", "Idempotente", "COMPANY");

        String first = mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertEquals(
                ((Number) JsonPath.read(first, "$.data.id")).longValue(),
                ((Number) JsonPath.read(second, "$.data.id")).longValue());
    }

    // =====================================================================================
    // PUT /{id}
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — payload válido atualiza (204)")
    void update_withValidToken_returns204() throws Exception {
        mockMvc.perform(put(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_UPD1", "Atualizado", "COMPANY")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("RDIS_UPD1"))
                .andExpect(jsonPath("$.data.description").value("Atualizado"));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — code/entityType de outro registro retorna 409")
    void update_duplicateCode_returns409() throws Exception {
        mockMvc.perform(put(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("UNDER_AUDIT", "Colidindo", "EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — id inexistente retorna 404")
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put(URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NOPE", "Inexistente", "COMPANY")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — sem code retorna 400 de validação")
    void update_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "entityType": "COMPANY"
                }
                """;

        mockMvc.perform(put(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — sem token retorna 401")
    void update_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NOTOK2", "Sem token", "COMPANY")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id} — sem permissão retorna 403")
    void update_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("RDIS_NOPERM2", "Sem permissão", "COMPANY")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // enable / disable
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/enable — registro inativo é reativado (204)")
    void enable_inactive_returns204() throws Exception {
        long id = create("RDIS_ENA", "Para reativar", "COMPANY");
        disable(id);

        mockMvc.perform(put(URI + "/{id}/enable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/enable — já ativo retorna 422 SCOS_REASON_DISABLE_003")
    void enable_alreadyActive_returns422() throws Exception {
        mockMvc.perform(put(URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_ACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/enable — id inexistente retorna 404")
    void enable_notFound_returns404() throws Exception {
        mockMvc.perform(put(URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/enable — sem token retorna 401")
    void enable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/enable — sem permissão retorna 403")
    void enable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/disable — registro ativo é inativado (204)")
    void disable_active_returns204() throws Exception {
        long id = create("RDIS_DIS", "Para inativar", "COMPANY");

        mockMvc.perform(put(URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/disable — já inativo retorna 422 SCOS_REASON_DISABLE_004")
    void disable_alreadyInactive_returns422() throws Exception {
        // Usa o registro semeado JÁ INATIVO: o disable é @JdempotentResource keyed pelo id, então
        // re-disable de um id recém-criado devolveria a resposta cacheada (204) sem exercitar a
        // regra _004. Partindo de um id inativo do seed, o disable executa e valida "já inativo".
        mockMvc.perform(put(URI + "/{id}/disable", SEEDED_INACTIVE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_INACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/disable — id inexistente retorna 404")
    void disable_notFound_returns404() throws Exception {
        mockMvc.perform(put(URI + "/{id}/disable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/disable — sem token retorna 401")
    void disable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/reason-disable/{id}/disable — sem permissão retorna 403")
    void disable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String body(String code, String description, String entityType) {
        return """
                {
                  "code": "%s",
                  "description": "%s",
                  "entityType": "%s"
                }
                """.formatted(code, description, entityType);
    }

    private long create(String code, String description, String entityType) throws Exception {
        String response = mockMvc.perform(post(URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code, description, entityType)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private void disable(long id) throws Exception {
        mockMvc.perform(put(URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }
}
