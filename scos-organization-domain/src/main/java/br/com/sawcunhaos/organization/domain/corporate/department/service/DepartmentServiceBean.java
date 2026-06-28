
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

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceBean implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentOutput create(@NonNull DepartmentInput departmentInput) {
        return null;
    }

    @Override
    public void update(@NonNull DepartmentInput departmentInput) {

    }

    @Override
    public void delete(@NonNull Long departmentId) {

        Department department = findDepartmentById(departmentId);

        departmentRepository.delete(department);
    }

    @Override
    public DepartmentOutput findById(@NonNull Long departmentId) {
        Department department = findDepartmentById(departmentId);
        return departmentMapper.toDepartmentOutput(department);
    }

    @Override
    public Page<DepartmentOutput> findAll(@NonNull Pageable pageable) {
        return findAll(pageable);
    }

    private Department findDepartmentById(@NonNull Long departmentId) {
        return departmentRepository.findById(departmentId).orElseThrow(
                () -> new ScosException(SCOS_DEPARTMENT_001)
        );
    }
}
