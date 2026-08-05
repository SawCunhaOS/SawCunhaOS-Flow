
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
import br.com.sawcunhaos.organization.api.dto.EmployeeStatusTransitionRequest;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link ActivateEmployeeUseCaseBean}: delegação a {@code employeeService.activate} e propagação. */
@ExtendWith(MockitoExtension.class)
class ActivateEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private ActivateEmployeeUseCaseBean useCase;

    @Test
    @DisplayName("delega id/reasonId/observation ao service.activate")
    void executes_delegatesActivateToService() {
        EmployeeStatusTransitionRequest request = EmployeeStatusTransitionRequest.builder()
                .reasonId(10L)
                .observation("Retorno de licença")
                .build();

        useCase.execute(1L, request);

        then(employeeService).should().activate(1L, 10L, "Retorno de licença");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(employeeService).activate(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(1L, EmployeeStatusTransitionRequest.builder().reasonId(10L).build()))
                .isInstanceOf(ScosException.class);
    }
}
