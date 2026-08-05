
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

package br.com.sawcunhaos.organization.api.delegate.employee;

import br.com.sawcunhaos.organization.api.controller.EmployeeApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.EmployeeStatusTransitionRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.employee.ActivateEmployeeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.employee.BlockEmployeeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.employee.CreateEmployeeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.employee.InactivateEmployeeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.employee.UnblockEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeDelegate implements EmployeeApiDelegate {

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final ActivateEmployeeUseCase activateEmployeeUseCase;
    private final InactivateEmployeeUseCase inactivateEmployeeUseCase;
    private final BlockEmployeeUseCase blockEmployeeUseCase;
    private final UnblockEmployeeUseCase unblockEmployeeUseCase;

    @Override
    public CreateResponse createEmployee(CreateEmployeeRequest createEmployeeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder().id(createEmployeeUseCase.execute(createEmployeeRequest)).build())
                .build();
    }

    @Override
    public Void activateEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        activateEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void inactivateEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        inactivateEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void blockEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        blockEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }

    @Override
    public Void unblockEmployee(Long id, EmployeeStatusTransitionRequest employeeStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        unblockEmployeeUseCase.execute(id, employeeStatusTransitionRequest);
        return null;
    }
}
