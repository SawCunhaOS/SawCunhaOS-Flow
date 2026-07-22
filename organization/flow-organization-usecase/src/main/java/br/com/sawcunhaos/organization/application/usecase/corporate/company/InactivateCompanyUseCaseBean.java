
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CompanyStatusTransitionRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link InactivateCompanyUseCase} — delega a {@link CompanyService#inactivate}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class InactivateCompanyUseCaseBean implements InactivateCompanyUseCase {

    private final CompanyService companyService;

    @Override
    public void execute(@NonNull Long id, @NonNull CompanyStatusTransitionRequest request) {
        log.info("Inactivate company: {}", id);
        companyService.inactivate(id, request.reasonId(), request.observation());
    }
}
