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
import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.EmployeeStatus;
import br.com.sawcunhaos.organization.api.dto.GetAllEmployeesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindAllEmployeeUseCaseBean}: mapeamento de filtros, paginação e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private FindAllEmployeeUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder()
                .page(1).sizePerPage(10).direction(Direction.ASC).order("name").build();
    }

    private EmployeeOutput employeeOutput(long id) {
        return EmployeeOutput.builder()
                .id(id).name("Funcionário " + id).nameTreatment("Sr.").email("f" + id + "@sawcunhaos.com.br")
                .status(StatusEmployee.ACTIVE).build();
    }

    @Test
    @DisplayName("repassa companyId/positionId/status ao service.findAll e mapeia a resposta resumida")
    void executes_mapsFiltersAndReturnsSummarizedPage() {
        given(employeeService.findAll(eq(1L), eq(2L), eq(StatusEmployee.ACTIVE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(employeeOutput(10L))));

        GetAllEmployeesResponse response = useCase.execute(filter(), 1L, 2L, EmployeeStatus.ACTIVE);

        then(employeeService).should().findAll(eq(1L), eq(2L), eq(StatusEmployee.ACTIVE), any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().id()).isEqualTo(10L);
        assertThat(response.data().getFirst().status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.paginatedDTO()).isNotNull();
        assertThat(response.paginatedDTO().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("filtros todos nulos chama o service com (null, null, null, pageable)")
    void executes_withAllFiltersNull_callsServiceWithNulls() {
        given(employeeService.findAll(eq(null), eq(null), eq(null), any(Pageable.class)))
                .willReturn(Page.empty());

        GetAllEmployeesResponse response = useCase.execute(filter(), null, null, null);

        then(employeeService).should().findAll(eq(null), eq(null), eq(null), any(Pageable.class));
        assertThat(response.data()).isEmpty();
        assertThat(response.paginatedDTO()).isNotNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(employeeService.findAll(any(), any(), any(), any(Pageable.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(filter(), 1L, null, null))
                .isInstanceOf(ScosException.class);
    }
}
