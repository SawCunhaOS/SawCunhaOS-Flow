
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

import br.com.sawcunhaos.organization.api.dto.GetSystemAccessApproversResponse;
import br.com.sawcunhaos.organization.api.dto.LoginSummary;
import br.com.sawcunhaos.organization.api.dto.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.AuthorityResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista os Logins {@code ACTIVE} que hoje têm a permissão {@code APPROVE_SYSTEM_ACCESS} - via
 * {@link AuthorityResponseService}, a mesma fonte de verdade que o {@code @PreAuthorize} já usa,
 * não uma segunda forma de calcular quem tem a permissão.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindSystemAccessApproversUseCaseBean implements FindSystemAccessApproversUseCase {

    private static final String PERMISSION_APPROVE_SYSTEM_ACCESS = "APPROVE_SYSTEM_ACCESS";

    private final AuthorityResponseService authorityResponseService;

    @Override
    public GetSystemAccessApproversResponse execute() {
        log.info("Find System Access Approvers");

        return GetSystemAccessApproversResponse.builder()
                .data(authorityResponseService.findAllByPermission(PERMISSION_APPROVE_SYSTEM_ACCESS).stream()
                        .map(FindSystemAccessApproversUseCaseBean::toApiLoginSummary)
                        .toList())
                .build();
    }

    private static LoginSummary toApiLoginSummary(AuthorityResponseOutput output) {
        return LoginSummary.builder()
                .id(output.loginId())
                .login(output.login())
                .type(LoginType.valueOf(output.type()))
                .status(br.com.sawcunhaos.organization.api.dto.LoginStatus.valueOf(output.status()))
                .build();
    }
}
