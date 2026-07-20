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

package br.com.sawcunhaos.organization.application.usecase.corporate.department;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateDepartmentRequest;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link CreateDepartmentUseCaseBean}: mapeamento request→input, montagem da resposta e propagação. */
@ExtendWith(MockitoExtension.class)
class CreateDepartmentUseCaseBeanTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private CreateDepartmentUseCaseBean useCase;

    @Test
    @DisplayName("mapeia request para input (id nulo, inativo) e retorna a resposta com id/active do output")
    void executes_mapsRequestToInputAndReturnsResponse() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("HR", "Human Resources");
        DepartmentOutput output = DepartmentOutput.builder()
                .id(1L).code("HR").description("Human Resources").active(true).build();
        given(departmentService.create(any(DepartmentInput.class))).willReturn(output);

        Department result = useCase.execute(request);

        ArgumentCaptor<DepartmentInput> captor = ArgumentCaptor.forClass(DepartmentInput.class);
        then(departmentService).should().create(captor.capture());
        DepartmentInput input = captor.getValue();
        assertThat(input.id()).isNull();
        assertThat(input.code()).isEqualTo("HR");
        assertThat(input.description()).isEqualTo("Human Resources");
        assertThat(input.active()).isFalse();

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("HR");
        assertThat(result.description()).isEqualTo("Human Resources");
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("HR", "Human Resources");
        given(departmentService.create(any(DepartmentInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(departmentService).shouldHaveNoInteractions();
    }
}
