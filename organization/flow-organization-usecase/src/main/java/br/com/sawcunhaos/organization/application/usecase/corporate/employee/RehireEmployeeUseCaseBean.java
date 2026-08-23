
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Employee;
import br.com.sawcunhaos.organization.api.dto.RehireEmployeeRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.RehireEmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link RehireEmployeeUseCase} — delega a {@link EmployeeService#rehire} e monta a resposta aninhada. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class RehireEmployeeUseCaseBean implements RehireEmployeeUseCase {

    private final EmployeeService employeeService;
    private final CompanyService companyService;
    private final PositionService positionService;

    @Override
    public Employee execute(@NonNull RehireEmployeeRequest request) {
        log.info("Rehire employee: {}", request.taxIdentifier());

        RehireEmployeeInput input = RehireEmployeeInput.builder()
                .taxIdentifier(request.taxIdentifier())
                .companyId(request.companyId())
                .positionId(request.positionId())
                .supervisorId(request.supervisorId())
                .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.valueOf(request.contractType().name()))
                .probationEndDate(request.probationEndDate())
                .dateOfRehire(request.dateOfRehire())
                .reasonActivateId(request.reasonActivateId())
                .reasonPositionChangeId(request.reasonPositionChangeId())
                .observation(request.observation())
                .build();

        EmployeeOutput output = employeeService.rehire(input);
        CompanyOutput company = companyService.findById(output.companyId());
        PositionOutput position = positionService.findById(output.positionId());
        EmployeeOutput supervisor = output.supervisorId() != null ? employeeService.findById(output.supervisorId()) : null;

        return EmployeeApiMapper.toApiEmployee(output, company, position, supervisor);
    }
}
