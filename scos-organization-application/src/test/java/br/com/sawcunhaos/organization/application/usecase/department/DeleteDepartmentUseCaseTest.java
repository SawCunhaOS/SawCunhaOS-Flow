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

package br.com.sawcunhaos.organization.application.usecase.department;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
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
@DisplayName("DeleteDepartmentUseCase Tests")
class DeleteDepartmentUseCaseTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentDomainService departmentDomainService;

    @InjectMocks
    private DeleteDepartmentUseCase deleteDepartmentUseCase;

    private Long departmentId;

    @BeforeEach
    void setUp() {
        departmentId = 1L;
    }

    @Test
    @DisplayName("Should delete department successfully when valid")
    void shouldDeleteDepartmentSuccessfullyWhenValid() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);

        // When
        Void result = deleteDepartmentUseCase.execute(departmentId);

        // Then
        assertThat(result).isNull();
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(departmentId);
        verify(departmentDomainService, times(1)).validateDepartmentLinkedToPositionValidation(departmentId);
        verify(departmentRepository, times(1)).deleteById(departmentId);
    }

    @Test
    @DisplayName("Should throw exception when department does not exist")
    void shouldThrowExceptionWhenDepartmentDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001))
                .when(departmentDomainService)
                .validateDepartmentExistsValidation(departmentId);

        // When / Then
        assertThatThrownBy(() -> deleteDepartmentUseCase.execute(departmentId))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        // Verify that further validations and deletion did not occur
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(departmentId);
        verify(departmentDomainService, never()).validateDepartmentLinkedToPositionValidation(departmentId);
        verify(departmentRepository, never()).deleteById(departmentId);
    }

    @Test
    @DisplayName("Should throw exception when department is linked to positions")
    void shouldThrowExceptionWhenDepartmentIsLinkedToPositions() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_003))
                .when(departmentDomainService)
                .validateDepartmentLinkedToPositionValidation(departmentId);

        // When / Then
        assertThatThrownBy(() -> deleteDepartmentUseCase.execute(departmentId))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_003.getCode());

        // Verify order and that deletion did not occur
        var inOrder = inOrder(departmentDomainService);
        inOrder.verify(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        inOrder.verify(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);

        verify(departmentRepository, never()).deleteById(departmentId);
    }

    @Test
    @DisplayName("Should call validations in correct order")
    void shouldCallValidationsInCorrectOrder() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);

        // When
        deleteDepartmentUseCase.execute(departmentId);

        // Then - verify exact order
        var inOrder = inOrder(departmentDomainService, departmentRepository);
        inOrder.verify(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        inOrder.verify(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);
        inOrder.verify(departmentRepository).deleteById(departmentId);
    }

    @Test
    @DisplayName("Should call repository deleteById exactly once")
    void shouldCallRepositoryDeleteByIdExactlyOnce() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);

        // When
        deleteDepartmentUseCase.execute(departmentId);

        // Then
        verify(departmentRepository, times(1)).deleteById(departmentId);
    }

    @Test
    @DisplayName("Should verify only deleteById is called on repository")
    void shouldVerifyOnlyDeleteByIdIsCalledOnRepository() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(departmentId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(departmentId);

        // When
        deleteDepartmentUseCase.execute(departmentId);

        // Then
        verify(departmentRepository, times(1)).deleteById(departmentId);
        // Verify validations occurred before delete
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(departmentId);
        verify(departmentDomainService, times(1)).validateDepartmentLinkedToPositionValidation(departmentId);
    }

    @Test
    @DisplayName("Should validate preconditions before deletion")
    void shouldValidatePreconditionsBeforeDeletion() {
        // Given
        Long testDepartmentId = 99L;
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(testDepartmentId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(testDepartmentId);

        // When
        deleteDepartmentUseCase.execute(testDepartmentId);

        // Then - validations must happen before deletion
        var inOrder = inOrder(departmentDomainService, departmentRepository);
        inOrder.verify(departmentDomainService).validateDepartmentExistsValidation(testDepartmentId);
        inOrder.verify(departmentDomainService).validateDepartmentLinkedToPositionValidation(testDepartmentId);
        inOrder.verify(departmentRepository).deleteById(testDepartmentId);
    }

    @Test
    @DisplayName("Should handle deletion of different department IDs")
    void shouldHandleDeletionOfDifferentDepartmentIds() {
        // Given - First department
        Long firstDeptId = 1L;
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(firstDeptId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(firstDeptId);

        // When - Delete first department
        Void firstResult = deleteDepartmentUseCase.execute(firstDeptId);

        // Then
        assertThat(firstResult).isNull();
        verify(departmentRepository, times(1)).deleteById(firstDeptId);

        // Reset mocks
        reset(departmentDomainService, departmentRepository);

        // Given - Second department
        Long secondDeptId = 2L;
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(secondDeptId);
        doNothing().when(departmentDomainService).validateDepartmentLinkedToPositionValidation(secondDeptId);

        // When - Delete second department
        Void secondResult = deleteDepartmentUseCase.execute(secondDeptId);

        // Then
        assertThat(secondResult).isNull();
        verify(departmentRepository, times(1)).deleteById(secondDeptId);
    }
}
