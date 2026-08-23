
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeLoginRequest;
import br.com.sawcunhaos.organization.api.dto.Login;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginInput;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_025;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link CreateEmployeeLoginUseCaseBean}: guarda de Funcionário ACTIVE e mapeamento request→input. */
@ExtendWith(MockitoExtension.class)
class CreateEmployeeLoginUseCaseBeanTest {

    @Mock
    private LoginService loginService;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private CreateEmployeeLoginUseCaseBean useCase;

    private EmployeeOutput employeeOutput(StatusEmployee status) {
        return EmployeeOutput.builder().id(2L).name("John Doe").status(status).build();
    }

    private LoginOutput loginOutput() {
        return LoginOutput.builder().id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profileId(1L).employeeId(2L).build();
    }

    @Test
    @DisplayName("funcionário ACTIVE: cria o Login com type EMPLOYEE e employeeId do path")
    void executes_whenEmployeeIsActive_createsLogin() {
        CreateEmployeeLoginRequest request = CreateEmployeeLoginRequest.builder().login("john.doe").profileId(1L).build();
        given(employeeService.findById(2L)).willReturn(employeeOutput(StatusEmployee.ACTIVE));
        given(loginService.create(any(LoginInput.class))).willReturn(loginOutput());

        Login result = useCase.execute(2L, request);

        ArgumentCaptor<LoginInput> captor = ArgumentCaptor.forClass(LoginInput.class);
        then(loginService).should().create(captor.capture());
        LoginInput input = captor.getValue();
        assertThat(input.login()).isEqualTo("john.doe");
        assertThat(input.profileId()).isEqualTo(1L);
        assertThat(input.type()).isEqualTo(LoginType.EMPLOYEE);
        assertThat(input.employeeId()).isEqualTo(2L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.login()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("funcionário não ACTIVE: lança SCOS_EMPLOYEE_025 e não chama loginService")
    void executes_whenEmployeeIsNotActive_throwsAndDoesNotCreateLogin() {
        CreateEmployeeLoginRequest request = CreateEmployeeLoginRequest.builder().login("john.doe").profileId(1L).build();
        given(employeeService.findById(2L)).willReturn(employeeOutput(StatusEmployee.INACTIVE));

        assertThatThrownBy(() -> useCase.execute(2L, request))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_025.getCode());

        then(loginService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("propaga ScosException quando employeeService falha (ex.: funcionário não encontrado)")
    void whenEmployeeServiceThrows_propagatesScosException() {
        CreateEmployeeLoginRequest request = CreateEmployeeLoginRequest.builder().login("john.doe").profileId(1L).build();
        given(employeeService.findById(999L)).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(999L, request)).isInstanceOf(ScosException.class);

        then(loginService).shouldHaveNoInteractions();
    }
}
