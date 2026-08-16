
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

import br.com.sawcunhaos.organization.api.dto.Login;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindLoginUseCaseBean implements FindLoginUseCase {

    private final LoginService loginService;

    @Override
    public Login execute(@NonNull Long id) {
        log.info("Find Login: {}", id);
        return LoginApiMapper.toApiLogin(loginService.findById(id));
    }
}
