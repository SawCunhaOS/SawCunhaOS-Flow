
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate;

import br.com.sawcunhaos.organization.api.dto.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindReasonActivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindReasonActivateUseCaseBean implements FindReasonActivateUseCase {

    private final ReasonActivateService reasonActivateService;

    @Override
    public ReasonActivate execute(@NonNull Long id) {
        log.info("Find ReasonActivate : {}", id);
        ReasonActivateOutput reasonActivateOutput = reasonActivateService.findById(id);
        return ReasonActivateApiMapper.toApiReasonActivate(reasonActivateOutput);
    }
}
