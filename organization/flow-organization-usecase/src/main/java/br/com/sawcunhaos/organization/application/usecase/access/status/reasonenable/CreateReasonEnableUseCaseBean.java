
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateReasonEnableRequest;
import br.com.sawcunhaos.organization.api.dto.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link CreateReasonEnableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateReasonEnableUseCaseBean implements CreateReasonEnableUseCase {

    private final ReasonEnableService reasonEnableService;

    @Override
    public ReasonEnable execute(@NonNull CreateReasonEnableRequest createReasonEnableRequest) {
        log.info("Create reason enable: {}", createReasonEnableRequest.code());

        ReasonEnableInput reasonEnableInput = ReasonEnableInput.builder()
                .code(createReasonEnableRequest.code())
                .description(createReasonEnableRequest.description())
                .entityType(ReasonEnableApiMapper.toDomainEntityType(createReasonEnableRequest.entityType()))
                .build();

        ReasonEnableOutput reasonEnableOutput = reasonEnableService.create(reasonEnableInput);
        return ReasonEnableApiMapper.toApiReasonEnable(reasonEnableOutput);
    }
}
