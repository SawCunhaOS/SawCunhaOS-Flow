
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestProfileChangeKind;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class RequestLoginProfileChangeUseCaseBean implements RequestLoginProfileChangeUseCase {

    private final LoginService loginService;

    @Override
    public Long execute(@NonNull Long loginId, @NonNull Long profileId, @NonNull ProfileChangeKind kind) {
        log.info("Request Login Profile Change: {}, ProfileId: {}, Kind: {}", loginId, profileId, kind);

        LoginApprovalRequestProfileChangeKind domainKind = kind == ProfileChangeKind.SET_PRIMARY
                ? LoginApprovalRequestProfileChangeKind.SET_PRIMARY
                : LoginApprovalRequestProfileChangeKind.ADD_ADDITIONAL;

        return loginService.requestProfileChange(loginId, profileId, domainKind);
    }
}
