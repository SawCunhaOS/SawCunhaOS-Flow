
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

package br.com.sawcunhaos.organization.api.delegate.department;

import br.com.sawcunhaos.organization.api.controller.DepartmentApiDelegate;
import br.com.sawcunhaos.organization.api.dto.GetAllDepartmentsResponse;
import br.com.sawcunhaos.organization.api.dto.GetDepartmentResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.FindDepartmentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepartmentDelegate implements DepartmentApiDelegate {

    private final FindDepartmentUseCase findDepartmentUseCase;

    @Override
    public GetDepartmentResponse getDepartmentById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetDepartmentResponse.builder()
                .data(findDepartmentUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllDepartmentsResponse getAllDepartments(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<Boolean> active) {
        return DepartmentApiDelegate.super.getAllDepartments(paginationFilter, xRequestID, acceptLanguage, active);
    }
}
