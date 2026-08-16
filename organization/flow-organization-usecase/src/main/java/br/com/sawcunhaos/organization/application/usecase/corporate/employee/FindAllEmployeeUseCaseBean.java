
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

package br.com.sawcunhaos.organization.application.usecase.corporate.employee;

import br.com.sawcunhaos.organization.api.dto.EmployeeStatus;
import br.com.sawcunhaos.organization.api.dto.GetAllEmployeesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindAllEmployeeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllEmployeeUseCaseBean implements FindAllEmployeeUseCase {

    private final EmployeeService employeeService;

    @Override
    public GetAllEmployeesResponse execute(@NonNull PaginationFilter paginationFilter, Long companyId, Long positionId, EmployeeStatus status) {
        log.info("Find All Employees, Page: {}, Size: {}, CompanyId: {}, PositionId: {}, Status: {}",
                paginationFilter.page(), paginationFilter.sizePerPage(), companyId, positionId, status);

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<EmployeeOutput> employeeOutput = employeeService.findAll(
                companyId, positionId, EmployeeApiMapper.toDomainStatus(status), pageable
        );

        return GetAllEmployeesResponse.builder()
                .data(employeeOutput.getContent().stream().map(EmployeeApiMapper::toApiEmployees).toList())
                .paginatedDTO(PaginatioUtils.createScosPaginated(employeeOutput))
                .build();
    }
}
