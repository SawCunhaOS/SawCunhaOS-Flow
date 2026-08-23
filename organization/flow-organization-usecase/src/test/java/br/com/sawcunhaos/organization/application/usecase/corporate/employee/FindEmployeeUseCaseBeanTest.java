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

package br.com.sawcunhaos.organization.application.usecase.corporate.employee;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Employee;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** Testes de {@link FindEmployeeUseCaseBean}: composição company/position/supervisor e propagação. */
@ExtendWith(MockitoExtension.class)
class FindEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private CompanyService companyService;
    @Mock
    private PositionService positionService;

    @InjectMocks
    private FindEmployeeUseCaseBean useCase;

    private EmployeeOutput employeeOutput(Long supervisorId) {
        return EmployeeOutput.builder()
                .id(1L)
                .name("Funcionário")
                .status(StatusEmployee.ACTIVE)
                .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.CLT)
                .companyId(1L)
                .positionId(2L)
                .supervisorId(supervisorId)
                .build();
    }

    @Test
    @DisplayName("busca employee/company/position/supervisor e monta o Employee completo")
    void executes_withSupervisor_assemblesCompleteEmployee() {
        given(employeeService.findById(1L)).willReturn(employeeOutput(9L));
        given(companyService.findById(1L)).willReturn(CompanyOutput.builder().id(1L).name("Empresa").build());
        given(positionService.findById(2L)).willReturn(PositionOutput.builder().id(2L).code("DEV").description("Dev").build());
        given(employeeService.findById(9L)).willReturn(EmployeeOutput.builder().id(9L).name("Supervisor").build());

        Employee result = useCase.execute(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.company().id()).isEqualTo(1L);
        assertThat(result.position().id()).isEqualTo(2L);
        assertThat(result.supervisor().id()).isEqualTo(9L);
    }

    @Test
    @DisplayName("supervisorId nulo não chama employeeService.findById novamente e supervisor fica nulo")
    void executes_withoutSupervisor_doesNotCallFindByIdAgainAndSupervisorIsNull() {
        given(employeeService.findById(1L)).willReturn(employeeOutput(null));
        given(companyService.findById(1L)).willReturn(CompanyOutput.builder().id(1L).name("Empresa").build());
        given(positionService.findById(2L)).willReturn(PositionOutput.builder().id(2L).code("DEV").description("Dev").build());

        Employee result = useCase.execute(1L);

        then(employeeService).should(never()).findById(9L);
        assertThat(result.supervisor()).isNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha (SCOS_EMPLOYEE_014)")
    void whenServiceThrows_propagatesScosException() {
        given(employeeService.findById(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(99L)).isInstanceOf(ScosException.class);
    }
}
