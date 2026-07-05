
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.internal.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_003;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceBean implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public DepartmentOutput create(@NonNull DepartmentInput departmentInput) {
        log.info("Create Department: {}", departmentInput.code());

        if (departmentRepository.existsByCode(departmentInput.code())) {
            throw new ScosException(SCOS_DEPARTMENT_002);
        }

        Department department = Department.builder()
                .code(departmentInput.code())
                .description(departmentInput.description())
                .active(true)
                .build();
        department.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        department = departmentRepository.merge(department);

        return DepartmentOutput.builder()
                .id(department.getId())
                .code(department.getCode())
                .description(department.getDescription())
                .active(department.isActive())
                .build();
    }

    @Override
    public void update(@NonNull DepartmentInput departmentInput) {
        log.info("Update Department: {}", departmentInput.id());
        Department department = findDepartmentById(departmentInput.id());

        if (departmentRepository.existsByCodeAndNotId(departmentInput.id(), departmentInput.code())) {
            throw new ScosException(SCOS_DEPARTMENT_002);
        }

        department.setCode(departmentInput.code());
        department.setDescription(departmentInput.description());
        department.updateAuditInfo(scosUserAuthentication.findUserAuthentication());
        departmentRepository.update(department);
    }

    @Override
    public DepartmentOutput findById(@NonNull Long departmentId) {
        log.info("Find Department by Id: {}", departmentId);
        Department department = findDepartmentById(departmentId);
        return departmentMapper.toDepartmentOutput(department);
    }

    @Override
    public Page<DepartmentOutput> findAll(@NonNull Boolean active, @NonNull Pageable pageable) {
        log.info("Find All Departments, Active: {}", active);
        return departmentRepository.findAllFiltered(active, pageable)
                .map(departmentMapper::toDepartmentOutput);
    }

    @Override
    public void enable(@NonNull Long departmentId) {
        log.info("Enable Department: {}", departmentId);
        Department department = findDepartmentById(departmentId);
        department.activate();

        departmentRepository.update(department);
        log.info("Department enabled");
    }

    @Override
    public void disable(@NonNull Long departmentId) {
        log.info("Disable Department: {}", departmentId);

        Department department = findDepartmentById(departmentId);

        if(departmentRepository.existsByIdAndPositionsActive(departmentId)){
            throw new ScosException(SCOS_DEPARTMENT_003);
        }

        department.deactivate();

        departmentRepository.update(department);
        log.info("Department disabled");
    }

    @Override
    public Department findDepartmentById(@NonNull Long departmentId) {
        log.info("Find Department by Id: {}", departmentId);
        return departmentRepository.findById(departmentId).orElseThrow(
                () -> new ScosException(SCOS_DEPARTMENT_001)
        );
    }
}
