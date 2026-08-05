
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

package br.com.sawcunhaos.organization.boot.api.employee;

import br.com.sawcunhaos.organization.boot.infrastructure.ScosOrganizationTestUtil;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistory;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração full-stack da admissão de Funcionário (UC-035, Story 2.1).
 *
 * <p>Seed: COMPANY id=1 (ACTIVE), POSITION id=1 (active=true, dept TI), REASON_ACTIVATE id=2
 * (EMPLOYEE/NEW_HIRE, ativo). Configuration: EMPLOYEE_MIN_AGE=18, EMPLOYEE_EMAIL_DOMAIN=sawcunhaos.com.br.
 * Nenhum {@code SCOS_POSITION_WORK_SCHEDULE} vem semeado — o teste do caminho feliz cria 1 linha via
 * {@code POST /v1/positions/{id}/work-schedule} antes de admitir o Funcionário, para exercitar a cópia.
 *
 * <p>{@code taxIdentifier} é o campo {@code x-jdempotentrequestpayload} de {@code createEmployee} —
 * cada teste que faz {@code POST} usa um CPF válido (DV correto) e único, mesmo cuidado da suíte de Company.
 */
class EmployeeControllerTest extends ScosOrganizationTestUtil {

    private static final String EMPLOYEES_URI = "/api/v1/employees";
    private static final String POSITIONS_URI = "/api/v1/positions";

    private static final String COMPANIES_URI = "/api/v1/companies";

    private static final long SEEDED_COMPANY_ID = 1L;
    private static final long SEEDED_POSITION_ID = 1L;
    private static final long SEEDED_DEPARTMENT_ID = 1L;
    private static final long SEEDED_EMPLOYEE_ID = 1L;
    private static final long REASON_ACTIVATE_EMPLOYEE_NEW_HIRE = 2L;
    private static final long REASON_ACTIVATE_EMPLOYEE_INACTIVE = 6L;
    private static final long REASON_ACTIVATE_COMPANY_SCOPED = 1L;
    private static final long REASON_INACTIVATE_EMPLOYEE_RESIGNATION = 2L;
    private static final long REASON_INACTIVATE_EMPLOYEE_INACTIVE = 5L;
    private static final long REASON_INACTIVATE_COMPANY_SCOPED = 1L;
    private static final long REASON_DISABLE_EMPLOYEE_UNDER_AUDIT = 2L;
    private static final long REASON_DISABLE_EMPLOYEE_INACTIVE = 5L;
    private static final long REASON_DISABLE_COMPANY_SCOPED = 1L;
    private static final long REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED = 2L;
    private static final long REASON_ENABLE_EMPLOYEE_INACTIVE = 5L;
    private static final long REASON_ENABLE_COMPANY_SCOPED = 1L;
    private static final long REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT = 3L;
    private static final long REASON_POSITION_CHANGE_TRANSFER = 3L;
    private static final long REASON_POSITION_CHANGE_INACTIVE = 4L;
    private static final long NONEXISTENT_ID = 999_999L;

    // CPFs válidos (DV correto) e distintos por cenário — evita colisão no cache de idempotência.
    private static final String CPF_HAPPY_PATH = "11144477735";
    private static final String CPF_DUPLICATE_TAX_IDENTIFIER = "52998224725";
    private static final String CPF_DUPLICATE_EMAIL = "93541134780";
    private static final String CPF_COMPANY_NOT_FOUND = "12345678909";
    private static final String CPF_POSITION_NOT_FOUND = "03840558930";
    private static final String CPF_WRONG_EMAIL_DOMAIN = "32262626022";
    private static final String CPF_NO_TOKEN = "39036331005";
    private static final String CPF_NO_PERMISSION = "87606057745";

    private static final String CPF_REHIRE_HAPPY_PATH = "10433218100";
    private static final String CPF_REHIRE_STILL_ACTIVE = "96001338914";
    private static final String CPF_REHIRE_DISABLED = "08386379499";
    private static final String CPF_REHIRE_NEVER_REGISTERED = "02654235114";
    private static final String CPF_REHIRE_COMPANY_NOT_FOUND = "16155940789";
    private static final String CPF_REHIRE_POSITION_NOT_FOUND = "81618495950";
    private static final String CPF_REHIRE_REASON_ACTIVATE_NOT_FOUND = "31034131656";
    private static final String CPF_REHIRE_REASON_POSITION_CHANGE_NOT_FOUND = "47525534144";
    private static final String CPF_REHIRE_SUPERVISOR_NOT_FOUND = "92832764851";
    private static final String CPF_REHIRE_COMPANY_INACTIVE = "35030564160";
    private static final String CPF_REHIRE_POSITION_INACTIVE = "39537672409";
    private static final String CPF_REHIRE_SUPERVISOR_INACTIVE = "23884969692";
    private static final String CPF_REHIRE_SUPERVISOR_INACTIVE_SUPERVISOR = "66392332154";
    private static final String CPF_REHIRE_REASON_ACTIVATE_INACTIVE = "46881973659";
    private static final String CPF_REHIRE_REASON_ACTIVATE_INCOMPATIBLE = "99356327254";
    private static final String CPF_REHIRE_REASON_POSITION_CHANGE_INACTIVE = "95181756085";
    private static final String CPF_REHIRE_NO_TOKEN = "48063581776";
    private static final String CPF_REHIRE_NO_PERMISSION = "83970523214";

    private static final String CNPJ_REHIRE_COMPANY_INACTIVE = "11122233000183";

    private static final String CODE_EMPLOYEE_TAX_IDENTIFIER_CONFLICT = "SCOS_EMPLOYEE_002";
    private static final String CODE_EMPLOYEE_EMAIL_CONFLICT = "SCOS_EMPLOYEE_003";
    private static final String CODE_EMPLOYEE_WRONG_EMAIL_DOMAIN = "SCOS_EMPLOYEE_010";
    private static final String CODE_COMPANY_NOT_FOUND = "SCOS_COMPANY_001";
    private static final String CODE_POSITION_NOT_FOUND = "SCOS_POSITION_001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

    private static final String CODE_EMPLOYEE_NOT_FOUND = "SCOS_EMPLOYEE_014";
    private static final String CODE_STATUS_INCOMPATIBLE = "SCOS_EMPLOYEE_001";
    private static final String CODE_ACTIVATE_REASON_INACTIVE = "SCOS_EMPLOYEE_008";
    private static final String CODE_ACTIVATE_REASON_INCOMPATIBLE = "SCOS_EMPLOYEE_009";
    private static final String CODE_INACTIVATE_REASON_INACTIVE = "SCOS_EMPLOYEE_015";
    private static final String CODE_INACTIVATE_REASON_INCOMPATIBLE = "SCOS_EMPLOYEE_016";
    private static final String CODE_DISABLE_REASON_INACTIVE = "SCOS_EMPLOYEE_017";
    private static final String CODE_DISABLE_REASON_INCOMPATIBLE = "SCOS_EMPLOYEE_018";
    private static final String CODE_ENABLE_REASON_INACTIVE = "SCOS_EMPLOYEE_019";
    private static final String CODE_ENABLE_REASON_INCOMPATIBLE = "SCOS_EMPLOYEE_020";

    private static final String CODE_REHIRE_NOT_FOUND = "SCOS_EMPLOYEE_021";
    private static final String CODE_REASON_POSITION_CHANGE_NOT_FOUND = "SCOS_EMPLOYEE_022";
    private static final String CODE_REASON_POSITION_CHANGE_INACTIVE = "SCOS_EMPLOYEE_023";
    private static final String CODE_SUPERVISOR_NOT_FOUND = "SCOS_EMPLOYEE_004";
    private static final String CODE_COMPANY_INACTIVE = "SCOS_EMPLOYEE_005";
    private static final String CODE_POSITION_INACTIVE = "SCOS_EMPLOYEE_006";
    private static final String CODE_SUPERVISOR_INACTIVE = "SCOS_EMPLOYEE_007";
    private static final String CODE_REASON_ACTIVATE_NOT_FOUND = "SCOS_REASON_ACTIVATE_001";

    @Autowired
    private EmployeeQueryRepository employeeQueryRepository;
    @Autowired
    private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    @Autowired
    private EmployeePositionHistoryRepository employeePositionHistoryRepository;
    @Autowired
    private EmployeeWorkScheduleRepository employeeWorkScheduleRepository;

    @Test
    @DisplayName("POST /v1/employees — válido cria o funcionário, copia a jornada do cargo e grava histórico (201)")
    void create_valid_returns201AndCopiesWorkScheduleAndHistory() throws Exception {
        mockMvc.perform(post(POSITIONS_URI + "/{positionId}/work-schedule", SEEDED_POSITION_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": "MONDAY",
                                  "startTime": "08:00:00",
                                  "lunchStart": "12:00:00",
                                  "lunchEnd": "13:00:00",
                                  "endTime": "17:00:00"
                                }
                                """))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_HAPPY_PATH, "valido@sawcunhaos.com.br", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        long employeeId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();

        assertThat(employeeQueryRepository.findById(employeeId)).isPresent();

        Iterable<EmployeeStatusHistory> statusHistory = employeeStatusHistoryRepository.findAll();
        assertThat(statusHistory).anyMatch(h -> h.getEmployee().getId().equals(employeeId)
                && h.getReasonActivate() != null && h.getReasonActivate().getId().equals(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE));

        // getId() em vez de getCode() — acessar getCode() no proxy lazy fora da sessão do Hibernate lançaria LazyInitializationException.
        assertThat(employeePositionHistoryRepository.findAll()).anyMatch(h -> h.getEmployee().getId().equals(employeeId)
                && h.getReasonPositionChange().getId().equals(1L));

        assertThat(employeeWorkScheduleRepository.findAll()).anyMatch(w -> w.getEmployee().getId().equals(employeeId));
    }

    @Test
    @DisplayName("POST /v1/employees — CPF já cadastrado retorna 409 SCOS_EMPLOYEE_002")
    void create_duplicateTaxIdentifier_returns409() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_DUPLICATE_TAX_IDENTIFIER, "primeiro@sawcunhaos.com.br", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_DUPLICATE_TAX_IDENTIFIER, "segundo@sawcunhaos.com.br", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_TAX_IDENTIFIER_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/employees — e-mail já cadastrado retorna 409 SCOS_EMPLOYEE_003")
    void create_duplicateEmail_returns409() throws Exception {
        String sharedEmail = "compartilhado@sawcunhaos.com.br";
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_DUPLICATE_EMAIL, sharedEmail, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("47573308639", sharedEmail, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_EMAIL_CONFLICT));
    }

    @Test
    @DisplayName("POST /v1/employees — companyId inexistente retorna 404 SCOS_COMPANY_001")
    void create_companyNotFound_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_COMPANY_NOT_FOUND, "companynotfound@sawcunhaos.com.br", NONEXISTENT_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees — positionId inexistente retorna 404 SCOS_POSITION_001")
    void create_positionNotFound_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_POSITION_NOT_FOUND, "positionnotfound@sawcunhaos.com.br", SEEDED_COMPANY_ID, NONEXISTENT_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_POSITION_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees — domínio de e-mail não permitido retorna 422 SCOS_EMPLOYEE_010")
    void create_wrongEmailDomain_returns422() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_WRONG_EMAIL_DOMAIN, "fora@outrodominio.com", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_WRONG_EMAIL_DOMAIN));
    }

    @Test
    @DisplayName("POST /v1/employees — sem token retorna 401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_NO_TOKEN, "notoken@sawcunhaos.com.br", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/employees — sem permissão retorna 403")
    void create_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(CPF_NO_PERMISSION, "nopermission@sawcunhaos.com.br", SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/employees/{id}/enable (UC-039) — rota "enable" chama Employee.activate()
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — INACTIVE ativa e o trigger sincroniza STATUS=ACTIVE (204 + GET)")
    void enable_seededInactive_returns204AndSyncsStatusViaTrigger() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE, "Retorno de licença")))
                .andExpect(status().isNoContent());

        assertThat(employeeQueryRepository.findById(SEEDED_EMPLOYEE_ID)).get()
                .extracting("status").isEqualTo(StatusEmployee.ACTIVE);
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — id inexistente retorna 404 SCOS_EMPLOYEE_014")
    void enable_notFound_returns404() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — ACTIVE (não INACTIVE) retorna 422 SCOS_EMPLOYEE_001")
    void enable_seededActive_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_STATUS_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — motivo de ativação inativo retorna 422 SCOS_EMPLOYEE_008")
    void enable_reasonInactive_returns422() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_INACTIVE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVATE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — motivo com entityType=COMPANY retorna 422 SCOS_EMPLOYEE_009")
    void enable_reasonIncompatible_returns422() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_COMPANY_SCOPED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVATE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — sem token retorna 401")
    void enable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/enable — sem permissão retorna 403")
    void enable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/enable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ACTIVATE_EMPLOYEE_NEW_HIRE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/employees/{id}/disable (UC-040) — rota "disable" chama Employee.inactivate()
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — ACTIVE inativa e o trigger sincroniza STATUS=INACTIVE (204 + GET)")
    void disable_seededActive_returns204AndSyncsStatusViaTrigger() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_RESIGNATION, "Desligamento")))
                .andExpect(status().isNoContent());

        assertThat(employeeQueryRepository.findById(SEEDED_EMPLOYEE_ID)).get()
                .extracting("status").isEqualTo(StatusEmployee.INACTIVE);
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — id inexistente retorna 404 SCOS_EMPLOYEE_014")
    void disable_notFound_returns404() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_RESIGNATION, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — já INACTIVE retorna 422 SCOS_EMPLOYEE_001")
    void disable_seededInactive_returns422() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_RESIGNATION, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_STATUS_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — motivo de inativação inativo retorna 422 SCOS_EMPLOYEE_015")
    void disable_reasonInactive_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_INACTIVE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_INACTIVATE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — motivo com entityType=COMPANY retorna 422 SCOS_EMPLOYEE_016")
    void disable_reasonIncompatible_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_COMPANY_SCOPED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_INACTIVATE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — sem token retorna 401")
    void disable_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_RESIGNATION, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/disable — sem permissão retorna 403")
    void disable_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/disable", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_EMPLOYEE_RESIGNATION, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/employees/{id}/block (UC-043) — rota "block" chama Employee.disable()
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — ACTIVE bloqueia e o trigger sincroniza STATUS=DISABLED (204 + GET)")
    void block_seededActive_returns204AndSyncsStatusViaTrigger() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_UNDER_AUDIT, "Suspensão em auditoria")))
                .andExpect(status().isNoContent());

        assertThat(employeeQueryRepository.findById(SEEDED_EMPLOYEE_ID)).get()
                .extracting("status").isEqualTo(StatusEmployee.DISABLED);
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — id inexistente retorna 404 SCOS_EMPLOYEE_014")
    void block_notFound_returns404() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_UNDER_AUDIT, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — INACTIVE (não ACTIVE) retorna 422 SCOS_EMPLOYEE_001")
    void block_seededInactive_returns422() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_UNDER_AUDIT, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_STATUS_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — motivo de bloqueio inativo retorna 422 SCOS_EMPLOYEE_017")
    void block_reasonInactive_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_INACTIVE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_DISABLE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — motivo com entityType=COMPANY retorna 422 SCOS_EMPLOYEE_018")
    void block_reasonIncompatible_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_COMPANY_SCOPED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_DISABLE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — sem token retorna 401")
    void block_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_UNDER_AUDIT, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/block — sem permissão retorna 403")
    void block_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/block", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_DISABLE_EMPLOYEE_UNDER_AUDIT, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // PUT /v1/employees/{id}/unblock (UC-139) — rota "unblock" chama Employee.enable()
    // =====================================================================================

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — DISABLED desbloqueia e o trigger sincroniza STATUS=ACTIVE (204 + GET)")
    void unblock_seededDisabled_returns204AndSyncsStatusViaTrigger() throws Exception {
        transition("block", REASON_DISABLE_EMPLOYEE_UNDER_AUDIT);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, "Auditoria concluída")))
                .andExpect(status().isNoContent());

        assertThat(employeeQueryRepository.findById(SEEDED_EMPLOYEE_ID)).get()
                .extracting("status").isEqualTo(StatusEmployee.ACTIVE);
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — id inexistente retorna 404 SCOS_EMPLOYEE_014")
    void unblock_notFound_returns404() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", NONEXISTENT_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — ACTIVE (não DISABLED) retorna 422 SCOS_EMPLOYEE_001")
    void unblock_seededActive_returns422() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_STATUS_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — INACTIVE (não DISABLED) retorna 422 SCOS_EMPLOYEE_001")
    void unblock_seededInactive_returns422() throws Exception {
        transition("disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_STATUS_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — motivo de desbloqueio inativo retorna 422 SCOS_EMPLOYEE_019")
    void unblock_reasonInactive_returns422() throws Exception {
        transition("block", REASON_DISABLE_EMPLOYEE_UNDER_AUDIT);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_INACTIVE, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ENABLE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — motivo com entityType=COMPANY retorna 422 SCOS_EMPLOYEE_020")
    void unblock_reasonIncompatible_returns422() throws Exception {
        transition("block", REASON_DISABLE_EMPLOYEE_UNDER_AUDIT);

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_COMPANY_SCOPED, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ENABLE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — sem token retorna 401")
    void unblock_withoutToken_returns401() throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /v1/employees/{id}/unblock — sem permissão retorna 403")
    void unblock_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/unblock", SEEDED_EMPLOYEE_ID)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_ENABLE_EMPLOYEE_AUDIT_CLEARED, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // POST /v1/employees/rehire (UC-041) — recontratação de Funcionário INACTIVE
    // =====================================================================================

    @Test
    @DisplayName("POST /v1/employees/rehire — INACTIVE recontrata (200), reatribui vínculos e fecha a posição anterior")
    void rehire_seededInactive_returns200AndClosesPreviousPositionHistory() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_HAPPY_PATH, "rehire.happy@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_HAPPY_PATH, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(employeeId))
                .andExpect(jsonPath("$.data.taxIdentifier").value(CPF_REHIRE_HAPPY_PATH))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.contractType").value("CLT"))
                .andExpect(jsonPath("$.data.company.id").value(SEEDED_COMPANY_ID))
                .andExpect(jsonPath("$.data.position.id").value(SEEDED_POSITION_ID))
                .andExpect(jsonPath("$.data.supervisor").doesNotExist());

        assertThat(employeeQueryRepository.findById(employeeId)).get().extracting("status").isEqualTo(StatusEmployee.ACTIVE);

        Iterable<EmployeePositionHistory> positionHistories = employeePositionHistoryRepository.findAll();
        // linha original (da criação) deve estar fechada pelo trigger trg_close_previous_position
        assertThat(positionHistories).anyMatch(h -> h.getEmployee().getId().equals(employeeId) && h.getEndDate() != null);
        // nova linha, aberta, com o reasonPositionChange informado no rehire
        assertThat(positionHistories).anyMatch(h -> h.getEmployee().getId().equals(employeeId) && h.getEndDate() == null
                && h.getReasonPositionChange().getId().equals(REASON_POSITION_CHANGE_TRANSFER));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — CPF de Funcionário ACTIVE retorna 404 SCOS_EMPLOYEE_021")
    void rehire_activeCpf_returns404() throws Exception {
        createEmployee(CPF_REHIRE_STILL_ACTIVE, "rehire.active@sawcunhaos.com.br");

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_STILL_ACTIVE, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REHIRE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — CPF de Funcionário DISABLED retorna 404 SCOS_EMPLOYEE_021")
    void rehire_disabledCpf_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_DISABLED, "rehire.disabled@sawcunhaos.com.br");
        transitionEmployee(employeeId, "block", REASON_DISABLE_EMPLOYEE_UNDER_AUDIT);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_DISABLED, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REHIRE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — CPF nunca cadastrado retorna 404 SCOS_EMPLOYEE_021")
    void rehire_neverRegisteredCpf_returns404() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_NEVER_REGISTERED, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REHIRE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — companyId inexistente retorna 404 SCOS_COMPANY_001")
    void rehire_companyNotFound_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_COMPANY_NOT_FOUND, "rehire.companynotfound@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_COMPANY_NOT_FOUND, NONEXISTENT_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — positionId inexistente retorna 404 SCOS_POSITION_001")
    void rehire_positionNotFound_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_POSITION_NOT_FOUND, "rehire.positionnotfound@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_POSITION_NOT_FOUND, SEEDED_COMPANY_ID, NONEXISTENT_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_POSITION_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — reasonActivateId inexistente retorna 404 SCOS_REASON_ACTIVATE_001")
    void rehire_reasonActivateNotFound_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_REASON_ACTIVATE_NOT_FOUND, "rehire.reasonactivatenotfound@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_REASON_ACTIVATE_NOT_FOUND, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                NONEXISTENT_ID, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REASON_ACTIVATE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — reasonPositionChangeId inexistente retorna 404 SCOS_EMPLOYEE_022")
    void rehire_reasonPositionChangeNotFound_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_REASON_POSITION_CHANGE_NOT_FOUND, "rehire.reasonposchangenotfound@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_REASON_POSITION_CHANGE_NOT_FOUND, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, NONEXISTENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_REASON_POSITION_CHANGE_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — supervisorId inexistente retorna 404 SCOS_EMPLOYEE_004")
    void rehire_supervisorNotFound_returns404() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_SUPERVISOR_NOT_FOUND, "rehire.supervisornotfound@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_SUPERVISOR_NOT_FOUND, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, NONEXISTENT_ID,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(CODE_SUPERVISOR_NOT_FOUND));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — companyId inativo retorna 422 SCOS_EMPLOYEE_005")
    void rehire_companyInactive_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_COMPANY_INACTIVE, "rehire.companyinactive@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);
        long inactiveCompanyId = createCompany(CNPJ_REHIRE_COMPANY_INACTIVE);
        disableCompany(inactiveCompanyId);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_COMPANY_INACTIVE, inactiveCompanyId, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_COMPANY_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — positionId inativo retorna 422 SCOS_EMPLOYEE_006")
    void rehire_positionInactive_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_POSITION_INACTIVE, "rehire.positioninactive@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);
        long inactivePositionId = createPosition("REHIRE_POS_INAT");
        disablePosition(inactivePositionId);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_POSITION_INACTIVE, SEEDED_COMPANY_ID, inactivePositionId, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_POSITION_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — supervisorId não ACTIVE retorna 422 SCOS_EMPLOYEE_007")
    void rehire_supervisorInactive_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_SUPERVISOR_INACTIVE, "rehire.supervisorinactive@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);
        long supervisorId = createEmployee(CPF_REHIRE_SUPERVISOR_INACTIVE_SUPERVISOR, "rehire.supervisor@sawcunhaos.com.br");
        transitionEmployee(supervisorId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_SUPERVISOR_INACTIVE, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, supervisorId,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_SUPERVISOR_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — motivo de ativação inativo retorna 422 SCOS_EMPLOYEE_008")
    void rehire_reasonActivateInactive_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_REASON_ACTIVATE_INACTIVE, "rehire.reasonactivateinactive@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_REASON_ACTIVATE_INACTIVE, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_INACTIVE, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVATE_REASON_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — motivo de ativação com entityType=COMPANY retorna 422 SCOS_EMPLOYEE_009")
    void rehire_reasonActivateIncompatible_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_REASON_ACTIVATE_INCOMPATIBLE, "rehire.reasonactivateincompatible@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_REASON_ACTIVATE_INCOMPATIBLE, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_COMPANY_SCOPED, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_ACTIVATE_REASON_INCOMPATIBLE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — motivo de mudança de cargo inativo retorna 422 SCOS_EMPLOYEE_023")
    void rehire_reasonPositionChangeInactive_returns422() throws Exception {
        long employeeId = createEmployee(CPF_REHIRE_REASON_POSITION_CHANGE_INACTIVE, "rehire.reasonposchangeinactive@sawcunhaos.com.br");
        transitionEmployee(employeeId, "disable", REASON_INACTIVATE_EMPLOYEE_RESIGNATION);

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_REASON_POSITION_CHANGE_INACTIVE, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_INACTIVE)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value(CODE_REASON_POSITION_CHANGE_INACTIVE));
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — sem token retorna 401")
    void rehire_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, null, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_NO_TOKEN, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/employees/rehire — sem permissão retorna 403")
    void rehire_withoutPermission_returns403() throws Exception {
        stubValidateAuthorityWithoutPermissions();

        mockMvc.perform(post(EMPLOYEES_URI + "/rehire")
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rehireBody(CPF_REHIRE_NO_PERMISSION, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, null,
                                REASON_ACTIVATE_EMPLOYEE_REINSTATEMENT, REASON_POSITION_CHANGE_TRANSFER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(CODE_ACCESS_DENIED));
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static String createBody(String taxIdentifier, String email, long companyId, long positionId, long reasonActivateId) {
        return """
                {
                  "name": "Funcionário Teste",
                  "nameTreatment": "Teste",
                  "taxIdentifier": "%s",
                  "email": "%s",
                  "birthDate": "2000-01-01",
                  "dateOfHiring": "2026-08-01",
                  "contractType": "CLT",
                  "companyId": %d,
                  "positionId": %d,
                  "reasonActivateId": %d
                }
                """.formatted(taxIdentifier, email, companyId, positionId, reasonActivateId);
    }

    private static String statusTransitionBody(long reasonId, String observation) {
        String observationField = observation == null ? "" : "  \"observation\": \"" + observation + "\",\n";
        return """
                {
                %s  "reasonId": %d
                }
                """.formatted(observationField, reasonId);
    }

    /**
     * Aplica uma transição de status auxiliar (setup de precondição) no Funcionário seed, esperando 204.
     * A {@code observation} carrega um nonce (timestamp) para não colidir com o cache de idempotência
     * (Redis, {@code x-jdempotentrequestpayload}) de outra chamada igual feita por outro teste/precondição.
     */
    private void transition(String route, long reasonId) throws Exception {
        transitionEmployee(SEEDED_EMPLOYEE_ID, route, reasonId);
    }

    private void transitionEmployee(long employeeId, String route, long reasonId) throws Exception {
        mockMvc.perform(put(EMPLOYEES_URI + "/{id}/" + route, employeeId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(reasonId, "setup-" + route + "-" + employeeId + "-" + System.nanoTime())))
                .andExpect(status().isNoContent());
    }

    private long createEmployee(String taxIdentifier, String email) throws Exception {
        String response = mockMvc.perform(post(EMPLOYEES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(taxIdentifier, email, SEEDED_COMPANY_ID, SEEDED_POSITION_ID, REASON_ACTIVATE_EMPLOYEE_NEW_HIRE)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    private long createCompany(String taxIdentifier) throws Exception {
        String response = mockMvc.perform(post(COMPANIES_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Empresa Rehire Teste",
                                  "nameTreatment": "Teste",
                                  "taxIdentifier": "%s",
                                  "foundationDate": "2020-01-01",
                                  "sectorOfActivity": "Tecnologia da Informação",
                                  "reasonActivateId": %d
                                }
                                """.formatted(taxIdentifier, REASON_ACTIVATE_COMPANY_SCOPED)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    private void disableCompany(long companyId) throws Exception {
        mockMvc.perform(put(COMPANIES_URI + "/{id}/disable", companyId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusTransitionBody(REASON_INACTIVATE_COMPANY_SCOPED, "setup-disable-company-" + companyId)))
                .andExpect(status().isNoContent());
    }

    private long createPosition(String code) throws Exception {
        String response = mockMvc.perform(post(POSITIONS_URI)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "description": "Cargo Rehire Teste",
                                  "departmentId": %d
                                }
                                """.formatted(code, SEEDED_DEPARTMENT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();
    }

    private void disablePosition(long positionId) throws Exception {
        mockMvc.perform(put(POSITIONS_URI + "/{id}/disable", positionId)
                        .headers(httpHeaders(LANGUAGE_PT, BEAR_TOKEN_VALID, MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(status().isNoContent());
    }

    private static String rehireBody(String taxIdentifier, long companyId, long positionId, Long supervisorId,
                                      long reasonActivateId, long reasonPositionChangeId) {
        String supervisorField = supervisorId == null ? "" : "  \"supervisorId\": " + supervisorId + ",\n";
        return """
                {
                  "taxIdentifier": "%s",
                  "companyId": %d,
                  "positionId": %d,
                %s  "contractType": "CLT",
                  "reasonActivateId": %d,
                  "reasonPositionChangeId": %d
                }
                """.formatted(taxIdentifier, companyId, positionId, supervisorField, reasonActivateId, reasonPositionChangeId);
    }
}
