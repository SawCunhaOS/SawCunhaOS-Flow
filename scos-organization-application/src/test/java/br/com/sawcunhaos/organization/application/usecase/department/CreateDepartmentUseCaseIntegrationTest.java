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
import br.com.sawcunhaos.organization.application.dto.CreateDepartmentDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateDepartmentUseCase Integration Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateDepartmentUseCaseIntegrationTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentDomainService departmentDomainService;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private CreateDepartmentUseCase createDepartmentUseCase;

    private CreateDepartmentDTO validCreateDTO;
    private Department mappedDepartment;
    private Department savedDepartment;

    @BeforeEach
    void setUp() {
        validCreateDTO = CreateDepartmentDTO.builder()
                .code("IT")
                .description("Information Technology Department")
                .build();

        mappedDepartment = Department.builder()
                .code("IT")
                .description("Information Technology Department")
                .build();

        savedDepartment = Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology Department")
                .build();

        when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    @Test
    @DisplayName("Should create department successfully with valid data")
    void shouldCreateDepartmentSuccessfullyWithValidData() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDepartment);

        // When
        Long departmentId = createDepartmentUseCase.execute(validCreateDTO);

        // Then
        assertThat(departmentId).isNotNull();
        assertThat(departmentId).isEqualTo(1L);

        // Verify interactions
        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        verify(departmentMapper, times(1)).toDepartment(validCreateDTO);
        verify(departmentRepository, times(1)).persist(any(Department.class));
    }

    @Test
    @DisplayName("Should call updateAuditInfo before persisting department")
    void shouldCallUpdateAuditInfoBeforePersisting() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDepartment);

        // When
        createDepartmentUseCase.execute(validCreateDTO);

        // Then
        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).persist(departmentCaptor.capture());

        Department capturedDepartment = departmentCaptor.getValue();
        assertThat(capturedDepartment.getCode()).isEqualTo("IT");
        assertThat(capturedDepartment.getDescription()).isEqualTo("Information Technology Department");
    }

    @Test
    @DisplayName("Should throw ScosException when department code already exists")
    void shouldThrowScosExceptionWhenDepartmentCodeAlreadyExists() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_002))
                .when(departmentDomainService)
                .validateDepartmentCodeExistsValidation(validCreateDTO.getCode());

        // When / Then
        assertThatThrownBy(() -> createDepartmentUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_002.getCode());

        // Verify that mapper and repository were never called
        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        verify(departmentMapper, never()).toDepartment(any(CreateDepartmentDTO.class));
        verify(departmentRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should validate department code before mapping")
    void shouldValidateDepartmentCodeBeforeMapping() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDepartment);

        // When
        createDepartmentUseCase.execute(validCreateDTO);

        // Then - verify order of operations
        var inOrder = inOrder(departmentDomainService, departmentMapper, departmentRepository);
        inOrder.verify(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        inOrder.verify(departmentMapper).toDepartment(validCreateDTO);
        inOrder.verify(departmentRepository).persist(any(Department.class));
    }

    @Test
    @DisplayName("Should handle department with special characters in code")
    void shouldHandleDepartmentWithSpecialCharactersInCode() {
        // Given
        CreateDepartmentDTO dtoWithSpecialChars = CreateDepartmentDTO.builder()
                .code("IT-001")
                .description("IT Department - Level 1")
                .build();

        Department mappedDept = Department.builder()
                .code("IT-001")
                .description("IT Department - Level 1")
                .build();

        Department savedDept = Department.builder()
                .id(2L)
                .code("IT-001")
                .description("IT Department - Level 1")
                .build();

        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(dtoWithSpecialChars.getCode());
        when(departmentMapper.toDepartment(dtoWithSpecialChars)).thenReturn(mappedDept);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDept);

        // When
        Long departmentId = createDepartmentUseCase.execute(dtoWithSpecialChars);

        // Then
        assertThat(departmentId).isNotNull();
        assertThat(departmentId).isEqualTo(2L);

        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation("IT-001");
    }

    @Test
    @DisplayName("Should handle department with long description")
    void shouldHandleDepartmentWithLongDescription() {
        // Given
        String longDescription = "This is a very long description for the department that contains " +
                "detailed information about its responsibilities, scope, and organizational structure";

        CreateDepartmentDTO dtoWithLongDesc = CreateDepartmentDTO.builder()
                .code("HR")
                .description(longDescription)
                .build();

        Department mappedDept = Department.builder()
                .code("HR")
                .description(longDescription)
                .build();

        Department savedDept = Department.builder()
                .id(3L)
                .code("HR")
                .description(longDescription)
                .build();

        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(dtoWithLongDesc.getCode());
        when(departmentMapper.toDepartment(dtoWithLongDesc)).thenReturn(mappedDept);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDept);

        // When
        Long departmentId = createDepartmentUseCase.execute(dtoWithLongDesc);

        // Then
        assertThat(departmentId).isNotNull();
        assertThat(departmentId).isEqualTo(3L);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).persist(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("Should throw exception when mapper returns null")
    void shouldThrowExceptionWhenMapperReturnsNull() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> createDepartmentUseCase.execute(validCreateDTO))
                .isInstanceOf(NullPointerException.class);

        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        verify(departmentMapper, times(1)).toDepartment(validCreateDTO);
        verify(departmentRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should handle repository persistence failure")
    void shouldHandleRepositoryPersistenceFailure() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.persist(any(Department.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // When / Then
        assertThatThrownBy(() -> createDepartmentUseCase.execute(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");

        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        verify(departmentMapper, times(1)).toDepartment(validCreateDTO);
        verify(departmentRepository, times(1)).persist(any(Department.class));
    }

    @Test
    @DisplayName("Should create multiple departments with different codes")
    void shouldCreateMultipleDepartmentsWithDifferentCodes() {
        // Given - First department
        CreateDepartmentDTO firstDTO = CreateDepartmentDTO.builder()
                .code("IT")
                .description("IT Department")
                .build();

        Department firstMapped = Department.builder()
                .code("IT")
                .description("IT Department")
                .build();

        Department firstSaved = Department.builder()
                .id(1L)
                .code("IT")
                .description("IT Department")
                .build();

        // Given - Second department
        CreateDepartmentDTO secondDTO = CreateDepartmentDTO.builder()
                .code("HR")
                .description("HR Department")
                .build();

        Department secondMapped = Department.builder()
                .code("HR")
                .description("HR Department")
                .build();

        Department secondSaved = Department.builder()
                .id(2L)
                .code("HR")
                .description("HR Department")
                .build();

        // Setup mocks for first department
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation("IT");
        when(departmentMapper.toDepartment(firstDTO)).thenReturn(firstMapped);
        when(departmentRepository.persist(firstMapped)).thenReturn(firstSaved);

        // When - Create first department
        Long firstId = createDepartmentUseCase.execute(firstDTO);

        // Then - Verify first department
        assertThat(firstId).isEqualTo(1L);

        // Setup mocks for second department
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation("HR");
        when(departmentMapper.toDepartment(secondDTO)).thenReturn(secondMapped);
        when(departmentRepository.persist(secondMapped)).thenReturn(secondSaved);

        // When - Create second department
        Long secondId = createDepartmentUseCase.execute(secondDTO);

        // Then - Verify second department
        assertThat(secondId).isEqualTo(2L);

        // Verify total interactions
        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation("IT");
        verify(departmentDomainService, times(1)).validateDepartmentCodeExistsValidation("HR");
        verify(departmentRepository, times(2)).persist(any(Department.class));
    }

    @Test
    @DisplayName("Should set active=true by default when creating a new department")
    void shouldSetActiveTrueByDefaultOnNewDepartmentCreation() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentCodeExistsValidation(validCreateDTO.getCode());
        when(departmentMapper.toDepartment(validCreateDTO)).thenReturn(mappedDepartment);
        when(departmentRepository.persist(any(Department.class))).thenReturn(savedDepartment);

        // When
        createDepartmentUseCase.execute(validCreateDTO);

        // Then
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).persist(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }
}

