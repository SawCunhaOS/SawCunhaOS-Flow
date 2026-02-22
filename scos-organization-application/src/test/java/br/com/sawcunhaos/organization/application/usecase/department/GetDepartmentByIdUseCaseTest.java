
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
import br.com.sawcunhaos.organization.application.dto.DepartmentDTO;
import br.com.sawcunhaos.organization.application.mapper.department.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetDepartmentByIdUseCase Tests")
class GetDepartmentByIdUseCaseTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentDomainService departmentDomainService;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private GetDepartmentByIdUseCase getDepartmentByIdUseCase;

    private Department department;
    private DepartmentDTO departmentDTO;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology Department")
                .build();

        departmentDTO = DepartmentDTO.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology Department")
                .build();
    }

    @Test
    @DisplayName("Should get department by id successfully")
    void shouldGetDepartmentByIdSuccessfully() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDepartmentDTO(department)).thenReturn(departmentDTO);

        // When
        DepartmentDTO result = getDepartmentByIdUseCase.execute(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("IT");
        assertThat(result.getDescription()).isEqualTo("Information Technology Department");

        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(1L);
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentMapper, times(1)).toDepartmentDTO(department);
    }

    @Test
    @DisplayName("Should throw exception when department does not exist")
    void shouldThrowExceptionWhenDepartmentDoesNotExist() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001))
                .when(departmentDomainService)
                .validateDepartmentExistsValidation(99L);

        // When / Then
        assertThatThrownBy(() -> getDepartmentByIdUseCase.execute(99L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(departmentDomainService, times(1)).validateDepartmentExistsValidation(99L);
        verify(departmentRepository, never()).findById(99L);
    }

    @Test
    @DisplayName("Should handle department not found on repository call")
    void shouldHandleDepartmentNotFoundOnRepositoryCall() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getDepartmentByIdUseCase.execute(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_DEPARTMENT_001.getCode());

        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentMapper, never()).toDepartmentDTO(any());
    }

    @Test
    @DisplayName("Should return department DTO with all fields")
    void shouldReturnDepartmentDTOWithAllFields() {
        // Given
        doNothing().when(departmentDomainService).validateDepartmentExistsValidation(1L);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDepartmentDTO(department)).thenReturn(departmentDTO);

        // When
        DepartmentDTO result = getDepartmentByIdUseCase.execute(1L);

        // Then
        assertThat(result.getId()).isEqualTo(departmentDTO.getId());
        assertThat(result.getCode()).isEqualTo(departmentDTO.getCode());
        assertThat(result.getDescription()).isEqualTo(departmentDTO.getDescription());
    }

}

