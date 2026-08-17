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
import br.com.sawcunhaos.organization.api.dto.UpdateDepartmentRequest;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link UpdateDepartmentUseCaseBean}: mapeamento id+request→input e propagação. */
@ExtendWith(MockitoExtension.class)
class UpdateDepartmentUseCaseBeanTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private UpdateDepartmentUseCaseBean useCase;

    @Test
    @DisplayName("mapeia id + request para input e chama update")
    void executes_mapsIdAndRequestToInputAndCallsUpdate() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR2", "Human Resources 2", null);

        useCase.execute(10L, request);

        ArgumentCaptor<DepartmentInput> captor = ArgumentCaptor.forClass(DepartmentInput.class);
        then(departmentService).should().update(captor.capture());
        DepartmentInput input = captor.getValue();
        assertThat(input.id()).isEqualTo(10L);
        assertThat(input.code()).isEqualTo("HR2");
        assertThat(input.description()).isEqualTo("Human Resources 2");
        assertThat(input.managerId()).isNull();
    }

    @Test
    @DisplayName("mapeia managerId para o input quando informado")
    void executes_mapsManagerIdToInputWhenInformed() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR2", "Human Resources 2", 7L);

        useCase.execute(10L, request);

        ArgumentCaptor<DepartmentInput> captor = ArgumentCaptor.forClass(DepartmentInput.class);
        then(departmentService).should().update(captor.capture());
        assertThat(captor.getValue().managerId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        UpdateDepartmentRequest request = new UpdateDepartmentRequest("HR2", "Human Resources 2", null);
        willThrow(new ScosException()).given(departmentService).update(any(DepartmentInput.class));

        assertThatThrownBy(() -> useCase.execute(10L, request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(10L, null)).isInstanceOf(NullPointerException.class);
        then(departmentService).shouldHaveNoInteractions();
    }
}
