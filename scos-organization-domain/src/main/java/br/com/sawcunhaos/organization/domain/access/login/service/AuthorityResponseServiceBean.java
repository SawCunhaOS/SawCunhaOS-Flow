
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.VwAuthorityResponse;
import br.com.sawcunhaos.organization.domain.access.login.internal.VwAuthorityResponseRepository;
import br.com.sawcunhaos.organization.domain.access.login.specification.AuthorityResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_AUTHORITY_001;

@Service
@RequiredArgsConstructor
@Slf4j
class AuthorityResponseServiceBean implements AuthorityResponseService {

    private final VwAuthorityResponseRepository vwAuthorityResponseRepository;
    private final AuthorityResponseMapper authorityResponseMapper;

    @Override
    public AuthorityResponseOutput validate(@NonNull String login) {
        log.info("Validating Authority: {}", login);

        VwAuthorityResponse authorityResponse = vwAuthorityResponseRepository.findByLogin(login).orElseThrow(
                () -> new ScosException(SCOS_AUTHORITY_001)
        );


        return authorityResponseMapper.toOutput(authorityResponse);
    }
}
