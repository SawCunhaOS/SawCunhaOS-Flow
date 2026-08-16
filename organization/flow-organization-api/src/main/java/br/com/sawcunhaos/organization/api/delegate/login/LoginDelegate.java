
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

package br.com.sawcunhaos.organization.api.delegate.login;

import br.com.sawcunhaos.organization.api.controller.LoginApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateEmployeeLoginRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllLoginsResponse;
import br.com.sawcunhaos.organization.api.dto.GetLoginResponse;
import br.com.sawcunhaos.organization.api.dto.LoginStatus;
import br.com.sawcunhaos.organization.api.dto.LoginType;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.access.login.CreateEmployeeLoginUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindAllEmployeeLoginUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindAllLoginUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindLoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginDelegate implements LoginApiDelegate {

    private final CreateEmployeeLoginUseCase createEmployeeLoginUseCase;
    private final FindLoginUseCase findLoginUseCase;
    private final FindAllLoginUseCase findAllLoginUseCase;
    private final FindAllEmployeeLoginUseCase findAllEmployeeLoginUseCase;

    @Override
    public CreateResponse createEmployeeLogin(Long employeeId, CreateEmployeeLoginRequest createEmployeeLoginRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createEmployeeLoginUseCase.execute(employeeId, createEmployeeLoginRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public GetLoginResponse getLoginById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetLoginResponse.builder()
                .data(findLoginUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllLoginsResponse getAllLogins(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<LoginType> type, Optional<LoginStatus> status, Optional<Long> employeeId) {
        return findAllLoginUseCase.execute(paginationFilter, type.orElse(null), status.orElse(null), employeeId.orElse(null));
    }

    @Override
    public GetAllLoginsResponse getAllEmployeeLogins(PaginationFilter paginationFilter, Long employeeId, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return findAllEmployeeLoginUseCase.execute(employeeId, paginationFilter);
    }
}
