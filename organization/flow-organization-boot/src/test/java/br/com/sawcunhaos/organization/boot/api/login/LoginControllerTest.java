
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração das regras de negócio do agregado Login — mecânica base de criação
 * (Story 3.1): {@code POST /v1/employees/{employeeId}/logins} nasce sempre em
 * {@code PENDING_APPROVAL}, os 3 GETs (por id, listagem geral, listagem por funcionário), e a
 * reativação via aprovação (Story 3.3): {@code PUT .../enable}/{@code unblock} abrem uma
 * {@code LoginApprovalRequest} sem mudar o status do Login.
 *
 * <p>Sobe o stack real via {@link ScosOrganizationTestUtil}: Postgres + Redis (ComposeContainer),
 * WireMock :7080 (Keycloak/JWT) e GrpcMock :8090 (registry/authority). Caminho exercitado:
 * MockMvc → filtros JWT → {@code @PreAuthorize} → delegate → use case → domain → Postgres real.
 * O path do MockMvc é relativo ao servlet (sem o context-path {@code /organization}).
 *
 * <p>Dados do seed usados:
 * <ul>
 *   <li>SCOS_EMPLOYEE id=1 ('Scos Admin'), status ACTIVE</li>
 *   <li>SCOS_PROFILE id=1 (ADMIN), id=2 (INTEGRATION)</li>
 *   <li>SCOS_LOGIN id=1 ('scos-admin', EMPLOYEE, employee_id=1, ACTIVE) — usado pro cenário
 *       de login duplicado (409) e pros GETs de sucesso</li>
 *   <li>SCOS_LOGIN id=2 ('scos-api', SERVICE, sem employee, ACTIVE) — usado pro filtro por type</li>
 *   <li>SCOS_LOGIN id=4 ('scos-inactive', SERVICE, sem employee, INACTIVE) — fixture da Story 3.3
 *       para {@code PUT .../enable}, já que não há endpoint que produza esse estado via API</li>
 *   <li>SCOS_LOGIN id=5 ('scos-blocked', SERVICE, sem employee, BLOCKED) — fixture da Story 3.3
 *       para {@code PUT .../unblock}</li>
 * </ul>
 *
 * <p>Isolamento por método via {@code @Sql} (setup/delete_all) — ver Javadoc de
 * {@link ScosOrganizationTestUtil}. Cada criação bem-sucedida usa um {@code login} único
 * pra não colidir com o cache de idempotência (Redis, {@code x-jdempotentrequestpayload}).
 */
public class LoginControllerTest extends ScosOrganizationTestUtil {

    private static final String EMPLOYEES_URI = "/api/v1/employees";
    private static final String LOGINS_URI = "/api/v1/logins";

    private static final long SEEDED_ACTIVE_EMPLOYEE_ID = 1L;
    private static final long SEEDED_ADMIN_PROFILE_ID = 1L;
    private static final long SEEDED_LOGIN_ID = 1L;
    private static final String SEEDED_LOGIN_VALUE = "scos-admin";
    private static final long SEEDED_INACTIVE_LOGIN_ID = 4L;
    private static final long SEEDED_BLOCKED_LOGIN_ID = 5L;
    private static final long NONEXISTENT_ID = 999_999L;

    private static final long SEEDED_COMPANY_ID = 1L;
    private static final long SEEDED_POSITION_ID = 1L;
    private static final long REASON_ACTIVATE_EMPLOYEE_NEW_HIRE = 2L;
    private static final long REASON_INACTIVATE_EMPLOYEE_RESIGNATION = 2L;

    private static final String CODE_LOGIN_CONFLICT = "SCOS_LOGIN_015";
    private static final String CODE_LOGIN_NOT_FOUND = "SCOS_LOGIN_016";
    private static final String CODE_LOGIN_INVALID_TRANSITION = "SCOS_LOGIN_013";
    private static final String CODE_LOGIN_REACTIVATION_PENDING = "SCOS_LOGIN_019";
    private static final String CODE_PROFILE_NOT_FOUND = "SCOS_PROFILE_001";
    private static final String CODE_EMPLOYEE_NOT_FOUND = "SCOS_EMPLOYEE_014";
    private static final String CODE_EMPLOYEE_NOT_ACTIVE = "SCOS_EMPLOYEE_025";
    private static final String CODE_VALIDATION = "SCOS-001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    // =====================================================================================
    // POST /v1/employees/{employeeId}/logins — criação
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — payload válido cria em PENDING_APPROVAL (201)")
    void createEmployeeLogin_withValidToken_returns201() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.1", SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — cria de fato PENDING_APPROVAL, confirmado via GET")
    void createEmployeeLogin_thenGet_returnsPendingApproval() throws Exception {
        long id = createEmployeeLogin(SEEDED_ACTIVE_EMPLOYEE_ID, "new.login.2", SEEDED_ADMIN_PROFILE_ID);

        mockMvc.perform(get(LOGINS_URI + "/{id}", id)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.login").value("new.login.2"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.type").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.profile.id").value(SEEDED_ADMIN_PROFILE_ID))
                .andExpect(jsonPath("$.data.employee.id").value(SEEDED_ACTIVE_EMPLOYEE_ID));
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — login já existente retorna 409 SCOS_LOGIN_015")
    void createEmployeeLogin_duplicateLogin_returns409() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody(SEEDED_LOGIN_VALUE, SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_LOGIN_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — profileId inexistente retorna 404 SCOS_PROFILE_001")
    void createEmployeeLogin_profileNotFound_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.3", NONEXISTENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_PROFILE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — employeeId inexistente retorna 404 SCOS_EMPLOYEE_014")
    void createEmployeeLogin_employeeNotFound_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.4", SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — funcionário não ACTIVE retorna 422 SCOS_EMPLOYEE_025")
    void createEmployeeLogin_employeeNotActive_returns422() throws Exception {
        long inactiveEmployeeId = createInactiveEmployee();

        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", inactiveEmployeeId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.5", SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_ACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — sem login retorna 400 de validação")
    void createEmployeeLogin_missingLogin_returns400() throws Exception {
        String body = """
                {
                  "profileId": %d
                }
                """.formatted(SEEDED_ADMIN_PROFILE_ID);

        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(CODE_VALIDATION))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — sem token retorna 401")
    void createEmployeeLogin_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.6", SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/employees/{employeeId}/logins — sem a permissão CREATE_LOGIN retorna 403")
    void createEmployeeLogin_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody("new.login.7", SEEDED_ADMIN_PROFILE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/logins/{id}/enable e /unblock — reativação via aprovação (Story 3.3)
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — Login INACTIVE abre LoginApprovalRequest PENDING e permanece INACTIVE (204)")
    void activateLogin_fromInactive_opensApprovalRequestWithoutChangingStatus() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("activate-inactive")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/login-approval-requests")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(SEEDED_INACTIVE_LOGIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loginId").value(SEEDED_INACTIVE_LOGIN_ID))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].requestType").value("REACTIVATE_LOGIN"));
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/unblock — Login BLOCKED abre LoginApprovalRequest PENDING e permanece BLOCKED (204)")
    void unblockLogin_fromBlocked_opensApprovalRequestWithoutChangingStatus() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/unblock", SEEDED_BLOCKED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("unblock-blocked")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_BLOCKED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        mockMvc.perform(get("/api/v1/login-approval-requests")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(SEEDED_BLOCKED_LOGIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestType").value("REACTIVATE_LOGIN"));
    }

    @Test
    @DisplayName("PUT .../approve — reativação (INACTIVE) aprovada leva o Login a ACTIVE via activate() (204)")
    void approveReactivation_fromInactive_activatesLogin() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("approve-inactive")))
                .andExpect(status().isNoContent());

        long requestId = findReactivationRequestId(SEEDED_INACTIVE_LOGIN_ID);

        // reasonId 7 (LOGIN/REACTIVATION) - não 4 (LOGIN/NEW_HIRE) - para não colidir no cache
        // jDempotent com LoginApprovalRequestControllerTest#approve_withSystemAccessHolder_activatesLogin,
        // que aprova a 1ª LoginApprovalRequest (id=1) da sua rodada com o mesmo reasonId=4 e nenhum
        // outro campo no corpo para diferenciar (Redis não é resetado entre métodos/classes)
        mockMvc.perform(put("/api/v1/login-approval-requests/{id}/approve", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": 7}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT .../reject — reativação (BLOCKED) rejeitada mantém o Login BLOCKED, sem novo estado terminal (204)")
    void rejectReactivation_fromBlocked_keepsLoginBlocked() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/unblock", SEEDED_BLOCKED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("reject-blocked")))
                .andExpect(status().isNoContent());

        long requestId = findReactivationRequestId(SEEDED_BLOCKED_LOGIN_ID);

        mockMvc.perform(put("/api/v1/login-approval-requests/{id}/reject", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonId": 3, "observation": "sem aval"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_BLOCKED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        mockMvc.perform(get("/api/v1/login-approval-requests/{id}", requestId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — Login já ACTIVE retorna 422 SCOS_LOGIN_013 (transição inválida)")
    void activateLogin_alreadyActive_returns422() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_LOGIN_INVALID_TRANSITION));
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — id inexistente retorna 404 SCOS_LOGIN_016")
    void activateLogin_notFound_returns404() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_LOGIN_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — já existe solicitação PENDING para o Login retorna 422 SCOS_LOGIN_019")
    void activateLogin_withPendingReactivationAlready_returns422() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("pending-first")))
                .andExpect(status().isNoContent());

        // observation diferente da 1ª chamada - senão o jDempotent devolveria a resposta cacheada
        // (204) da 1ª chamada em vez de executar a regra de negócio de novo (ver Javadoc da classe)
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("pending-second")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_LOGIN_REACTIVATION_PENDING));
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — sem token retorna 401")
    void activateLogin_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/logins/{id}/enable — sem a permissão ENABLE_LOGIN retorna 403")
    void activateLogin_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(LOGINS_URI + "/{id}/enable", SEEDED_INACTIVE_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reactivationBody("without-permission")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // GET /v1/logins/{id} — consulta por id
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/logins/{id} — retorna o login do seed, completo (profile/employee aninhados) (200)")
    void getLoginById_seeded_returns200() throws Exception {
        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SEEDED_LOGIN_ID))
                .andExpect(jsonPath("$.data.login").value(SEEDED_LOGIN_VALUE))
                .andExpect(jsonPath("$.data.type").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.profile.id").value(SEEDED_ADMIN_PROFILE_ID))
                .andExpect(jsonPath("$.data.profile.code").value("ADMIN"))
                .andExpect(jsonPath("$.data.employee.id").value(SEEDED_ACTIVE_EMPLOYEE_ID))
                .andExpect(jsonPath("$.data.employee.name").value("Scos Admin"));
    }

    @Test
    @DisplayName("GET /v1/logins/{id} — id inexistente retorna 404 SCOS_LOGIN_016")
    void getLoginById_notFound_returns404() throws Exception {
        mockMvc.perform(get(LOGINS_URI + "/{id}", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_LOGIN_NOT_FOUND));
    }

    @Test
    @DisplayName("GET /v1/logins/{id} — sem token retorna 401")
    void getLoginById_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/logins/{id} — sem a permissão GET_LOGIN retorna 403")
    void getLoginById_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(LOGINS_URI + "/{id}", SEEDED_LOGIN_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // GET /v1/logins — listagem geral (resumida) com filtros
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/logins — token válido lista os logins semeados, forma resumida (200)")
    void getAllLogins_withValidToken_returns200() throws Exception {
        mockMvc.perform(get(LOGINS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.login == '" + SEEDED_LOGIN_VALUE + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.login == 'scos-api')]").exists());
    }

    @Test
    @DisplayName("GET /v1/logins — filtro type=SERVICE retorna só scos-api (200)")
    void getAllLogins_filterTypeService_returnsOnlyServiceLogin() throws Exception {
        mockMvc.perform(get(LOGINS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("type", "SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.login == 'scos-api')]").exists())
                .andExpect(jsonPath("$.data[?(@.login == '" + SEEDED_LOGIN_VALUE + "')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/logins — filtro employeeId retorna só os logins desse funcionário (200)")
    void getAllLogins_filterByEmployeeId_returnsOnlyThatEmployeeLogins() throws Exception {
        mockMvc.perform(get(LOGINS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10")
                        .param("employeeId", String.valueOf(SEEDED_ACTIVE_EMPLOYEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.login == '" + SEEDED_LOGIN_VALUE + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.login == 'scos-api')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/logins — sem token retorna 401")
    void getAllLogins_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(LOGINS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/logins — sem a permissão GET_LOGIN retorna 403")
    void getAllLogins_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(LOGINS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // GET /v1/employees/{employeeId}/logins — listagem por funcionário
    // =====================================================================================

    @Test
    @DisplayName("GET /v1/employees/{employeeId}/logins — retorna só os logins do funcionário (200)")
    void getAllEmployeeLogins_seeded_returns200() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.login == '" + SEEDED_LOGIN_VALUE + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.login == 'scos-api')]").doesNotExist());
    }

    @Test
    @DisplayName("GET /v1/employees/{employeeId}/logins — sem token retorna 401")
    void getAllEmployeeLogins_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /v1/employees/{employeeId}/logins — sem a permissão GET_LOGIN retorna 403")
    void getAllEmployeeLogins_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(get(EMPLOYEES_URI + "/{employeeId}/logins", SEEDED_ACTIVE_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1")
                        .param("sizePerPage", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String createLoginBody(String login, long profileId) {
        return """
                {
                  "login": "%s",
                  "profileId": %d
                }
                """.formatted(login, profileId);
    }

    /** {@code observation} é a chave de idempotência de enable/unblock (sem mais reasonId) - precisa ser única por teste, mesma lógica do {@code login} único usado na criação. */
    private static String reactivationBody(String observation) {
        return """
                {
                  "observation": "%s"
                }
                """.formatted(observation);
    }

    private long findReactivationRequestId(long loginId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/login-approval-requests")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .param("page", "1").param("sizePerPage", "10")
                        .param("loginId", String.valueOf(loginId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data[0].id")).longValue();
    }

    private long createEmployeeLogin(long employeeId, String login, long profileId) throws Exception {
        String response = mockMvc.perform(post(EMPLOYEES_URI + "/{employeeId}/logins", employeeId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoginBody(login, profileId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    /** Cria um novo Funcionário e o inativa em seguida — usado só pra ter um id INACTIVE isolado do seed (id=1). */
    private long createInactiveEmployee() throws Exception {
        String createBody = """
                {
                  "name": "Funcionário Inativo",
                  "nameTreatment": "Teste",
                  "taxIdentifier": "98765432100",
                  "email": "inactive.employee@sawcunhaos.com.br",
                  "birthDate": "2000-01-01",
                  "dateOfHiring": "2026-08-01",
                  "contractType": "CLT",
                  "companyId": %d,
                  "positionId": %d,
                  "reasonActivateId": %d
                }
                """.formatted(SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE);

        String createResponse = mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long employeeId = ((Number) com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id")).longValue();

        String disableBody = """
                {
                  "reasonId": %d
                }
                """.formatted(REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", employeeId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disableBody))
                .andExpect(status().isNoContent());

        return employeeId;
    }
}
