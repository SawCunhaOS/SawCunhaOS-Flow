
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


import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateDepartmentRequest;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentInput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateDepartmentUseCaseBean implements UpdateDepartmentUseCase {
    private final DepartmentService departmentService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateDepartmentRequest updateDepartmentRequest) {
        departmentService.update(
                DepartmentInput.builder()
                        .id(id)
                        .code(updateDepartmentRequest.code())
                        .description(updateDepartmentRequest.description())
                        .managerId(updateDepartmentRequest.managerId())
                        .build()
        );
    }
}
