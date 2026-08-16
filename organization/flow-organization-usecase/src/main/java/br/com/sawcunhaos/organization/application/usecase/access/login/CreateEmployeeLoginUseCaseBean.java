
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeLoginRequest;
import br.com.sawcunhaos.organization.api.dto.Login;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginInput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_025;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateEmployeeLoginUseCaseBean implements CreateEmployeeLoginUseCase {

    private final LoginService loginService;
    private final EmployeeService employeeService;

    @Override
    public Login execute(@NonNull Long employeeId, @NonNull CreateEmployeeLoginRequest createEmployeeLoginRequest) {
        log.info("Create Employee Login: {}, EmployeeId: {}", createEmployeeLoginRequest.login(), employeeId);

        EmployeeOutput employee = employeeService.findById(employeeId);
        if (employee.status() != StatusEmployee.ACTIVE) {
            throw new ScosException(SCOS_EMPLOYEE_025);
        }

        LoginInput loginInput = LoginInput.builder()
                .login(createEmployeeLoginRequest.login())
                .profileId(createEmployeeLoginRequest.profileId())
                .type(LoginType.EMPLOYEE)
                .employeeId(employeeId)
                .build();

        return LoginApiMapper.toApiLogin(loginService.create(loginInput));
    }
}
