
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
import br.com.sawcunhaos.organization.api.dto.EmployeeStatusTransitionRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link InactivateEmployeeUseCaseBean}: delegação a {@code employeeService.inactivate} e propagação. */
@ExtendWith(MockitoExtension.class)
class InactivateEmployeeUseCaseBeanTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private InactivateEmployeeUseCaseBean useCase;

    @Test
    @DisplayName("delega id/reasonId/observation ao service.inactivate")
    void executes_delegatesInactivateToService() {
        EmployeeStatusTransitionRequest request = EmployeeStatusTransitionRequest.builder()
                .reasonId(20L)
                .observation("Desligamento")
                .build();

        useCase.execute(1L, request);

        then(employeeService).should().inactivate(1L, 20L, "Desligamento", null);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(employeeService).inactivate(any(), any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(1L, EmployeeStatusTransitionRequest.builder().reasonId(20L).build()))
                .isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("repassa expectedReturnDate ao service.inactivate")
    void executes_delegatesExpectedReturnDateToService() {
        LocalDate expectedReturnDate = LocalDate.of(2026, 9, 1);
        EmployeeStatusTransitionRequest request = EmployeeStatusTransitionRequest.builder()
                .reasonId(20L)
                .observation("Férias")
                .expectedReturnDate(expectedReturnDate)
                .build();

        useCase.execute(1L, request);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        then(employeeService).should().inactivate(eq(1L), eq(20L), eq("Férias"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(expectedReturnDate);
    }
}
