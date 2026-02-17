
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

package br.com.sawcunhaos.organization.application.usecase.position;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.mapper.position.PositionMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.service.department.PositionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePositionUseCase Tests")
class DeletePositionUseCaseTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionDomainService positionDomainService;

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private DeletePositionUseCase deletePositionUseCase;

    private Long validPositionId;

    @BeforeEach
    void setUp() {
        validPositionId = 1L;
    }

    @Test
    @DisplayName("Should delete position successfully when no employees are linked")
    void shouldDeletePositionSuccessfullyWhenNoEmployeesAreLinked() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validPositionId);
        doNothing().when(positionDomainService).validatePositionLinkedToEmployeeValidation(validPositionId);

        // When
        Void result = deletePositionUseCase.execute(validPositionId);

        // Then
        assertThat(result).isNull();

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validPositionId);
        verify(positionDomainService, times(1)).validatePositionLinkedToEmployeeValidation(validPositionId);
        verify(positionRepository, times(1)).deleteById(validPositionId);
    }

    @Test
    @DisplayName("Should throw exception when position does not exist")
    void shouldThrowExceptionWhenPositionDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_001))
                .when(positionDomainService)
                .validatePositionExistsValidation(validPositionId);

        // When / Then
        assertThatThrownBy(() -> deletePositionUseCase.execute(validPositionId))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validPositionId);
        verify(positionDomainService, never()).validatePositionLinkedToEmployeeValidation(validPositionId);
        verify(positionRepository, never()).deleteById(validPositionId);
    }

    @Test
    @DisplayName("Should throw exception when position has linked employees")
    void shouldThrowExceptionWhenPositionHasLinkedEmployees() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validPositionId);
        doThrow(new ScosException(ExceptionCodeError.SCOS_POSITION_003))
                .when(positionDomainService)
                .validatePositionLinkedToEmployeeValidation(validPositionId);

        // When / Then
        assertThatThrownBy(() -> deletePositionUseCase.execute(validPositionId))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_003.getCode());

        verify(positionDomainService, times(1)).validatePositionExistsValidation(validPositionId);
        verify(positionDomainService, times(1)).validatePositionLinkedToEmployeeValidation(validPositionId);
        verify(positionRepository, never()).deleteById(validPositionId);
    }

    @Test
    @DisplayName("Should validate position existence before checking employees")
    void shouldValidatePositionExistenceBeforeCheckingEmployees() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validPositionId);
        doNothing().when(positionDomainService).validatePositionLinkedToEmployeeValidation(validPositionId);

        // When
        deletePositionUseCase.execute(validPositionId);

        // Then - verify order of validations
        var inOrder = inOrder(positionDomainService, positionRepository);
        inOrder.verify(positionDomainService).validatePositionExistsValidation(validPositionId);
        inOrder.verify(positionDomainService).validatePositionLinkedToEmployeeValidation(validPositionId);
        inOrder.verify(positionRepository).deleteById(validPositionId);
    }

    @Test
    @DisplayName("Should handle repository deletion failure")
    void shouldHandleRepositoryDeletionFailure() {
        // Given
        doNothing().when(positionDomainService).validatePositionExistsValidation(validPositionId);
        doNothing().when(positionDomainService).validatePositionLinkedToEmployeeValidation(validPositionId);
        doThrow(new RuntimeException("Database error"))
                .when(positionRepository)
                .deleteById(validPositionId);

        // When / Then
        assertThatThrownBy(() -> deletePositionUseCase.execute(validPositionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(positionRepository, times(1)).deleteById(validPositionId);
    }

}

