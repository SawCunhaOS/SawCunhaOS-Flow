
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
import br.com.sawcunhaos.organization.api.dto.EmployeeContractType;
import br.com.sawcunhaos.organization.api.dto.RehireEmployeeRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.RehireEmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link RehireEmployeeUseCaseBean}: mapeamento request→input, montagem da resposta aninhada e propagação. */
@ExtendWith(MockitoExtension.class)
class RehireEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private CompanyService companyService;
    @Mock
    private PositionService positionService;

    @InjectMocks
    private RehireEmployeeUseCaseBean useCase;

    private RehireEmployeeRequest.RehireEmployeeRequestBuilder validRequest() {
        return RehireEmployeeRequest.builder()
                .taxIdentifier("11144477735")
                .companyId(1L)
                .positionId(2L)
                .contractType(EmployeeContractType.CLT)
                .reasonActivateId(3L)
                .reasonPositionChangeId(4L);
    }

    private EmployeeOutput employeeOutput(Long supervisorId) {
        return EmployeeOutput.builder()
                .id(1L)
                .name("Funcionário Recontratado")
                .status(StatusEmployee.ACTIVE)
                .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.CLT)
                .companyId(1L)
                .positionId(2L)
                .supervisorId(supervisorId)
                .build();
    }

    @Test
    @DisplayName("mapeia request para input e monta a resposta com company/position/supervisor")
    void executes_mapsRequestToInputAndAssemblesResponse() {
        EmployeeOutput output = employeeOutput(9L);
        given(employeeService.rehire(any(RehireEmployeeInput.class))).willReturn(output);
        given(companyService.findById(1L)).willReturn(CompanyOutput.builder().id(1L).name("Empresa").build());
        given(positionService.findById(2L)).willReturn(PositionOutput.builder().id(2L).code("DEV").description("Dev").build());
        given(employeeService.findById(9L)).willReturn(EmployeeOutput.builder().id(9L).name("Supervisor").build());

        Employee result = useCase.execute(validRequest().supervisorId(9L).probationEndDate(LocalDate.of(2027, 1, 1)).build());

        ArgumentCaptor<RehireEmployeeInput> captor = ArgumentCaptor.forClass(RehireEmployeeInput.class);
        then(employeeService).should().rehire(captor.capture());
        RehireEmployeeInput input = captor.getValue();
        assertThat(input.taxIdentifier()).isEqualTo("11144477735");
        assertThat(input.companyId()).isEqualTo(1L);
        assertThat(input.positionId()).isEqualTo(2L);
        assertThat(input.supervisorId()).isEqualTo(9L);
        assertThat(input.contractType().name()).isEqualTo("CLT");
        assertThat(input.reasonActivateId()).isEqualTo(3L);
        assertThat(input.reasonPositionChangeId()).isEqualTo(4L);
        assertThat(input.probationEndDate()).isEqualTo(LocalDate.of(2027, 1, 1));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.company().id()).isEqualTo(1L);
        assertThat(result.position().id()).isEqualTo(2L);
        assertThat(result.supervisor().id()).isEqualTo(9L);
    }

    @Test
    @DisplayName("supervisorId nulo não chama employeeService.findById e supervisor fica nulo na resposta")
    void executes_withoutSupervisorId_doesNotCallFindByIdAndSupervisorIsNull() {
        EmployeeOutput output = employeeOutput(null);
        given(employeeService.rehire(any(RehireEmployeeInput.class))).willReturn(output);
        given(companyService.findById(1L)).willReturn(CompanyOutput.builder().id(1L).name("Empresa").build());
        given(positionService.findById(2L)).willReturn(PositionOutput.builder().id(2L).code("DEV").description("Dev").build());

        Employee result = useCase.execute(validRequest().build());

        then(employeeService).should(org.mockito.Mockito.never()).findById(any());
        assertThat(result.supervisor()).isNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(employeeService).rehire(any(RehireEmployeeInput.class));

        assertThatThrownBy(() -> useCase.execute(validRequest().build())).isInstanceOf(ScosException.class);
    }
}
