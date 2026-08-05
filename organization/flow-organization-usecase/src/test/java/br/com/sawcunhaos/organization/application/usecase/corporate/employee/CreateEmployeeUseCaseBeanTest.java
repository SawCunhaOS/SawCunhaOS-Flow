
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeRequest;
import br.com.sawcunhaos.organization.api.dto.EmployeeContractType;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
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

/** Testes de {@link CreateEmployeeUseCaseBean}: mapeamento request→input, montagem da resposta e propagação. */
@ExtendWith(MockitoExtension.class)
class CreateEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private CreateEmployeeUseCaseBean useCase;

    private CreateEmployeeRequest.CreateEmployeeRequestBuilder validRequest() {
        return CreateEmployeeRequest.builder()
                .name("Novo Funcionário")
                .nameTreatment("Novo")
                .taxIdentifier("11144477735")
                .email("novo@sawcunhaos.com.br")
                .birthDate(LocalDate.of(2000, 1, 1))
                .dateOfHiring(LocalDate.of(2026, 8, 1))
                .contractType(EmployeeContractType.CLT)
                .companyId(1L)
                .positionId(2L)
                .reasonActivateId(3L);
    }

    @Test
    @DisplayName("mapeia request para input e retorna o id do funcionário criado")
    void executes_mapsRequestToInputAndReturnsId() {
        EmployeeOutput output = EmployeeOutput.builder().id(42L).build();
        given(employeeService.create(any(EmployeeInput.class))).willReturn(output);

        Long result = useCase.execute(validRequest().supervisorId(9L).probationEndDate(LocalDate.of(2027, 1, 1)).build());

        ArgumentCaptor<EmployeeInput> captor = ArgumentCaptor.forClass(EmployeeInput.class);
        then(employeeService).should().create(captor.capture());
        EmployeeInput input = captor.getValue();
        assertThat(input.name()).isEqualTo("Novo Funcionário");
        assertThat(input.nameTreatment()).isEqualTo("Novo");
        assertThat(input.taxIdentifier()).isEqualTo("11144477735");
        assertThat(input.email()).isEqualTo("novo@sawcunhaos.com.br");
        assertThat(input.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(input.dateOfHiring()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(input.contractType().name()).isEqualTo("CLT");
        assertThat(input.supervisorId()).isEqualTo(9L);
        assertThat(input.companyId()).isEqualTo(1L);
        assertThat(input.positionId()).isEqualTo(2L);
        assertThat(input.reasonActivateId()).isEqualTo(3L);
        assertThat(input.probationEndDate()).isEqualTo(LocalDate.of(2027, 1, 1));

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(employeeService.create(any(EmployeeInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(validRequest().build())).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(employeeService).shouldHaveNoInteractions();
    }
}
