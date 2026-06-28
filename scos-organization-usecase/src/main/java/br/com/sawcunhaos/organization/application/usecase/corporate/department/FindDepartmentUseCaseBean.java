
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

import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class FindDepartmentUseCaseBean implements FindDepartmentUseCase {
    
    private final DepartmentService departmentService;

    @Override
    public Department execute(@NonNull Long id) {
        log.info("Find Department : {}", id);
        DepartmentOutput departmentOutput = departmentService.findById(id);
        return Department.builder()
                .id(departmentOutput.id())
                .code(departmentOutput.code())
                .description(departmentOutput.description())
                .active(departmentOutput.active())
                .build();
    }
}
