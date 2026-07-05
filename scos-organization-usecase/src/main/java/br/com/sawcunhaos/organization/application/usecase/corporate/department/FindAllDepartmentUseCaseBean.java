
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
import br.com.sawcunhaos.organization.api.dto.GetAllDepartmentsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class FindAllDepartmentUseCaseBean implements FindAllDepartmentUseCase {

    private final DepartmentService departmentService;

    @Override
    public GetAllDepartmentsResponse execute(@NonNull PaginationFilter paginationFilter) {
        log.info("Find All Department, Page: {}, Size: {}, Direction: {}", paginationFilter.page(), paginationFilter.sizePerPage(), paginationFilter.direction());

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<DepartmentOutput> departmentOutput = departmentService.findAll(pageable);

        return GetAllDepartmentsResponse.builder()
                .data(
                        departmentOutput.getContent().stream()
                                .map( department ->
                                        Department.builder()
                                                .id(department.id())
                                                .code(department.code())
                                                .description(department.description())
                                                .active(department.active())
                                                .build()
                                )
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(departmentOutput)
                )
                .build();
    }

}
