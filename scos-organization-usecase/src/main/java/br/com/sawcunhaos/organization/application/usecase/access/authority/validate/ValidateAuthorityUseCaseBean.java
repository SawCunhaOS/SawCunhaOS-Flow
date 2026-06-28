
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

package br.com.sawcunhaos.organization.application.usecase.access.authority.validate;

import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.AuthorityResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class ValidateAuthorityUseCaseBean implements ValidateAuthorityUseCase {

    private final AuthorityResponseService authorityResponseService;

    @Override
    public ValidateAuthorityOutput execute(@NonNull String login) {
        log.info("Validating Authority: {}", login);

        AuthorityResponseOutput authorityResponseOutput = authorityResponseService.validate(login);

        return ValidateAuthorityOutput.builder()
                .login(authorityResponseOutput.login())
                .name(authorityResponseOutput.name())
                .email(authorityResponseOutput.email())
                .companyId(authorityResponseOutput.companyId())
                .companyName(authorityResponseOutput.companyName())
                .branchId(authorityResponseOutput.branchId())
                .branchName(authorityResponseOutput.branchName())
                .employeeId(authorityResponseOutput.employeeId())
                .permissions(authorityResponseOutput.permissions())
                .build();
    }
}
