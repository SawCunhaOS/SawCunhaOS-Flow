
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable;

import br.com.sawcunhaos.organization.api.dto.ReasonDisable;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindReasonDisableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindReasonDisableUseCaseBean implements FindReasonDisableUseCase {

    private final ReasonDisableService reasonDisableService;

    @Override
    public ReasonDisable execute(@NonNull Long id) {
        log.info("Find ReasonDisable : {}", id);
        ReasonDisableOutput reasonDisableOutput = reasonDisableService.findById(id);
        return ReasonDisableApiMapper.toApiReasonDisable(reasonDisableOutput);
    }
}
