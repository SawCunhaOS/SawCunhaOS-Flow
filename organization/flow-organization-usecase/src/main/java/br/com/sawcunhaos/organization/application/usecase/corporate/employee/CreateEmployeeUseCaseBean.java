
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeRequest;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeInput;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateEmployeeUseCaseBean implements CreateEmployeeUseCase {

    private final EmployeeService employeeService;

    @Override
    public Long execute(@NonNull CreateEmployeeRequest request) {
        log.info("Create employee: {}", request.taxIdentifier());

        EmployeeInput input = EmployeeInput.builder()
                .name(request.name())
                .nameTreatment(request.nameTreatment())
                .taxIdentifier(request.taxIdentifier())
                .email(request.email())
                .birthDate(request.birthDate())
                .observation(request.observation())
                .dateOfHiring(request.dateOfHiring())
                .contractType(br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType.valueOf(request.contractType().name()))
                .probationEndDate(request.probationEndDate())
                .supervisorId(request.supervisorId())
                .companyId(request.companyId())
                .positionId(request.positionId())
                .reasonActivateId(request.reasonActivateId())
                .build();

        return employeeService.create(input).id();
    }
}
