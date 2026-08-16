
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

import br.com.sawcunhaos.organization.api.dto.GetAllLoginsResponse;
import br.com.sawcunhaos.organization.api.dto.LoginStatus;
import br.com.sawcunhaos.organization.api.dto.LoginType;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllLoginUseCaseBean implements FindAllLoginUseCase {

    private final LoginService loginService;

    @Override
    public GetAllLoginsResponse execute(@NonNull PaginationFilter paginationFilter, LoginType type, LoginStatus status, Long employeeId) {
        log.info("Find All Logins, Type: {}, Status: {}, EmployeeId: {}", type, status, employeeId);

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<LoginOutput> loginOutputPage = loginService.findAll(
                LoginApiMapper.toDomainType(type), LoginApiMapper.toDomainStatus(status), employeeId, pageable
        );

        return GetAllLoginsResponse.builder()
                .data(loginOutputPage.getContent().stream().map(LoginApiMapper::toApiLoginSummary).toList())
                .paginatedDTO(PaginatioUtils.createScosPaginated(loginOutputPage))
                .build();
    }
}
