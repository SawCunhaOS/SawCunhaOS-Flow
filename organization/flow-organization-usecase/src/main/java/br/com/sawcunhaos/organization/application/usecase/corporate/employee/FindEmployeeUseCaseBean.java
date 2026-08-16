
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

import br.com.sawcunhaos.organization.api.dto.Employee;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindEmployeeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindEmployeeUseCaseBean implements FindEmployeeUseCase {

    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final PositionService positionService;

    @Override
    public Employee execute(@NonNull Long id) {
        log.info("Find Employee: {}", id);
        EmployeeOutput output = employeeService.findById(id);
        CompanyOutput company = companyService.findById(output.companyId());
        PositionOutput position = positionService.findById(output.positionId());
        EmployeeOutput supervisor = output.supervisorId() != null ? employeeService.findById(output.supervisorId()) : null;
        return EmployeeApiMapper.toApiEmployee(output, company, position, supervisor);
    }
}
