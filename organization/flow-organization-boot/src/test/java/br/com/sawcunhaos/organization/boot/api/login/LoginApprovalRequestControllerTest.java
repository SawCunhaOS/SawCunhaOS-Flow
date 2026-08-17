
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

package br.com.sawcunhaos.organization.boot.api.login;

import br.com.sawcunhaos.organization.boot.infrastructure.ScosOrganizationTestUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração da cadeia de aprovação de acesso (Story 3.2): abertura automática da
 * {@code LoginApprovalRequest} na criação do Login (Story 3.1), consulta (2 GETs + o novo
 * {@code system-access-approvers}) e decisão ({@code approve}/{@code reject}).
 *
 * <p>Sobe o stack real via {@link ScosOrganizationTestUtil}: Postgres + Redis (ComposeContainer),
 * WireMock :7080 (Keycloak/JWT) e GrpcMock :8090 (registry/authority). Caminho exercitado:
 * MockMvc → filtros JWT → {@code @PreAuthorize} → delegate → use case → domain → Postgres real.
 *
 * <p>O JWT de teste ({@code BEAR_TOKEN_VALID}) sempre resolve para o Login semeado
 * {@code inside.admin} (id=3, employee_id=1, profile ADMIN - todas as permissões, incluindo
 * {@code APPROVE_SYSTEM_ACCESS}), então toda decisão feita com ele passa pela válvula de última
 * instância (AC 3), independentemente do {@code currentLevel} resolvido - suficiente para exercitar
 * o fluxo feliz completo sem precisar emular supervisores/gerentes reais.
 *
 * <p>Dados do seed usados:
 * <ul>
 *   <li>SCOS_EMPLOYEE id=1 ('Scos Admin'), department TI (id=1) sem gerente por padrão -
 *       cadeia resolve direto para SYSTEM_ACCESS_GROUP</li>
 *   <li>SCOS_REASON_ACTIVATE id=4 (LOGIN/NEW_HIRE), SCOS_REASON_INACTIVATE id=3 (LOGIN/ACCOUNT_CLOSED)</li>
 * </ul>
 */
public class LoginApprovalRequestControllerTest extends ScosOrganizationTestUtil {

    private static final String EMPLOYEES_URI = "/api/v1/employees";
    private static final String LOGINS_URI = "/api/v1/logins";
    private static final String REQUESTS_URI = "/api/v1/login-approval-requests";
    private static final String DEPARTMENTS_URI = "/api/v1/departments";

    private static final long SEEDED_ACTIVE_EMPLOYEE_ID = 1L;
    private static final long SEEDED_ADMIN_PROFILE_ID = 1L;
    private static final long SEEDED_DEPARTMENT_ID = 1L;
    private static final long REASON_ACTIVATE_LOGIN_NEW_HIRE = 4L;
    private static final long REASON_INACTIVATE_LOGIN_ACCOUNT_CLOSED = 3L;
    private static final long NONEXISTENT_ID = 999_999L;

    private static final String CODE_REQUEST_NOT_FOUND = "SCOS_LOGIN_APPROVAL_REQUEST_001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // Abertura automática na criação (Task 8) + GET /v1/login-approval-requests
    // =====================================================================================

    @Test
    @DisplayName("POST cria Login PENDING_APPROVAL e abre LoginApprovalRequest PENDING na mesma transação")
    void createEmployeeLogin_opensApprovalRequestAutomatically() throws Exception {
        long loginId = createEmployeeLogin("lar.login.1");

        mockMvc.perform(get(REQUESTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].loginId").value(loginId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].requestType").value("CREATE_LOGIN"))
                .andExpect(jsonPath("$.data[0].escalationPolicy").value("INDEFINITE"))
                // Employee 1 sem supervisor e departamento TI sem gerente -> cadeia pula direto para SYSTEM_ACCESS_GROUP
                .andExpect(jsonPath("$.data[0].currentLevel").value("SYSTEM_ACCESS_GROUP"))
                .andExpect(jsonPath("$.data[0].currentApprover").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/login-approval-requests — sem token retorna 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(REQUESTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/login-approval-requests — sem a permissão GET_LOGIN_APPROVAL_REQUEST retorna 403")
    void getAll_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(REQUESTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("GET /v1/login-approval-requests com o Gerente do Departamento cadastrado -> currentLevel=MANAGER, currentApprover presente (AC 10)")
    void createEmployeeLogin_whenDepartmentHasManager_resolvesManagerLevel() throws Exception {
        // Employee 1 vira o próprio gerente do departamento TI (id=1) - circular, mas suficiente
        // para exercitar a resolução do nível MANAGER sem precisar criar um 2º funcionário.
        mockMvc.perform(put(DEPARTMENTS_URI + "/{id}", SEEDED_DEPARTMENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "TI", "description": "Tecnologia da Informação", "managerId": %d}
                                """.formatted(SEEDED_ACTIVE_EMPLOYEE_ID)))
                .andExpect(status().isNoContent());

        long loginId = createEmployeeLogin("lar.login.2");

        mockMvc.perform(get(REQUESTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(loginId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentLevel").value("MANAGER"))
                .andExpect(jsonPath("$.data[0].currentApprover.employeeId").value(SEEDED_ACTIVE_EMPLOYEE_ID));
    }

    // =====================================================================================
    // GET /v1/login-approval-requests/{id}
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/login-approval-requests/{id} — retorna o detalhe da solicitação (200)")
    void getById_seeded_returns200() throws Exception {
        long loginId = createEmployeeLogin("lar.login.3");
        long requestId = findRequestIdByLoginId(loginId);

        mockMvc.perform(get(REQUESTS_URI + "/{id}", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requestId))
                .andExpect(jsonPath("$.data.loginId").value(loginId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /v1/login-approval-requests/{id} — id inexistente retorna 404 SCOS_LOGIN_APPROVAL_REQUEST_001")
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get(REQUESTS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REQUEST_NOT_FOUND));
    }

    // =====================================================================================
    // GET /v1/login-approval-requests/system-access-approvers
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/login-approval-requests/system-access-approvers — lista quem tem APPROVE_SYSTEM_ACCESS hoje (200)")
    void getSystemAccessApprovers_returns200() throws Exception {
        mockMvc.perform(get(REQUESTS_URI + "/system-access-approvers")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.login == 'inside.admin')]").exists());
    }

    // =====================================================================================
    // PUT /v1/login-approval-requests/{id}/approve
    // =====================================================================================

    @Test
    @DisplayName("PUT .../approve — aprovador com APPROVE_SYSTEM_ACCESS aprova e o Login vira ACTIVE (204)")
    void approve_withSystemAccessHolder_activatesLogin() throws Exception {
        long loginId = createEmployeeLogin("lar.login.4");
        long requestId = findRequestIdByLoginId(loginId);

        mockMvc.perform(put(REQUESTS_URI + "/{id}/approve", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": %d}
                                """.formatted(REASON_ACTIVATE_LOGIN_NEW_HIRE)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", loginId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get(REQUESTS_URI + "/{id}", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.decidedByLoginId").exists());
    }

    @Test
    @DisplayName("PUT .../approve — id inexistente retorna 404 SCOS_LOGIN_APPROVAL_REQUEST_001")
    void approve_notFound_returns404() throws Exception {
        mockMvc.perform(put(REQUESTS_URI + "/{id}/approve", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": %d}
                                """.formatted(REASON_ACTIVATE_LOGIN_NEW_HIRE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REQUEST_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT .../approve — sem token retorna 401")
    void approve_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(REQUESTS_URI + "/{id}/approve", 1L)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": %d}
                                """.formatted(REASON_ACTIVATE_LOGIN_NEW_HIRE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT .../approve — sem a permissão DECIDE_LOGIN_APPROVAL_REQUEST retorna 403")
    void approve_withoutPermission_returns403() throws Exception {
        // Sem chamada autenticada prévia nesta identidade: o cache scos:authority:ctx só é limpo
        // entre métodos (@AfterEach) - uma chamada anterior com o mesmo token cachearia full-admin
        // e mascararia o stub abaixo. @PreAuthorize nega antes do use case rodar, então o id nem
        // precisa existir de fato.
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(REQUESTS_URI + "/{id}/approve", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": %d}
                                """.formatted(REASON_ACTIVATE_LOGIN_NEW_HIRE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/login-approval-requests/{id}/reject
    // =====================================================================================

    @Test
    @DisplayName("PUT .../reject — rejeita e o Login vira REJECTED, estado terminal (204)")
    void reject_withSystemAccessHolder_rejectsLogin() throws Exception {
        long loginId = createEmployeeLogin("lar.login.6");
        long requestId = findRequestIdByLoginId(loginId);

        mockMvc.perform(put(REQUESTS_URI + "/{id}/reject", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": %d, "observation": "sem aderência ao cargo"}
                                """.formatted(REASON_INACTIVATE_LOGIN_ACCOUNT_CLOSED)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", loginId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get(REQUESTS_URI + "/{id}", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String createLoginBody(String login) {
        return """
                {
                  "login": "%s",
                  "profileId": %d
                }
                """.formatted(login, SEEDED_ADMIN_PROFILE_ID);
    }

    private long createEmployeeLogin(String login) throws Exception {
        String response = mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody(login)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private long findRequestIdByLoginId(long loginId) throws Exception {
        String response = mockMvc.perform(get(REQUESTS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(loginId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data[0].id")).longValue();
    }
}
