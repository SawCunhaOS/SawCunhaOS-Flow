
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

package br.com.sawcunhaos.organization.boot.api.company;

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
 * Teste de integração full-stack do cadastro da Empresa (UC-001..005).
 *
 * <p>Seed (após reseed): COMPANY id=1 ('SawCunhaOS Tecnologia LTDA', ACTIVE, matriz). REASON_ACTIVATE
 * id=1 (COMPANY/ativo), id=2 (EMPLOYEE), id=5 (COMPANY/inativo). COMPANY_HIERARCHY_MAX_DEPTH=10.
 *
 * <p>A idempotência (Redis) do POST/PUT é keyed em {@code taxIdentifier} e o cache sobrevive entre
 * métodos (o reseed recria só o banco). Por isso cada teste que persiste usa um CNPJ válido único —
 * mesmo padrão da suíte de LegalNature (code único por teste). O 409 de unicidade é exercido via PUT.
 */
public class CompanyControllerTest extends ScosOrganizationTestUtil {

    private static final String COMPANIES_URI = "/api/v1/companies";
    private static final long SEEDED_ID = 1L;
    private static final long NONEXISTENT_ID = 999_999L;

    // CNPJs válidos (DV correto) e distintos por cenário — evita colisão no cache de idempotência.
    private static final String CNPJ_MATRIX = "11111222000106";
    private static final String CNPJ_FILIAL = "22222333000106";
    private static final String CNPJ_REASON_INACTIVE = "33333444000106";
    private static final String CNPJ_REASON_INCOMPAT = "44444555000106";
    private static final String CNPJ_REASON_NOTFOUND = "55555666000106";
    private static final String CNPJ_PARENT_NOTFOUND = "66666777000106";
    private static final String CNPJ_IDEMPOTENT = "77777888000106";
    private static final String CNPJ_UPDATE = "88888999000106";
    private static final String CNPJ_DUP_A = "99999000000104";
    private static final String CNPJ_DUP_B = "12121212000106";
    private static final String CNPJ_NO_TOKEN = "34343434000106";
    private static final String CNPJ_NO_PERMISSION = "56565656000106";
    private static final String CNPJ_INVALID_DV = "11222333000199";

    private static final long REASON_COMPANY_ACTIVE = 1L;
    private static final long REASON_EMPLOYEE = 2L;
    private static final long REASON_COMPANY_INACTIVE = 5L;

    private static final String CODE_COMPANY_NOT_FOUND = "SCOS_COMPANY_001";
    private static final String CODE_COMPANY_CONFLICT = "SCOS_COMPANY_002";
    private static final String CODE_REASON_INACTIVE = "SCOS_COMPANY_008";
    private static final String CODE_REASON_INCOMPATIBLE = "SCOS_COMPANY_009";
    private static final String CODE_REASON_NOT_FOUND = "SCOS_REASON_ACTIVATE_001";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET /v1/companies (UC-004)
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/companies — token válido lista com paginação (200)")
    void getAll_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == 1)]").exists())
                .andExpect(jsonPath("$.paginatedDTO").exists());
    }

    @Test
    @DisplayName("GET /v1/companies — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    // =====================================================================================
    // GET /v1/companies/{id} (UC-003)
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/companies/{id} — retorna a empresa semeada (200)")
    void getById_seeded_returns200() throws Exception {
        mockMvc.perform(get(COMPANIES_URI + "/{id}", SEEDED_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_ID))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /v1/companies/{id} — id inexistente retorna 404 SCOS_COMPANY_001")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(COMPANIES_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_NOT_FOUND));
    }

    // =====================================================================================
    // POST /v1/companies (UC-001/UC-002)
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/companies — matriz válida cria e retorna o id (201)")
    void create_matrix_returns201() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_MATRIX, REASON_COMPANY_ACTIVE, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/companies — filial com empresa mãe ativa cria (201)")
    void create_filial_returns201() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_FILIAL, REASON_COMPANY_ACTIVE, SEEDED_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/companies — sem taxIdentifier retorna 400 de validação")
    void create_missingTaxIdentifier_returns400() throws Exception {
        String body = """
                {
                  "name": "Sem CNPJ",
                  "nameTreatment": "Sem",
                  "foundationDate": "2020-01-01",
                  "sectorOfActivity": "Tecnologia",
                  "reasonActivateId": 1
                }
                """;

        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/companies — CNPJ com dígito verificador inválido retorna 400")
    void create_invalidCnpjDv_returns400() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_INVALID_DV, REASON_COMPANY_ACTIVE, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION));
    }

    @Test
    @DisplayName("POST /v1/companies — motivo de ativação inativo retorna 422 SCOS_COMPANY_008")
    void create_reasonInactive_returns422() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_REASON_INACTIVE, REASON_COMPANY_INACTIVE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/companies — motivo com entityType=EMPLOYEE retorna 422 SCOS_COMPANY_009")
    void create_reasonIncompatible_returns422() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_REASON_INCOMPAT, REASON_EMPLOYEE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("POST /v1/companies — reasonActivateId inexistente retorna 404")
    void create_reasonNotFound_returns404() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_REASON_NOTFOUND, NONEXISTENT_ID, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REASON_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/companies — parentCompanyId inexistente retorna 404 SCOS_COMPANY_001")
    void create_parentNotFound_returns404() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_PARENT_NOTFOUND, REASON_COMPANY_ACTIVE, NONEXISTENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/companies — sem token retorna 401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_NO_TOKEN, REASON_COMPANY_ACTIVE, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/companies — sem permissão retorna 403")
    void create_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CNPJ_NO_PERMISSION, REASON_COMPANY_ACTIVE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("POST /v1/companies — duas chamadas idênticas são idempotentes (Redis)")
    void create_sameRequestTwice_isIdempotent() throws Exception {
        String body = createBody(CNPJ_IDEMPOTENT, REASON_COMPANY_ACTIVE, null);

        String first = mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post(COMPANIES_URI)
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
    // PUT /v1/companies/{id} (UC-005)
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/companies/{id} — payload válido atualiza (204)")
    void update_valid_returns204() throws Exception {
        long id = create(CNPJ_UPDATE, REASON_COMPANY_ACTIVE);

        mockMvc.perform(put(COMPANIES_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(CNPJ_UPDATE, "Nome Atualizado")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(COMPANIES_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Nome Atualizado"));
    }

    @Test
    @DisplayName("PUT /v1/companies/{id} — id inexistente retorna 404 SCOS_COMPANY_001")
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put(COMPANIES_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(CNPJ_DUP_B, "Inexistente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/companies/{id} — taxIdentifier de outra empresa retorna 409 SCOS_COMPANY_002")
    void update_duplicateTaxIdentifier_returns409() throws Exception {
        create(CNPJ_DUP_A, REASON_COMPANY_ACTIVE);
        long idB = create(CNPJ_DUP_B, REASON_COMPANY_ACTIVE);

        mockMvc.perform(put(COMPANIES_URI + "/{id}", idB)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(CNPJ_DUP_A, "Colidindo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_CONFLICT));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String createBody(String taxIdentifier, long reasonActivateId, Long parentCompanyId) {
        String parent = parentCompanyId == null ? "" : "  \"parentCompanyId\": " + parentCompanyId + ",\n";
        return """
                {
                  "name": "Empresa Teste LTDA",
                  "nameTreatment": "Teste",
                  "taxIdentifier": "%s",
                  "foundationDate": "2020-01-01",
                  "sectorOfActivity": "Tecnologia da Informação",
                %s  "reasonActivateId": %d
                }
                """.formatted(taxIdentifier, parent, reasonActivateId);
    }

    private static String updateBody(String taxIdentifier, String name) {
        return """
                {
                  "name": "%s",
                  "nameTreatment": "Teste",
                  "taxIdentifier": "%s",
                  "foundationDate": "2020-01-01",
                  "sectorOfActivity": "Tecnologia da Informação"
                }
                """.formatted(name, taxIdentifier);
    }

    private long create(String taxIdentifier, long reasonActivateId) throws Exception {
        String response = mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(taxIdentifier, reasonActivateId, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }
}
