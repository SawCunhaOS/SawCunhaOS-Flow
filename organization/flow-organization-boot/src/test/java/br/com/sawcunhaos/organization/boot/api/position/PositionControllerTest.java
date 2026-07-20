
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

package br.com.sawcunhaos.organization.boot.api.position;

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
 * Teste de integração full-stack do agregado Position (shape A — enable/disable) com as
 * particularidades do vínculo com Department e com Employee.
 *
 * <p>Seed (após reseed): Department id=1 (TI, ativo); Position id=1 (ADMIN_SISTEMA, ativo,
 * department_id=1) com Employee id=1 ATIVO vinculado. Logo:
 * <ul>
 *   <li>disable do id=1 → 422 SCOS_POSITION_003 (possui empregado ativo)</li>
 *   <li>enable do id=1 (ativo) → 422 SCOS_POSITION_004</li>
 *   <li>criar/atualizar com departmentId inexistente → 404 SCOS_DEPARTMENT_001</li>
 * </ul>
 * Unicidade de {@code code} é global (não por department). Idempotência (Redis) keyed em
 * {@code code} → cada POST bem-sucedido usa um {@code code} único.
 */
public class PositionControllerTest extends ScosOrganizationTestUtil {

    private static final String POSITIONS_URI = "/api/v1/positions";
    private static final long SEEDED_ID = 1L;
    private static final String SEEDED_CODE = "ADMIN_SISTEMA";
    private static final long SEEDED_DEPARTMENT_ID = 1L;
    private static final long NONEXISTENT_ID = 999_999L;

    private static final String DETAIL_NOT_FOUND = "O cargo informado não existe.";
    private static final String DETAIL_CODE_CONFLICT = "Já existe um cargo cadastrado com esse código.";
    private static final String DETAIL_HAS_ACTIVE_EMPLOYEES =
            "Não foi possível inativar o cargo, pois ele possui vínculos com empregados ativos.";
    private static final String DETAIL_ALREADY_ACTIVE = "O cargo informado já está ativo.";
    private static final String DETAIL_ALREADY_INACTIVE = "O cargo informado já está inativo.";
    private static final String DETAIL_DEPARTMENT_NOT_FOUND = "O departamento informado não existe.";

    private static final String TITLE_NOT_FOUND = "Recurso não encontrado";
    private static final String TITLE_CONFLICT = "Conflito de dados";
    private static final String TITLE_BUSINESS_RULE = "Regra de negócio violada";

    private static final String CODE_NOT_FOUND = "SCOS_POSITION_001";
    private static final String CODE_CONFLICT = "SCOS_POSITION_002";
    private static final String CODE_ACTIVE_EMPLOYEES = "SCOS_POSITION_003";
    private static final String CODE_ALREADY_ACTIVE = "SCOS_POSITION_004";
    private static final String CODE_ALREADY_INACTIVE = "SCOS_POSITION_005";
    private static final String CODE_DEPARTMENT_NOT_FOUND = "SCOS_DEPARTMENT_001";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET /v1/positions — listagem
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/positions — filtro departmentId lista os cargos do departamento (200)")
    void getAll_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("departmentId", String.valueOf(SEEDED_DEPARTMENT_ID))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/positions — filtro active=false não retorna o seed ativo (200)")
    void getAll_filterActiveFalse_doesNotReturnActive() throws Exception {
        mockMvc.perform(get(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("departmentId", String.valueOf(SEEDED_DEPARTMENT_ID))
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/positions — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("departmentId", String.valueOf(SEEDED_DEPARTMENT_ID))
                        .param("active", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/positions — token inválido (fora do JWKS) retorna 401")
    void getAll_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_INVALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("departmentId", String.valueOf(SEEDED_DEPARTMENT_ID))
                        .param("active", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/positions — sem permissão retorna 403 no padrão RFC 9457")
    void getAll_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("departmentId", String.valueOf(SEEDED_DEPARTMENT_ID))
                        .param("active", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.instance").exists());
    }

    // =====================================================================================
    // GET /v1/positions/{id} — consulta por id
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/positions/{id} — retorna o cargo do seed com o departamento (200)")
    void getById_withValidToken_returnsSeeded() throws Exception {
        mockMvc.perform(get(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_ID))
                .andExpect(jsonPath("$.data.code").value(SEEDED_CODE))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.department.id").value(SEEDED_DEPARTMENT_ID));
    }

    @Test
    @DisplayName("GET /v1/positions/{id} — id inexistente retorna 404 SCOS_POSITION_001")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(POSITIONS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/positions/{id} — sem token retorna 401")
    void getById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/positions/{id} — sem permissão retorna 403")
    void getById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST /v1/positions — criação
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/positions — payload válido cria e retorna o id (201)")
    void create_withValidToken_returns201() throws Exception {
        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DEV_JUNIOR", "Desenvolvedor Júnior", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/positions — code já existente retorna 409 SCOS_POSITION_002")
    void create_duplicateCode_returns409() throws Exception {
        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(SEEDED_CODE, "Duplicado", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.title").value(TITLE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/positions — departmentId inexistente retorna 404 SCOS_DEPARTMENT_001")
    void create_departmentNotFound_returns404() throws Exception {
        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DEP_NOPE1", "Departamento inexistente", NONEXISTENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_DEPARTMENT_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_DEPARTMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/positions — sem code retorna 400 de validação")
    void create_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "departmentId": %d
                }
                """.formatted(SEEDED_DEPARTMENT_ID);

        mockMvc.perform(post(POSITIONS_URI)
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
    @DisplayName("POST /v1/positions — sem departmentId retorna 400 de validação")
    void create_missingDepartmentId_returns400() throws Exception {
        String body = """
                {
                  "code": "NODEP01",
                  "description": "Sem departmentId"
                }
                """;

        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/positions — sem token retorna 401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOTOKEN01", "Sem token", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/positions — sem permissão retorna 403")
    void create_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPERM01", "Sem permissão", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("POST /v1/positions — duas chamadas idênticas são idempotentes (Redis)")
    void create_sameRequestTwice_isIdempotent() throws Exception {
        String body = body("IDEMP01", "Idempotente", SEEDED_DEPARTMENT_ID);

        String first = mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(POSITIONS_URI)
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
    // PUT /v1/positions/{id} — atualização
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/positions/{id} — payload válido atualiza (204)")
    void update_withValidToken_returns204() throws Exception {
        long id = create("UPD_BASE1", "A ser atualizado", SEEDED_DEPARTMENT_ID);

        mockMvc.perform(put(POSITIONS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("UPD_NEW01", "Atualizado", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(POSITIONS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("UPD_NEW01"))
                .andExpect(jsonPath("$.data.description").value("Atualizado"));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id} — code de outro cargo retorna 409")
    void update_duplicateCode_returns409() throws Exception {
        long id = create("UPD_DUP01", "A renomear", SEEDED_DEPARTMENT_ID);

        mockMvc.perform(put(POSITIONS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(SEEDED_CODE, "Colidindo", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id} — id inexistente retorna 404")
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPE01", "Inexistente", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id} — sem code retorna 400 de validação")
    void update_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "departmentId": %d
                }
                """.formatted(SEEDED_DEPARTMENT_ID);

        mockMvc.perform(put(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id} — sem token retorna 401")
    void update_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOTOK02", "Sem token", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id} — sem permissão retorna 403")
    void update_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(POSITIONS_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPERM02", "Sem permissão", SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/positions/{id}/enable — reativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/positions/{id}/enable — cargo inativo é reativado (204)")
    void enable_inactive_returns204() throws Exception {
        long id = create("ENA_POS01", "Para reativar", SEEDED_DEPARTMENT_ID);
        disable(id);

        mockMvc.perform(put(POSITIONS_URI + "/{id}/enable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/enable — cargo já ativo retorna 422 SCOS_POSITION_004")
    void enable_alreadyActive_returns422() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_ACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/enable — id inexistente retorna 404")
    void enable_notFound_returns404() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/enable — sem token retorna 401")
    void enable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/enable — sem permissão retorna 403")
    void enable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(POSITIONS_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/positions/{id}/disable — inativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — cargo sem empregados ativos é inativado (204)")
    void disable_withoutActiveEmployees_returns204() throws Exception {
        long id = create("DIS_POS01", "Sem empregados", SEEDED_DEPARTMENT_ID);

        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — cargo com empregado ativo retorna 422 SCOS_POSITION_003")
    void disable_withActiveEmployees_returns422() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVE_EMPLOYEES))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_HAS_ACTIVE_EMPLOYEES));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — cargo já inativo retorna 422 SCOS_POSITION_005")
    void disable_alreadyInactive_returns422() throws Exception {
        long id = create("DIS_POS02", "Inativado duas vezes", SEEDED_DEPARTMENT_ID);
        disable(id);

        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_INACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — id inexistente retorna 404")
    void disable_notFound_returns404() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — sem token retorna 401")
    void disable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/positions/{id}/disable — sem permissão retorna 403")
    void disable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String body(String code, String description, long departmentId) {
        return """
                {
                  "code": "%s",
                  "description": "%s",
                  "departmentId": %d
                }
                """.formatted(code, description, departmentId);
    }

    private long create(String code, String description, long departmentId) throws Exception {
        String response = mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code, description, departmentId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private void disable(long id) throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }
}
