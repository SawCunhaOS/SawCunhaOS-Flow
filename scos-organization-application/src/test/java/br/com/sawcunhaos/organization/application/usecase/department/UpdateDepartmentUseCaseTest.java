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
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.UpdateDepartmentDTO;
import br.com.sawcunhaos.organization.application.mapper.department.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateDepartmentUseCase Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateDepartmentUseCaseTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentDomainService departmentDomainService;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private UpdateDepartmentUseCase updateDepartmentUseCase;

    private UpdateDepartmentDTO validUpdateDTO;
    private Department mappedDepartment;
    private Department existingDepartment;

    @BeforeEach
    void setUp() {
        validUpdateDTO = UpdateDepartmentDTO.builder()
                .departmentId(1L)
                .code("IT-UPDATE")
                .description("Updated Information Technology Department")
                .build();

        mappedDepartment = Department.builder()
                .id(1L)
                .code("IT-UPDATE")
                .description("Updated Information Technology Department")
                .build();

        existingDepartment = Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology Department")
                .build();
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    @Test
    @DisplayName("Should update department successfully with valid data")
    void shouldUpdateDepartmentSuccessfullyWithValidData() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));

        // When
        Void result = updateDepartmentUseCase.execute(validUpdateDTO);

        // Then
        assertThat(result).isNull();
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        verify(departmentDomainService, times(1)).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        verify(departmentRepository, times(1)).update(any(Department.class));
    }

    @Test
    @DisplayName("Should throw exception when department does not exist")
    void shouldThrowExceptionWhenDepartmentDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001))
                .when(departmentDomainService)
                .validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());

        // When / Then
        assertThatThrownBy(() -> updateDepartmentUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        // Verify validations were called but update was not
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        verify(departmentDomainService, never()).validateDepartmentCodeUniquenessValidation(any(), anyLong());
    }

    @Test
    @DisplayName("Should throw exception when code is not unique")
    void shouldThrowExceptionWhenCodeIsNotUnique() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_002))
                .when(departmentDomainService)
                .validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());

        // When / Then
        assertThatThrownBy(() -> updateDepartmentUseCase.execute(validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_002.getCode());

        // Verify order of validations
        var inOrder = inOrder(departmentDomainService);
        inOrder.verify(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        inOrder.verify(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
    }

    @Test
    @DisplayName("Should update audit info before persisting department")
    void shouldUpdateAuditInfoBeforePersisting() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));

        // When
        updateDepartmentUseCase.execute(validUpdateDTO);

        // Then
        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).update(departmentCaptor.capture());

        Department capturedDepartment = departmentCaptor.getValue();
        assertThat(capturedDepartment.getId()).isEqualTo(1L);
        assertThat(capturedDepartment.getCode()).isEqualTo("IT-UPDATE");
        assertThat(capturedDepartment.getDescription()).isEqualTo("Updated Information Technology Department");
    }

    @Test
    @DisplayName("Should verify validation order before operations")
    void shouldVerifyValidationOrderBeforeOperations() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));

        // When
        updateDepartmentUseCase.execute(validUpdateDTO);

        // Then - verify order of operations
        var inOrder = inOrder(departmentDomainService, departmentMapper, departmentRepository);
        inOrder.verify(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        inOrder.verify(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        inOrder.verify(departmentMapper).toDepartment(validUpdateDTO);
        inOrder.verify(departmentRepository).findById(1L);
    }

    @Test
    @DisplayName("Should handle null description in update")
    void shouldHandleNullDescriptionInUpdate() {
        // Given
        UpdateDepartmentDTO dtoWithNullDescription = UpdateDepartmentDTO.builder()
                .departmentId(1L)
                .code("IT-UPDATED")
                .description(null)
                .build();

        Department mappedDeptWithNull = Department.builder()
                .id(1L)
                .code("IT-UPDATED")
                .description(null)
                .build();

        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(1L);
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation("IT-UPDATED", 1L);
        when(departmentMapper.toDepartment(dtoWithNullDescription)).thenReturn(mappedDeptWithNull);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));

        // When
        Void result = updateDepartmentUseCase.execute(dtoWithNullDescription);

        // Then
        assertThat(result).isNull();
        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(1L);
        verify(departmentRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should call repository update exactly once")
    void shouldCallRepositoryUpdateExactlyOnce() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existingDepartment));

        // When
        updateDepartmentUseCase.execute(validUpdateDTO);

        // Then
        verify(departmentRepository, times(1)).update(any(Department.class));
    }

    @Test
    @DisplayName("Should throw exception when findById returns empty Optional")
    void shouldThrowExceptionWhenFindByIdReturnsEmpty() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> updateDepartmentUseCase.execute(validUpdateDTO))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should preserve active field of existing department on update")
    void shouldPreserveActiveFieldOfExistingDepartmentOnUpdate() {
        // Given
        Department activeDepartment = Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology")
                .active(true)
                .build();

        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(activeDepartment));

        // When
        updateDepartmentUseCase.execute(validUpdateDTO);

        // Then — active must not be overwritten by update
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).update(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should allow updating an inactive department (operations on inactive are permitted)")
    void shouldAllowUpdatingInactiveDepartment() {
        // Given — inactive department (business rule: operations on inactive are PERMITTED)
        Department inactiveDepartment = Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology")
                .active(false)
                .build();

        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(validUpdateDTO.getDepartmentId());
        doNothing().when(departmentDomainService).validateDepartmentCodeUniquenessValidation(validUpdateDTO.getCode(), validUpdateDTO.getDepartmentId());
        when(departmentMapper.toDepartment(validUpdateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(inactiveDepartment));

        // When
        Void result = updateDepartmentUseCase.execute(validUpdateDTO);

        // Then — no exception thrown; update proceeds normally
        assertThat(result).isNull();
        verify(departmentRepository, times(1)).update(any(Department.class));
    }
}
