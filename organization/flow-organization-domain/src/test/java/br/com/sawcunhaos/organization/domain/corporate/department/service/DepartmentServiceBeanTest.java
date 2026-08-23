
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

package br.com.sawcunhaos.organization.domain.corporate.department.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_005;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceBeanTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private EmployeeQueryRepository employeeQueryRepository;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private DepartmentServiceBean departmentServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    /**
     * {@code update(S)} exists both on {@link BaseJpaRepository} and, since Spring Data JPA 4,
     * on {@code JpaSpecificationExecutor.update(UpdateSpecification)} - javac can't disambiguate
     * the overload on a Mockito mock without narrowing the static type first.
     */
    private BaseJpaRepository<Department, Long> asBaseJpaRepository() {
        return departmentRepository;
    }

    private Department department(Long id, String code, boolean active) {
        return Department.builder().id(id).code(code).description("Engineering").active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        DepartmentInput input = DepartmentInput.builder().code("ENG").description("Engineering").build();
        Department persisted = department(1L, "ENG", true);

        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(departmentRepository.merge(any(Department.class))).thenReturn(persisted);

        DepartmentOutput result = departmentServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("ENG");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        DepartmentInput input = DepartmentInput.builder().code("ENG").description("Engineering").build();

        when(departmentRepository.existsByCode("ENG")).thenReturn(true);

        assertThatThrownBy(() -> departmentServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_002.getCode());

        verify(departmentRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenDepartmentDoesNotExist() {
        DepartmentInput input = DepartmentInput.builder().id(999L).code("ENG").description("Engineering").build();

        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherDepartment() {
        Department existing = department(1L, "ENG", true);
        DepartmentInput input = DepartmentInput.builder().id(1L).code("ENG2").description("Engineering").build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndNotId(1L, "ENG2")).thenReturn(true);

        assertThatThrownBy(() -> departmentServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        Department existing = department(1L, "ENG", true);
        DepartmentInput input = DepartmentInput.builder().id(1L).code("ENG").description("Software Engineering").build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndNotId(1L, "ENG")).thenReturn(false);

        departmentServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Software Engineering");
    }

    @Test
    void updateShouldSetManagerWhenManagerIdIsInformed() {
        Department existing = department(1L, "ENG", true);
        Employee manager = Employee.builder().id(7L).name("Jane Manager").build();
        DepartmentInput input = DepartmentInput.builder().id(1L).code("ENG").description("Engineering").managerId(7L).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndNotId(1L, "ENG")).thenReturn(false);
        when(employeeQueryRepository.findById(7L)).thenReturn(Optional.of(manager));

        departmentServiceBean.update(input);

        assertThat(existing.getManager()).isEqualTo(manager);
        verify(asBaseJpaRepository()).update(existing);
    }

    @Test
    void updateShouldRemoveManagerWhenManagerIdIsNull() {
        Department existing = department(1L, "ENG", true);
        existing.setManager(Employee.builder().id(7L).name("Jane Manager").build());
        DepartmentInput input = DepartmentInput.builder().id(1L).code("ENG").description("Engineering").managerId(null).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndNotId(1L, "ENG")).thenReturn(false);

        departmentServiceBean.update(input);

        assertThat(existing.getManager()).isNull();
        verify(employeeQueryRepository, never()).findById(any());
    }

    @Test
    void updateShouldThrowWhenManagerIdDoesNotCorrespondToAnyEmployee() {
        Department existing = department(1L, "ENG", true);
        DepartmentInput input = DepartmentInput.builder().id(1L).code("ENG").description("Engineering").managerId(999L).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCodeAndNotId(1L, "ENG")).thenReturn(false);
        when(employeeQueryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_014.getCode());

        verify(asBaseJpaRepository(), never()).update(any());
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        Department existing = department(1L, "ENG", true);
        DepartmentOutput output = DepartmentOutput.builder().id(1L).code("ENG").description("Engineering").active(true).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentMapper.toDepartmentOutput(existing)).thenReturn(output);

        DepartmentOutput result = departmentServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenActiveFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        Department existing = department(1L, "ENG", true);
        DepartmentOutput output = DepartmentOutput.builder().id(1L).code("ENG").build();

        when(departmentRepository.findAllFiltered(null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(departmentMapper.toDepartmentOutput(existing)).thenReturn(output);

        Page<DepartmentOutput> result = departmentServiceBean.findAll(null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(departmentRepository).findAllFiltered(eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithActiveFilter() {
        Pageable pageable = Pageable.unpaged();
        Department existing = department(1L, "ENG", true);
        DepartmentOutput output = DepartmentOutput.builder().id(1L).code("ENG").build();

        when(departmentRepository.findAllFiltered(true, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(departmentMapper.toDepartmentOutput(existing)).thenReturn(output);

        Page<DepartmentOutput> result = departmentServiceBean.findAll(true, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(departmentRepository).findAllFiltered(eq(true), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveDepartment() {
        Department inactive = department(1L, "ENG", false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(inactive));

        departmentServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        Department active = department(1L, "ENG", true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> departmentServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_004.getCode());
    }

    @Test
    void enableShouldThrowWhenDepartmentDoesNotExist() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateWhenNoActivePositionLinked() {
        Department active = department(1L, "ENG", true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(active));
        when(departmentRepository.existsByIdAndPositionsActive(1L)).thenReturn(false);

        departmentServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenActivePositionLinked() {
        Department active = department(1L, "ENG", true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(active));
        when(departmentRepository.existsByIdAndPositionsActive(1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_003.getCode());

        assertThat(active.isActive()).isTrue();
        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        Department inactive = department(1L, "ENG", false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(departmentRepository.existsByIdAndPositionsActive(1L)).thenReturn(false);

        assertThatThrownBy(() -> departmentServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_005.getCode());
    }

    @Test
    void disableShouldThrowWhenDepartmentDoesNotExist() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());

        verify(departmentRepository, never()).existsByIdAndPositionsActive(any());
    }

    // ---- findDepartmentById ----

    @Test
    void findDepartmentByIdShouldThrowWhenNotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentServiceBean.findDepartmentById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());
    }

    @Test
    void findDepartmentByIdShouldReturnEntityWhenFound() {
        Department existing = department(1L, "ENG", true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Department result = departmentServiceBean.findDepartmentById(1L);

        assertThat(result).isEqualTo(existing);
    }
}
