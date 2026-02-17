
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

package br.com.sawcunhaos.organization.domain.service.department;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.repository.employee.EmployeeQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionDomainService Unit Tests")
class PositionDomainServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private PositionDomainService positionDomainService;

    private static final Long VALID_POSITION_ID = 1L;
    private static final Long VALID_DEPARTMENT_ID = 1L;
    private static final String VALID_POSITION_CODE = "DEV";

    @BeforeEach
    void setUp() {
        // Default mock behaviors
    }

    @Test
    @DisplayName("Should throw ScosException when position code already exists")
    void shouldThrowScosExceptionWhenPositionCodeAlreadyExists() {
        // Given
        when(positionRepository.existsByCode(VALID_POSITION_CODE)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> positionDomainService.validatePositionCodeExistsValidation(VALID_POSITION_CODE))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_002.getCode());

        verify(positionRepository, times(1)).existsByCode(VALID_POSITION_CODE);
    }

    @Test
    @DisplayName("Should not throw exception when position code does not exist")
    void shouldNotThrowExceptionWhenPositionCodeDoesNotExist() {
        // Given
        when(positionRepository.existsByCode(VALID_POSITION_CODE)).thenReturn(false);

        // When / Then - should not throw
        positionDomainService.validatePositionCodeExistsValidation(VALID_POSITION_CODE);

        verify(positionRepository, times(1)).existsByCode(VALID_POSITION_CODE);
    }

    @Test
    @DisplayName("Should throw ScosException when position does not exist")
    void shouldThrowScosExceptionWhenPositionDoesNotExist() {
        // Given
        when(positionRepository.existsById(VALID_POSITION_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> positionDomainService.validatePositionExistsValidation(VALID_POSITION_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_001.getCode());

        verify(positionRepository, times(1)).existsById(VALID_POSITION_ID);
    }

    @Test
    @DisplayName("Should not throw exception when position exists")
    void shouldNotThrowExceptionWhenPositionExists() {
        // Given
        when(positionRepository.existsById(VALID_POSITION_ID)).thenReturn(true);

        // When / Then - should not throw
        positionDomainService.validatePositionExistsValidation(VALID_POSITION_ID);

        verify(positionRepository, times(1)).existsById(VALID_POSITION_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when position is linked to employees")
    void shouldThrowScosExceptionWhenPositionIsLinkedToEmployees() {
        // Given
        when(employeeQueryRepository.existsByPositionId(VALID_POSITION_ID)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> positionDomainService.validatePositionLinkedToEmployeeValidation(VALID_POSITION_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_003.getCode());

        verify(employeeQueryRepository, times(1)).existsByPositionId(VALID_POSITION_ID);
    }

    @Test
    @DisplayName("Should not throw exception when position has no linked employees")
    void shouldNotThrowExceptionWhenPositionHasNoLinkedEmployees() {
        // Given
        when(employeeQueryRepository.existsByPositionId(VALID_POSITION_ID)).thenReturn(false);

        // When / Then - should not throw
        positionDomainService.validatePositionLinkedToEmployeeValidation(VALID_POSITION_ID);

        verify(employeeQueryRepository, times(1)).existsByPositionId(VALID_POSITION_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when position code is duplicate on update")
    void shouldThrowScosExceptionWhenPositionCodeIsDuplicateOnUpdate() {
        // Given
        when(positionRepository.existsByCodeAndNotId(VALID_POSITION_ID, VALID_POSITION_CODE)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> positionDomainService.validatePositionCodeUniquenessValidation(VALID_POSITION_CODE, VALID_POSITION_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_POSITION_002.getCode());

        verify(positionRepository, times(1)).existsByCodeAndNotId(VALID_POSITION_ID, VALID_POSITION_CODE);
    }

    @Test
    @DisplayName("Should not throw exception when position code is unique on update")
    void shouldNotThrowExceptionWhenPositionCodeIsUniqueOnUpdate() {
        // Given
        when(positionRepository.existsByCodeAndNotId(VALID_POSITION_ID, VALID_POSITION_CODE)).thenReturn(false);

        // When / Then - should not throw
        positionDomainService.validatePositionCodeUniquenessValidation(VALID_POSITION_CODE, VALID_POSITION_ID);

        verify(positionRepository, times(1)).existsByCodeAndNotId(VALID_POSITION_ID, VALID_POSITION_CODE);
    }

    @Test
    @DisplayName("Should throw ScosException when department does not exist")
    void shouldThrowScosExceptionWhenDepartmentDoesNotExist() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> positionDomainService.validateDepartmentExistsValidation(VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should not throw exception when department exists")
    void shouldNotThrowExceptionWhenDepartmentExists() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

        // When / Then - should not throw
        positionDomainService.validateDepartmentExistsValidation(VALID_DEPARTMENT_ID);

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should handle multiple validations in sequence")
    void shouldHandleMultipleValidationsInSequence() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);
        when(positionRepository.existsByCode(VALID_POSITION_CODE)).thenReturn(false);
        when(positionRepository.existsById(VALID_POSITION_ID)).thenReturn(true);

        // When / Then - should not throw
        positionDomainService.validateDepartmentExistsValidation(VALID_DEPARTMENT_ID);
        positionDomainService.validatePositionCodeExistsValidation(VALID_POSITION_CODE);
        positionDomainService.validatePositionExistsValidation(VALID_POSITION_ID);

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
        verify(positionRepository, times(1)).existsByCode(VALID_POSITION_CODE);
        verify(positionRepository, times(1)).existsById(VALID_POSITION_ID);
    }

}

