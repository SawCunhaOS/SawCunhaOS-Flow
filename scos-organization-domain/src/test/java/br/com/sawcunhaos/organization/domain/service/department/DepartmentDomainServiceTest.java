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
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentDomainService Unit Tests")
class DepartmentDomainServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private DepartmentDomainService departmentDomainService;

    private static final String VALID_DEPARTMENT_CODE = "DEPT001";
    private static final Long VALID_DEPARTMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        // Setup comum se necessário
    }

    @Test
    @DisplayName("Should validate successfully when department code does not exist")
    void shouldValidateSuccessfullyWhenDepartmentCodeDoesNotExist() {
        // Given
        when(departmentRepository.existsByCode(VALID_DEPARTMENT_CODE)).thenReturn(false);

        // When / Then
        assertThatCode(() -> departmentDomainService.validateDepartmentCodeExistsValidation(VALID_DEPARTMENT_CODE))
                .doesNotThrowAnyException();

        verify(departmentRepository, times(1)).existsByCode(VALID_DEPARTMENT_CODE);
    }

    @Test
    @DisplayName("Should throw ScosException when department code already exists")
    void shouldThrowScosExceptionWhenDepartmentCodeAlreadyExists() {
        // Given
        when(departmentRepository.existsByCode(VALID_DEPARTMENT_CODE)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentCodeExistsValidation(VALID_DEPARTMENT_CODE))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_002.getCode());

        verify(departmentRepository, times(1)).existsByCode(VALID_DEPARTMENT_CODE);
    }

    @Test
    @DisplayName("Should validate successfully when department is not linked to any position")
    void shouldValidateSuccessfullyWhenDepartmentIsNotLinkedToPosition() {
        // Given
        when(positionRepository.existsByDepartmentId(VALID_DEPARTMENT_ID)).thenReturn(false);

        // When / Then
        assertThatCode(() -> departmentDomainService.validateDepartmentLinkedToPositionValidation(VALID_DEPARTMENT_ID))
                .doesNotThrowAnyException();

        verify(positionRepository, times(1)).existsByDepartmentId(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when department is linked to positions")
    void shouldThrowScosExceptionWhenDepartmentIsLinkedToPositions() {
        // Given
        when(positionRepository.existsByDepartmentId(VALID_DEPARTMENT_ID)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentLinkedToPositionValidation(VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_003.getCode());

        verify(positionRepository, times(1)).existsByDepartmentId(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should validate successfully when department exists")
    void shouldValidateSuccessfullyWhenDepartmentExists() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

        // When / Then
        assertThatCode(() -> departmentDomainService.validateDepartmentExistsValidation(VALID_DEPARTMENT_ID))
                .doesNotThrowAnyException();

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when department does not exist")
    void shouldThrowScosExceptionWhenDepartmentDoesNotExist() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentExistsValidation(VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should validate successfully when department code is unique for update")
    void shouldValidateSuccessfullyWhenDepartmentCodeIsUniqueForUpdate() {
        // Given
        when(departmentRepository.existsByCodeAndNotId(VALID_DEPARTMENT_ID, VALID_DEPARTMENT_CODE)).thenReturn(false);

        // When / Then
        assertThatCode(() -> departmentDomainService.validateDepartmentCodeUniquenessValidation(VALID_DEPARTMENT_CODE, VALID_DEPARTMENT_ID))
                .doesNotThrowAnyException();

        verify(departmentRepository, times(1)).existsByCodeAndNotId(VALID_DEPARTMENT_ID, VALID_DEPARTMENT_CODE);
    }

    @Test
    @DisplayName("Should throw ScosException when department code is not unique for update")
    void shouldThrowScosExceptionWhenDepartmentCodeIsNotUniqueForUpdate() {
        // Given
        when(departmentRepository.existsByCodeAndNotId(VALID_DEPARTMENT_ID, VALID_DEPARTMENT_CODE)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentCodeUniquenessValidation(VALID_DEPARTMENT_CODE, VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_002.getCode());

        verify(departmentRepository, times(1)).existsByCodeAndNotId(VALID_DEPARTMENT_ID, VALID_DEPARTMENT_CODE);
    }

    // --- validateDepartmentExistsAndActiveValidation ---

    @Test
    @DisplayName("Should validate successfully when department exists and is active")
    void shouldValidateSuccessfullyWhenDepartmentExistsAndIsActive() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);
        when(departmentRepository.existsByIdAndActive(VALID_DEPARTMENT_ID)).thenReturn(true);

        // When / Then
        assertThatCode(() -> departmentDomainService.validateDepartmentExistsAndActiveValidation(VALID_DEPARTMENT_ID))
                .doesNotThrowAnyException();

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
        verify(departmentRepository, times(1)).existsByIdAndActive(VALID_DEPARTMENT_ID);
    }

    @Test
    @DisplayName("Should throw SCOS_DEPARTMENT_001 when department does not exist on active validation")
    void shouldThrowScosDepartment001WhenDepartmentDoesNotExistOnActiveValidation() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentExistsAndActiveValidation(VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
        verify(departmentRepository, never()).existsByIdAndActive(anyLong());
    }

    @Test
    @DisplayName("Should throw SCOS_DEPARTMENT_006 when department exists but is inactive")
    void shouldThrowScosDepartment006WhenDepartmentExistsButIsInactive() {
        // Given
        when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);
        when(departmentRepository.existsByIdAndActive(VALID_DEPARTMENT_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> departmentDomainService.validateDepartmentExistsAndActiveValidation(VALID_DEPARTMENT_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_006.getCode());

        verify(departmentRepository, times(1)).existsById(VALID_DEPARTMENT_ID);
        verify(departmentRepository, times(1)).existsByIdAndActive(VALID_DEPARTMENT_ID);
    }

    // --- Department entity activate() / deactivate() ---

    @Test
    @DisplayName("Should throw SCOS_DEPARTMENT_004 when activating an already active department")
    void shouldThrowScosDepartment004WhenActivatingAlreadyActiveDepartment() {
        // Given
        Department activeDepartment = Department.builder()
                .id(VALID_DEPARTMENT_ID)
                .code(VALID_DEPARTMENT_CODE)
                .description("Test Department")
                .active(true)
                .build();

        // When / Then
        assertThatThrownBy(activeDepartment::activate)
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_004.getCode());
    }

    @Test
    @DisplayName("Should throw SCOS_DEPARTMENT_005 when deactivating an already inactive department")
    void shouldThrowScosDepartment005WhenDeactivatingAlreadyInactiveDepartment() {
        // Given
        Department inactiveDepartment = Department.builder()
                .id(VALID_DEPARTMENT_ID)
                .code(VALID_DEPARTMENT_CODE)
                .description("Test Department")
                .active(false)
                .build();

        // When / Then
        assertThatThrownBy(inactiveDepartment::deactivate)
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_005.getCode());
    }
}

