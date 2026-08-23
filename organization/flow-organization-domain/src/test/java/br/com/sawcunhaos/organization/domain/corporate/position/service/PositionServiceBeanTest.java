
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

package br.com.sawcunhaos.organization.domain.corporate.position.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.service.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeePositionQueryService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionRepository;
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

import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_006;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_005;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceBeanTest {

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private PositionMapper positionMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private EmployeePositionQueryService employeePositionQueryService;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private PositionServiceBean positionServiceBean;

    private Department activeDepartment;
    private DepartmentOutput activeDepartmentOutput;

    @BeforeEach
    void setUp() {
        activeDepartment = Department.builder().id(1L).code("ENG").description("Engineering").active(true).build();
        activeDepartmentOutput = DepartmentOutput.builder().id(1L).code("ENG").description("Engineering").active(true).build();
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
        lenient().when(departmentMapper.toDepartmentOutput(activeDepartment)).thenReturn(activeDepartmentOutput);
    }

    /**
     * {@code update(S)} exists both on {@link BaseJpaRepository} and, since Spring Data JPA 4,
     * on {@code JpaSpecificationExecutor.update(UpdateSpecification)} - javac can't disambiguate
     * the overload on a Mockito mock without narrowing the static type first.
     */
    private BaseJpaRepository<Position, Long> asBaseJpaRepository() {
        return positionRepository;
    }

    private Position position(Long id, String code, boolean active, Department department) {
        return Position.builder()
                .id(id)
                .code(code)
                .description("Developer")
                .active(active)
                .isTrustPosition(false)
                .department(department)
                .build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUniqueAndDepartmentIsActive() {
        PositionInput input = PositionInput.builder().code("DEV").description("Developer").departmentId(1L).isTrustPosition(false).build();
        Position persisted = position(10L, "DEV", true, activeDepartment);
        PositionOutput output = PositionOutput.builder().id(10L).code("DEV").description("Developer").active(true).isTrustPosition(false).build();

        when(positionRepository.existsByCode("DEV")).thenReturn(false);
        when(departmentService.findDepartmentById(1L)).thenReturn(activeDepartment);
        when(positionRepository.merge(any(Position.class))).thenReturn(persisted);
        when(positionMapper.toPositionOutput(persisted)).thenReturn(output);

        PositionOutput result = positionServiceBean.create(input);

        assertThat(result.id()).isEqualTo(output.id());
        assertThat(result.code()).isEqualTo(output.code());
        assertThat(result.department()).isEqualTo(activeDepartmentOutput);
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        PositionInput input = PositionInput.builder().code("DEV").description("Developer").departmentId(1L).isTrustPosition(false).build();

        when(positionRepository.existsByCode("DEV")).thenReturn(true);

        assertThatThrownBy(() -> positionServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_002.getCode());

        verify(departmentService, never()).findDepartmentById(any());
    }

    @Test
    void createShouldThrowWhenDepartmentDoesNotExist() {
        PositionInput input = PositionInput.builder().code("DEV").description("Developer").departmentId(99L).isTrustPosition(false).build();

        when(positionRepository.existsByCode("DEV")).thenReturn(false);
        when(departmentService.findDepartmentById(99L)).thenThrow(new ScosException(SCOS_DEPARTMENT_001));

        assertThatThrownBy(() -> positionServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_001.getCode());
    }


    @Test
    void createShouldThrowWhenDepartmentIsInactive() {
        Department inactiveDepartment = Department.builder().id(2L).code("HR").description("Human Resources").active(false).build();
        PositionInput input = PositionInput.builder().code("DEV").description("Developer").departmentId(2L).isTrustPosition(false).build();

        when(positionRepository.existsByCode("DEV")).thenReturn(false);
        when(departmentService.findDepartmentById(2L)).thenReturn(inactiveDepartment);

        assertThatThrownBy(() -> positionServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_006.getCode());

        verify(positionRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenPositionDoesNotExist() {
        PositionInput input = PositionInput.builder().id(999L).code("DEV").description("Developer").departmentId(1L).isTrustPosition(false).build();

        when(positionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherPosition() {
        Position existing = position(10L, "DEV", true, activeDepartment);
        PositionInput input = PositionInput.builder().id(10L).code("DEV2").description("Developer").departmentId(1L).isTrustPosition(false).build();

        when(positionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(positionRepository.existsByCodeAndNotId(10L, "DEV2")).thenReturn(true);

        assertThatThrownBy(() -> positionServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_002.getCode());
    }

    @Test
    void updateShouldRevalidateDepartmentEvenWhenUnchanged() {
        Position existing = position(10L, "DEV", true, activeDepartment);
        Department departmentBecameInactive = Department.builder().id(1L).code("ENG").description("Engineering").active(false).build();
        PositionInput input = PositionInput.builder().id(10L).code("DEV").description("Developer").departmentId(1L).isTrustPosition(false).build();

        when(positionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(positionRepository.existsByCodeAndNotId(10L, "DEV")).thenReturn(false);
        when(departmentService.findDepartmentById(1L)).thenReturn(departmentBecameInactive);

        assertThatThrownBy(() -> positionServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_DEPARTMENT_006.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        Position existing = position(10L, "DEV", true, activeDepartment);
        PositionInput input = PositionInput.builder().id(10L).code("DEV").description("Senior Developer").departmentId(1L).isTrustPosition(true).build();

        when(positionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(positionRepository.existsByCodeAndNotId(10L, "DEV")).thenReturn(false);
        when(departmentService.findDepartmentById(1L)).thenReturn(activeDepartment);

        positionServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Senior Developer");
        assertThat(existing.isTrustPosition()).isTrue();
    }

    // ---- findAll ----

    @Test
    void findAllShouldDelegateToRepositoryWithDepartmentFilter() {
        Pageable pageable = Pageable.unpaged();
        Position existing = position(10L, "DEV", true, activeDepartment);
        PositionOutput output = PositionOutput.builder().id(10L).code("DEV").build();

        when(positionRepository.findAllFiltered(5L, null, pageable)).thenReturn(new PageImpl<>(java.util.List.of(existing)));
        when(positionMapper.toPositionOutput(existing)).thenReturn(output);

        Page<PositionOutput> result = positionServiceBean.findAll(5L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(output.id());
        verify(positionRepository).findAllFiltered(eq(5L), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithActiveFilter() {
        Pageable pageable = Pageable.unpaged();
        Position existing = position(10L, "DEV", true, activeDepartment);
        PositionOutput output = PositionOutput.builder().id(10L).code("DEV").build();

        when(positionRepository.findAllFiltered(null, true, pageable)).thenReturn(new PageImpl<>(java.util.List.of(existing)));
        when(positionMapper.toPositionOutput(existing)).thenReturn(output);

        Page<PositionOutput> result = positionServiceBean.findAll(null, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(positionRepository).findAllFiltered(eq(null), eq(true), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactivePosition() {
        Position inactive = position(10L, "DEV", false, activeDepartment);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(inactive));

        positionServiceBean.enable(10L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        Position active = position(10L, "DEV", true, activeDepartment);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> positionServiceBean.enable(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_004.getCode());
    }

    @Test
    void enableShouldThrowWhenPositionDoesNotExist() {
        when(positionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateWhenNoActiveEmployeeLinked() {
        Position active = position(10L, "DEV", true, activeDepartment);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(active));
        when(employeePositionQueryService.existsActiveEmployeeInPosition(10L)).thenReturn(false);

        positionServiceBean.disable(10L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenActiveEmployeeLinked() {
        Position active = position(10L, "DEV", true, activeDepartment);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(active));
        when(employeePositionQueryService.existsActiveEmployeeInPosition(10L)).thenReturn(true);

        assertThatThrownBy(() -> positionServiceBean.disable(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_003.getCode());

        assertThat(active.isActive()).isTrue();
        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        Position inactive = position(10L, "DEV", false, activeDepartment);
        when(positionRepository.findById(10L)).thenReturn(Optional.of(inactive));
        when(employeePositionQueryService.existsActiveEmployeeInPosition(10L)).thenReturn(false);

        assertThatThrownBy(() -> positionServiceBean.disable(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_005.getCode());
    }

    @Test
    void disableShouldThrowWhenPositionDoesNotExist() {
        when(positionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());

        verify(employeePositionQueryService, never()).existsActiveEmployeeInPosition(anyLong());
    }

    // ---- findPositionById ----

    @Test
    void findPositionByIdShouldThrowWhenNotFound() {
        when(positionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionServiceBean.findPositionById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());
    }
}
