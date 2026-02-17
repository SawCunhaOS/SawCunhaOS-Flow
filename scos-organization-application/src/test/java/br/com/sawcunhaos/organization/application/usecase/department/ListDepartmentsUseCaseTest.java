
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

import br.com.sawcunhaos.organization.application.dto.DepartmentDTO;
import br.com.sawcunhaos.organization.application.mapper.department.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDepartmentsUseCase Tests")
class ListDepartmentsUseCaseTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private ListDepartmentsUseCase listDepartmentsUseCase;

    private Pageable pageable;
    private List<Department> departments;
    private List<DepartmentDTO> departmentDTOs;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        departments = new ArrayList<>();
        departments.add(Department.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology")
                .build());
        departments.add(Department.builder()
                .id(2L)
                .code("HR")
                .description("Human Resources")
                .build());

        departmentDTOs = new ArrayList<>();
        departmentDTOs.add(DepartmentDTO.builder()
                .id(1L)
                .code("IT")
                .description("Information Technology")
                .build());
        departmentDTOs.add(DepartmentDTO.builder()
                .id(2L)
                .code("HR")
                .description("Human Resources")
                .build());
    }

    @Test
    @DisplayName("Should list departments with pagination successfully")
    void shouldListDepartmentsWithPaginationSuccessfully() {
        // Given
        Page<Department> departmentsPage = new PageImpl<>(departments, pageable, 2);
        when(departmentRepository.findAll(pageable)).thenReturn(departmentsPage);
        when(departmentMapper.toDepartmentDTO(departments.get(0))).thenReturn(departmentDTOs.get(0));
        when(departmentMapper.toDepartmentDTO(departments.get(1))).thenReturn(departmentDTOs.get(1));

        // When
        Page<DepartmentDTO> result = listDepartmentsUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        verify(departmentRepository, times(1)).findAll(pageable);
        verify(departmentMapper, times(2)).toDepartmentDTO(any());
    }

    @Test
    @DisplayName("Should return empty page when no departments exist")
    void shouldReturnEmptyPageWhenNoDepartmentsExist() {
        // Given
        Page<Department> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(departmentRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<DepartmentDTO> result = listDepartmentsUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(departmentRepository, times(1)).findAll(pageable);
        verify(departmentMapper, never()).toDepartmentDTO(any());
    }

    @Test
    @DisplayName("Should handle different page numbers")
    void shouldHandleDifferentPageNumbers() {
        // Given
        Pageable page2 = PageRequest.of(1, 10);
        Page<Department> departmentsPage = new PageImpl<>(departments, page2, 25);
        when(departmentRepository.findAll(page2)).thenReturn(departmentsPage);
        when(departmentMapper.toDepartmentDTO(departments.get(0))).thenReturn(departmentDTOs.get(0));
        when(departmentMapper.toDepartmentDTO(departments.get(1))).thenReturn(departmentDTOs.get(1));

        // When
        Page<DepartmentDTO> result = listDepartmentsUseCase.execute(page2);

        // Then
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(25);

        verify(departmentRepository, times(1)).findAll(page2);
    }

    @Test
    @DisplayName("Should handle different page sizes")
    void shouldHandleDifferentPageSizes() {
        // Given
        Pageable pageable20 = PageRequest.of(0, 20);
        Page<Department> departmentsPage = new PageImpl<>(departments, pageable20, 2);
        when(departmentRepository.findAll(pageable20)).thenReturn(departmentsPage);
        when(departmentMapper.toDepartmentDTO(departments.get(0))).thenReturn(departmentDTOs.get(0));
        when(departmentMapper.toDepartmentDTO(departments.get(1))).thenReturn(departmentDTOs.get(1));

        // When
        Page<DepartmentDTO> result = listDepartmentsUseCase.execute(pageable20);

        // Then
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getContent()).hasSize(2);

        verify(departmentRepository, times(1)).findAll(pageable20);
    }

    @Test
    @DisplayName("Should maintain department order in paginated result")
    void shouldMaintainDepartmentOrderInPaginatedResult() {
        // Given
        Page<Department> departmentsPage = new PageImpl<>(departments, pageable, 2);
        when(departmentRepository.findAll(pageable)).thenReturn(departmentsPage);
        when(departmentMapper.toDepartmentDTO(departments.get(0))).thenReturn(departmentDTOs.get(0));
        when(departmentMapper.toDepartmentDTO(departments.get(1))).thenReturn(departmentDTOs.get(1));

        // When
        Page<DepartmentDTO> result = listDepartmentsUseCase.execute(pageable);

        // Then
        assertThat(result.getContent())
                .extracting(DepartmentDTO::getCode)
                .containsExactly("IT", "HR");
    }

}

