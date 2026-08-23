
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

package br.com.sawcunhaos.organization.domain.access.login.internal.rules;

import br.com.sawcunhaos.foundation.spring.annotation.rules.ScosRule;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.shared.validation.BusinessRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_014;

@ScosRule(3)
@RequiredArgsConstructor
@Slf4j
class LoginPendingApprovalRule implements BusinessRule<LoginStatus> {

    @Override
    public Optional<String> validate(LoginStatus context) {
        if (context.equals(LoginStatus.PENDING_APPROVAL))
            return Optional.of(SCOS_LOGIN_014.getCode());
        return Optional.empty();
    }
}
