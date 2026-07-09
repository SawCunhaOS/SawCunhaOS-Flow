
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

package br.com.sawcunhaos.organization.boot.api.department;

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
 * Teste de integração das regras de negócio do agregado Department.
 *
 * <p>Sobe o stack real via {@link ScosOrganizationTestUtil}:
 * <ul>
 *   <li>Postgres + Redis (ComposeContainer) → app real com Liquibase + seed (@Sql)</li>
 *   <li>WireMock :7080 (Keycloak) → validação do JWT</li>
 *   <li>GrpcMock :8090 (registry/authority) → permissões</li>
 * </ul>
 *
 * <p>Caminho exercitado: MockMvc → filtros (JWT) → {@code @PreAuthorize} → delegate →
 * use case → domain → Postgres real (triggers/views). O path do MockMvc é relativo ao
 * servlet (sem o context-path {@code /organization}).
 *
 * <p>Isolamento por método: {@code setsup_database.sql} (BEFORE) recria o seed e
 * {@code delete_all.sql} (AFTER) faz TRUNCATE ... RESTART IDENTITY, então os IDs
 * gerados voltam a ser determinísticos. O cache de idempotência (Redis) NÃO é
 * resetado entre métodos — por isso cada criação/atualização bem-sucedida usa um
 * {@code code} único, evitando colisão de payload no jDempotent.
 *
 * <p>Dados do seed usados:
 * <ul>
 *   <li>SCOS_DEPARTMENT id=1, code='TI', description='Tecnologia da Informação', active=true</li>
 *   <li>SCOS_POSITION id=1, code='ADMIN_SISTEMA', active=true, department_id=1 (vínculo ativo)</li>
 * </ul>
 *
 * <p>Contrato de erro (RFC 9457 ProblemDetail) validado nos cenários de falha:
 * {@code type}, {@code title}, {@code status}, {@code detail}, {@code instance} e as
 * propriedades {@code code}/{@code requestId}/{@code timestamp} (mais {@code errors}
 * na validação de campos).
 */
public class DepartmentControllerTest extends ScosOrganizationTestUtil {

    private static final String DEPARTMENTS_URI = "/api/v1/departments";
    private static final long SEEDED_DEPARTMENT_ID = 1L;
    private static final String SEEDED_CODE = "TI";
    private static final long NONEXISTENT_ID = 999_999L;

    // Mensagens (detail) e títulos esperados nas respostas de erro — PT-BR.
    private static final String DETAIL_NOT_FOUND = "O departamento informado não existe.";
    private static final String DETAIL_CODE_CONFLICT = "Já existe um departamento cadastrado com esse código.";
    private static final String DETAIL_HAS_ACTIVE_POSITIONS =
            "Não foi possível inativar o departamento, pois ele possui vínculos com posições ativas.";
    private static final String DETAIL_ALREADY_ACTIVE = "O departamento informado já está ativo.";
    private static final String DETAIL_ALREADY_INACTIVE = "O departamento informado já está inativo.";

    private static final String TITLE_NOT_FOUND = "Recurso não encontrado";
    private static final String TITLE_CONFLICT = "Conflito de dados";
    private static final String TITLE_BUSINESS_RULE = "Regra de negócio violada";

    // Códigos de erro (propriedade `code` do ProblemDetail).
    private static final String CODE_NOT_FOUND = "SCOS_DEPARTMENT_001";
    private static final String CODE_CONFLICT = "SCOS_DEPARTMENT_002";
    private static final String CODE_ACTIVE_POSITIONS = "SCOS_DEPARTMENT_003";
    private static final String CODE_ALREADY_ACTIVE = "SCOS_DEPARTMENT_004";
    private static final String CODE_ALREADY_INACTIVE = "SCOS_DEPARTMENT_005";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // GET /v1/departments — listagem
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/departments — token válido lista os departamentos semeados (200)")
    void getAllDepartments_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists());
    }

    @Test
    @DisplayName("GET /v1/departments — filtro active=true retorna somente os ativos (200)")
    void getAllDepartments_filterActiveTrue_returnsOnlyActive() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.active == false)]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/departments — filtro active=false não retorna o seed ativo (200)")
    void getAllDepartments_filterActiveFalse_doesNotReturnActive() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == '" + SEEDED_CODE + "')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.active == true)]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/departments — sem token retorna 401")
    void getAllDepartments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/departments — token inválido (fora do JWKS) retorna 401")
    void getAllDepartments_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_INVALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/departments — sem a permissão GET_DEPARTMENT retorna 403 no padrão RFC 9457")
    void getAllDepartments_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(DEPARTMENTS_URI)
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
    // GET /v1/departments/{id} — consulta por id
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/departments/{id} — retorna o departamento do seed (200)")
    void getDepartmentById_withValidToken_returnsSeededDepartment() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_DEPARTMENT_ID))
                .andExpect(jsonPath("$.data.code").value(SEEDED_CODE))
                .andExpect(jsonPath("$.data.description").value("Tecnologia da Informação"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /v1/departments/{id} — id inexistente retorna 404 SCOS_DEPARTMENT_001")
    void getDepartmentById_notFound_returns404() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/departments/{id} — sem token retorna 401")
    void getDepartmentById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/departments/{id} — sem a permissão GET_DEPARTMENT retorna 403")
    void getDepartmentById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST /v1/departments — criação
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/departments — payload válido cria e retorna o id (201)")
    void createDepartment_withValidToken_returns201() throws Exception {
        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("CRE11", "Financeiro")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/departments — code já existente retorna 409 SCOS_DEPARTMENT_002")
    void createDepartment_duplicateCode_returns409() throws Exception {
        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody(SEEDED_CODE, "Duplicado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.title").value(TITLE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/departments — sem code retorna 400 de validação")
    void createDepartment_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code"
                }
                """;

        mockMvc.perform(post(DEPARTMENTS_URI)
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
    @DisplayName("POST /v1/departments — sem description retorna 400 de validação")
    void createDepartment_missingDescription_returns400() throws Exception {
        String body = """
                {
                  "code": "CRE14"
                }
                """;

        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/departments — code vazio retorna 400 de validação")
    void createDepartment_blankCode_returns400() throws Exception {
        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("", "Code vazio")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/departments — description vazia retorna 400 de validação")
    void createDepartment_blankDescription_returns400() throws Exception {
        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("CRE16", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/departments — sem token retorna 401")
    void createDepartment_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("CRE17", "Sem token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/departments — sem a permissão CREATE_DEPARTMENT retorna 403")
    void createDepartment_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("CRE18", "Sem permissão")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST /v1/departments — idempotência (jDempotent + Redis)
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/departments — duas chamadas idênticas seguidas não geram erro (idempotência via Redis)")
    void createDepartment_sameRequestTwice_isIdempotent() throws Exception {
        String body = departmentBody("IDP40", "Idempotente");

        // 1ª chamada: cria o departamento (201).
        String firstResponse = mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long firstId = ((Number) JsonPath.read(firstResponse, "$.data.id")).longValue();

        // 2ª chamada com o MESMO payload: o jDempotent devolve a resposta em cache
        // (Redis) sem re-executar o use case — logo NÃO retorna 409 de code duplicado.
        String secondResponse = mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long secondId = ((Number) JsonPath.read(secondResponse, "$.data.id")).longValue();

        // Mesma resposta cacheada → mesmo id, sem colisão de code (409).
        assertEquals(firstId, secondId);
    }

    // =====================================================================================
    // PUT /v1/departments/{id} — atualização
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/departments/{id} — payload válido atualiza (204)")
    void updateDepartment_withValidToken_returns204() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("UPD19", "TI Atualizado")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_DEPARTMENT_ID))
                .andExpect(jsonPath("$.data.code").value("UPD19"))
                .andExpect(jsonPath("$.data.description").value("TI Atualizado"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — mantendo o próprio code é permitido (204)")
    void updateDepartment_keepingOwnCode_returns204() throws Exception {
        long id = createDepartment("KEP20", "Keep own code");

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("KEP20", "Descrição alterada")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(DEPARTMENTS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.code").value("KEP20"))
                .andExpect(jsonPath("$.data.description").value("Descrição alterada"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — code de outro departamento retorna 409 SCOS_DEPARTMENT_002")
    void updateDepartment_duplicateCode_returns409() throws Exception {
        long id = createDepartment("UPD21", "A ser renomeado");

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody(SEEDED_CODE, "Colidindo com TI")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_CONFLICT))
                .andExpect(jsonPath("$.title").value(TITLE_CONFLICT))
                .andExpect(jsonPath("$.detail").value(DETAIL_CODE_CONFLICT));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — id inexistente retorna 404 SCOS_DEPARTMENT_001")
    void updateDepartment_notFound_returns404() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("UPD22", "Inexistente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — sem code retorna 400 de validação")
    void updateDepartment_missingCode_returns400() throws Exception {
        String body = """
                {
                  "description": "Sem code"
                }
                """;

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — sem token retorna 401")
    void updateDepartment_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("UPD24", "Sem token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id} — sem a permissão UPDATE_DEPARTMENT retorna 403")
    void updateDepartment_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody("UPD25", "Sem permissão")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/departments/{id}/enable — reativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/departments/{id}/enable — departamento inativo é reativado (204)")
    void enableDepartment_inactiveDepartment_returns204() throws Exception {
        long id = createDepartment("ENA27", "Para reativar");
        disableDepartment(id);

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/enable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/enable — departamento já ativo retorna 422 SCOS_DEPARTMENT_004")
    void enableDepartment_alreadyActive_returns422() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/enable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_ACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/enable — id inexistente retorna 404 SCOS_DEPARTMENT_001")
    void enableDepartment_notFound_returns404() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/enable — sem token retorna 401")
    void enableDepartment_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/enable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/enable — sem a permissão ENABLE_DEPARTMENT retorna 403")
    void enableDepartment_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/enable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/departments/{id}/disable — inativação
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — departamento sem posições ativas é inativado (204)")
    void disableDepartment_withoutActivePositions_returns204() throws Exception {
        long id = createDepartment("DIS32", "Sem posições");

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — departamento com posições ativas retorna 422 SCOS_DEPARTMENT_003")
    void disableDepartment_withActivePositions_returns422() throws Exception {
        // Seed: department id=1 (TI) possui a position ADMIN_SISTEMA ativa.
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVE_POSITIONS))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_HAS_ACTIVE_POSITIONS));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — departamento já inativo retorna 422 SCOS_DEPARTMENT_005")
    void disableDepartment_alreadyInactive_returns422() throws Exception {
        long id = createDepartment("DIS33", "Inativado duas vezes");
        disableDepartment(id);

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ALREADY_INACTIVE))
                .andExpect(jsonPath("$.title").value(TITLE_BUSINESS_RULE))
                .andExpect(jsonPath("$.detail").value(DETAIL_ALREADY_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — id inexistente retorna 404 SCOS_DEPARTMENT_001")
    void disableDepartment_notFound_returns404() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value(DETAIL_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — sem token retorna 401")
    void disableDepartment_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/departments/{id}/disable — sem a permissão DISABLE_DEPARTMENT retorna 403")
    void disableDepartment_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /** Monta o corpo JSON de criação/atualização de departamento. */
    private static String departmentBody(String code, String description) {
        return """
                {
                  "code": "%s",
                  "description": "%s"
                }
                """.formatted(code, description);
    }

    /** Cria um departamento via API e devolve o id gerado. Cada teste usa um code único. */
    private long createDepartment(String code, String description) throws Exception {
        String response = mockMvc.perform(post(DEPARTMENTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentBody(code, description)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    /** Inativa o departamento informado (pré-condição para reativação / dupla inativação). */
    private void disableDepartment(long id) throws Exception {
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}/disable", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }
}
