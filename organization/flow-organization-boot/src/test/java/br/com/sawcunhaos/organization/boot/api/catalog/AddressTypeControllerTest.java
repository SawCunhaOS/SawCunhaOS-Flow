
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

package br.com.sawcunhaos.organization.boot.api.catalog;

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
 * Teste de integração full-stack do agregado AddressType (shape A — enable/disable).
 *
 * <p>Replica a matriz do {@code DepartmentControllerTest} sobre a infra singleton
 * ({@link ScosOrganizationTestUtil}): MockMvc → filtros JWT → {@code @PreAuthorize} →
 * delegate → use case → domain → Postgres real. Isolamento por método via {@code @Sql}
 * (reseed BEFORE / TRUNCATE RESTART IDENTITY AFTER). O cache de idempotência (Redis) NÃO
 * reseta entre métodos, então cada POST bem-sucedido usa um {@code code} único.
 *
 * <p>Dados do seed usados (após reseed, IDs determinísticos):
 * <ul>
 *   <li>id=1 HOME/EMPLOYEE, id=2 WORK/EMPLOYEE (ativos)</li>
 *   <li>id=3 COMMERCIAL/COMPANY, id=4 BILLING/COMPANY, id=5 BRANCH/COMPANY (ativos)</li>
 * </ul>
 *
 * <p>Unicidade é por {@code (code, entityType)} — o 409 usa HOME/EMPLOYEE do seed.
 */
public class AddressTypeControllerTest extends ScosOrganizationTestUtil {

    private static final String ADDRESS_TYPES_URI = "/api/v1/address-types";
    private static final long SEEDED_ID = 1L;
    private static final String SEEDED_CODE = "HOME";
    private static final String SEEDED_ENTITY_TYPE = "EMPLOYEE";
    // Registro semeado JÁ INATIVO (ARCHIVED/EMPLOYEE) — ver setsup_database.sql.
    private static final long SEEDED_INACTIVE_ID = 6L;
    private static final long NONEXISTENT_ID = 999_999L;

    private static final String DETAIL_NOT_FOUND = "O tipo de endereço informado não existe.";
    private static final String DETAIL_CODE_CONFLICT = "Já existe um tipo de endereço cadastrado com esse código.";
    private static final String DETAIL_ALREADY_ACTIVE = "O tipo de endereço informado já está ativo.";
    private static final String DETAIL_ALREADY_INACTIVE = "O tipo de endereço informado já está inativo.";

    private static final String TITLE_NOT_FOUND = "Recurso não encontrado";
    private static final String TITLE_CONFLICT = "Conflito de dados";
    private static final String TITLE_BUSINESS_RULE = "Regra de negócio violada";

    private static final String CODE_NOT_FOUND = "SCOS_ADDRESS_TYPE_001";
    private static final String CODE_CONFLICT = "SCOS_ADDRESS_TYPE_002";
    private static final String CODE_ALREADY_ACTIVE = "SCOS_ADDRESS_TYPE_003";
    private static final String CODE_ALREADY_INACTIVE = "SCOS_ADDRESS_TYPE_004";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET /v1/address-types — listagem
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/address-types — token válido lista os tipos semeados (200)")
    void getAll_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/address-types — filtro entityType=COMPANY retorna só os da empresa (200)")
    void getAll_filterEntityTypeCompany_returnsOnlyCompany() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("entityType", "COMPANY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'COMMERCIAL')]").exists())
                .andExpect(jsonPath("$.data[?(@.entityType == 'EMPLOYEE')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/address-types — filtro active=true não retorna inativos (200)")
    void getAll_filterActiveTrue_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.active == false)]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/address-types — filtro active=false não retorna o seed ativo (200)")
    void getAll_filterActiveFalse_doesNotReturnActive() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/address-types — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/address-types — token inválido (fora do JWKS) retorna 401")
    void getAll_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_INVALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/address-types — sem permissão retorna 403 no padrão RFC 9457")
    void getAll_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(ADDRESS_TYPES_URI)
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
    // GET /v1/address-types/{id} — consulta por id
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/address-types/{id} — retorna o tipo do seed (200)")
    void getById_withValidToken_returnsSeeded() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_ID))
                .andExpect(jsonPath("$.data.code").value(SEEDED_CODE))
                .andExpect(jsonPath("$.data.entityType").value(SEEDED_ENTITY_TYPE))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /v1/address-types/{id} — id inexistente retorna 404 SCOS_ADDRESS_TYPE_001")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/address-types/{id} — sem token retorna 401")
    void getById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/address-types/{id} — sem permissão retorna 403")
    void getById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST /v1/address-types — criação
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/address-types — payload válido cria e retorna o id (201)")
    void create_withValidToken_returns201() throws Exception {
        mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("HOMEOFF01", "Home office", "EMPLOYEE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/address-types — code/entityType já existente retorna 409 SCOS_ADDRESS_TYPE_002")
    void create_duplicateCode_returns409() throws Exception {
        mockMvc.perform(post(ADDRESS_TYPES_URI)
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
    @DisplayName("POST /v1/address-types — sem code retorna 400 de validação")
    void create_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "entityType": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post(ADDRESS_TYPES_URI)
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
    @DisplayName("POST /v1/address-types — code vazio retorna 400 de validação")
    void create_blankCode_returns400() throws Exception {
        mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "Code vazio", "EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/address-types — sem description retorna 400 de validação")
    void create_missingDescription_returns400() throws Exception {
        String body = """
                {
                  "code": "NODESC01",
                  "entityType": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/address-types — sem token retorna 401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOTOKEN01", "Sem token", "EMPLOYEE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/address-types — sem permissão retorna 403")
    void create_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPERM01", "Sem permissão", "EMPLOYEE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("POST /v1/address-types — duas chamadas idênticas são idempotentes (Redis)")
    void create_sameRequestTwice_isIdempotent() throws Exception {
        String body = body("IDEMP01", "Idempotente", "EMPLOYEE");

        String first = mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(ADDRESS_TYPES_URI)
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
    // PUT /v1/address-types/{id} — atualização
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/address-types/{id} — payload válido atualiza (204)")
    void update_withValidToken_returns204() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("HOMEUPD01", "Residencial atualizado", "EMPLOYEE")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("HOMEUPD01"))
                .andExpect(jsonPath("$.data.description").value("Residencial atualizado"));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id} — code/entityType de outro registro retorna 409")
    void update_duplicateCode_returns409() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("WORK", "Colidindo", "EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id} — id inexistente retorna 404")
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPE01", "Inexistente", "EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id} — sem code retorna 400 de validação")
    void update_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code",
                  "entityType": "EMPLOYEE"
                }
                """;

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id} — sem token retorna 401")
    void update_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOTOK02", "Sem token", "EMPLOYEE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id} — sem permissão retorna 403")
    void update_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NOPERM02", "Sem permissão", "EMPLOYEE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/address-types/{id}/enable — reativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/address-types/{id}/enable — registro inativo é reativado (204)")
    void enable_inactive_returns204() throws Exception {
        long id = create("ENA01", "Para reativar", "EMPLOYEE");
        disable(id);

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/enable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/enable — já ativo retorna 422 SCOS_ADDRESS_TYPE_003")
    void enable_alreadyActive_returns422() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_ACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/enable — id inexistente retorna 404")
    void enable_notFound_returns404() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/enable — sem token retorna 401")
    void enable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/enable — sem permissão retorna 403")
    void enable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/enable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/address-types/{id}/disable — inativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/address-types/{id}/disable — registro ativo é inativado (204)")
    void disable_active_returns204() throws Exception {
        long id = create("DIS01", "Para inativar", "EMPLOYEE");

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/disable — já inativo retorna 422 SCOS_ADDRESS_TYPE_004")
    void disable_alreadyInactive_returns422() throws Exception {
        // Usa o registro semeado JÁ INATIVO: o disable é @JdempotentResource keyed pelo id, então
        // re-disable de um id recém-criado devolveria a resposta cacheada (204) sem exercitar a
        // regra _004. Partindo de um id inativo do seed, o disable executa e valida "já inativo".
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", SEEDED_INACTIVE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_INACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/disable — id inexistente retorna 404")
    void disable_notFound_returns404() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/disable — sem token retorna 401")
    void disable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/address-types/{id}/disable — sem permissão retorna 403")
    void disable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", SEEDED_ID)
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
        String response = mockMvc.perform(post(ADDRESS_TYPES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(code, description, entityType)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private void disable(long id) throws Exception {
        mockMvc.perform(put(ADDRESS_TYPES_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }
}
