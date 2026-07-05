
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.service.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeePositionQueryService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionRepository;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_006;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_003;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionServiceBean implements PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final DepartmentMapper departmentMapper;
    private final DepartmentService departmentService;
    private final EmployeePositionQueryService employeePositionQueryService;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public PositionOutput create(@NonNull PositionInput positionInput) {
        log.info("Create Position: {}", positionInput.code());

        if (positionRepository.existsByCode(positionInput.code())) {
            throw new ScosException(SCOS_POSITION_002);
        }

        Department department = findActiveDepartmentOrThrow(positionInput.departmentId());

        Position position = Position.builder()
                .code(positionInput.code())
                .description(positionInput.description())
                .active(true)
                .isTrustPosition(positionInput.isTrustPosition())
                .department(department)
                .build();
        position.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        position = positionRepository.merge(position);

        return toPositionOutput(position);
    }

    @Override
    public void update(@NonNull PositionInput positionInput) {
        log.info("Update Position: {}", positionInput.id());
        Position position = findPositionById(positionInput.id());

        if (positionRepository.existsByCodeAndNotId(positionInput.id(), positionInput.code())) {
            throw new ScosException(SCOS_POSITION_002);
        }

        Department department = findActiveDepartmentOrThrow(positionInput.departmentId());

        position.setCode(positionInput.code());
        position.setDescription(positionInput.description());
        position.setTrustPosition(positionInput.isTrustPosition());
        position.setDepartment(department);
        position.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        positionRepository.update(position);
    }

    @Override
    public PositionOutput findById(@NonNull Long positionId) {
        log.info("Find Position by Id: {}", positionId);
        Position position = findPositionById(positionId);
        return toPositionOutput(position);
    }

    @Override
    public Page<PositionOutput> findAll(@NonNull Long departmentId, @NonNull Boolean active, @NonNull Pageable pageable) {
        log.info("Find All Positions, DepartmentId: {}, Active: {}", departmentId, active);
        return positionRepository.findAllFiltered(departmentId, active, pageable)
                .map(positionMapper::toPositionOutput);
    }

    @Override
    public void enable(@NonNull Long positionId) {
        log.info("Enable Position: {}", positionId);
        Position position = findPositionById(positionId);
        position.activate();

        positionRepository.update(position);
        log.info("Position enabled");
    }

    @Override
    public void disable(@NonNull Long positionId) {
        log.info("Disable Position: {}", positionId);

        Position position = findPositionById(positionId);

        if (employeePositionQueryService.existsActiveEmployeeInPosition(positionId)) {
            throw new ScosException(SCOS_POSITION_003);
        }

        position.deactivate();

        positionRepository.update(position);
        log.info("Position disabled");
    }

    @Override
    public Position findPositionById(@NonNull Long positionId) {
        log.info("Find Position by Id: {}", positionId);
        return positionRepository.findById(positionId).orElseThrow(
                () -> new ScosException(SCOS_POSITION_001)
        );
    }

    private Department findActiveDepartmentOrThrow(Long departmentId) {
        Department department = departmentService.findDepartmentById(departmentId);
        if (!department.isActive()) {
            throw new ScosException(SCOS_DEPARTMENT_006);
        }
        return department;
    }

    private PositionOutput toPositionOutput(Position position) {
        PositionOutput output = positionMapper.toPositionOutput(position);
        return PositionOutput.builder()
                .id(output.id())
                .code(output.code())
                .description(output.description())
                .active(output.active())
                .isTrustPosition(output.isTrustPosition())
                .department(departmentMapper.toDepartmentOutput(position.getDepartment()))
                .build();
    }
}
