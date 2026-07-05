
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

package br.com.sawcunhaos.organization.application.usecase.corporate.department;

import br.com.sawcunhaos.organization.api.dto.CreateDepartmentRequest;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class CreateDepartmentUseCaseBean implements CreateDepartmentUseCase {

    private final DepartmentService departmentService;

    @Override
    public Department execute(@NonNull CreateDepartmentRequest createDepartmentRequest) {
        log.info("Create department: {}", createDepartmentRequest.code());

        DepartmentInput departmentInput = DepartmentInput.builder()
                .description(createDepartmentRequest.description())
                .code(createDepartmentRequest.code())
                .build();

        DepartmentOutput departmentOutput = departmentService.create(departmentInput);
        return Department.builder()
                .id(departmentOutput.id())
                .code(departmentInput.code())
                .description(departmentInput.description())
                .active(departmentOutput.active())
                .build();
    }
}
