
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
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateDepartmentRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllDepartmentsResponse;
import br.com.sawcunhaos.organization.api.dto.GetDepartmentResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateDepartmentRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.CreateDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.DisableDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.EnableDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.FindAllDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.FindDepartmentUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.department.UpdateDepartmentUseCase;
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
    private final FindAllDepartmentUseCase findAllDepartmentUseCase;
    private final DisableDepartmentUseCase disableDepartmentUseCase;
    private final EnableDepartmentUseCase enableDepartmentUseCase;
    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;

    @Override
    public GetDepartmentResponse getDepartmentById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetDepartmentResponse.builder()
                .data(findDepartmentUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllDepartmentsResponse getAllDepartments(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<Boolean> active) {
        return findAllDepartmentUseCase.execute(paginationFilter, active.orElse(null));
    }

    @Override
    public Void activateDepartment(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableDepartmentUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createDepartment(CreateDepartmentRequest createDepartmentRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createDepartmentUseCase.execute(createDepartmentRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateDepartment(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableDepartmentUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateDepartment(Long id, UpdateDepartmentRequest updateDepartmentRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateDepartmentUseCase.execute(id, updateDepartmentRequest);
        return null;
    }
}
