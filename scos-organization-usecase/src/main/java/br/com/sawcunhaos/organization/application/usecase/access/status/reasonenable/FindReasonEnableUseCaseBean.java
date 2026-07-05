
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable;

import br.com.sawcunhaos.organization.api.dto.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindReasonEnableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindReasonEnableUseCaseBean implements FindReasonEnableUseCase {

    private final ReasonEnableService reasonEnableService;

    @Override
    public ReasonEnable execute(@NonNull Long id) {
        log.info("Find ReasonEnable : {}", id);
        ReasonEnableOutput reasonEnableOutput = reasonEnableService.findById(id);
        return ReasonEnableApiMapper.toApiReasonEnable(reasonEnableOutput);
    }
}
