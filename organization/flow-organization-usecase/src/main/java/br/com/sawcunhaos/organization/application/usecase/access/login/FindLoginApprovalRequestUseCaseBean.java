
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

import br.com.sawcunhaos.organization.api.dto.GetLoginApprovalRequestResponse;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindLoginApprovalRequestUseCaseBean implements FindLoginApprovalRequestUseCase {

    private final LoginApprovalRequestService loginApprovalRequestService;

    @Override
    public GetLoginApprovalRequestResponse execute(@NonNull Long id) {
        log.info("Find Login Approval Request by Id: {}", id);

        return GetLoginApprovalRequestResponse.builder()
                .data(LoginApprovalRequestApiMapper.toApiLoginApprovalRequest(loginApprovalRequestService.findById(id)))
                .build();
    }
}
