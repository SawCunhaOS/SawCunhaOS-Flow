
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
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeePositionHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeWorkScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private static final long SEEDED_COMPANY_ID = 1L;
    private static final long SEEDED_POSITION_ID = 1L;
    private static final long REASON_ACTIVATE_EMPLOYEE_NEW_HIRE = 2L;
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

    private static final String CODE_EMPLOYEE_TAX_IDENTIFIER_CONFLICT = "SCOS_EMPLOYEE_002";
    private static final String CODE_EMPLOYEE_EMAIL_CONFLICT = "SCOS_EMPLOYEE_003";
    private static final String CODE_EMPLOYEE_WRONG_EMAIL_DOMAIN = "SCOS_EMPLOYEE_010";
    private static final String CODE_COMPANY_NOT_FOUND = "SCOS_COMPANY_001";
    private static final String CODE_POSITION_NOT_FOUND = "SCOS_POSITION_001";
    private static final String CODE_ACCESS_DENIED = "SCOS-004";

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
}
