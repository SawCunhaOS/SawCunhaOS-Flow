
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate;

import br.com.sawcunhaos.organization.api.dto.CreateReasonInactivateRequest;
import br.com.sawcunhaos.organization.api.dto.ReasonInactivate;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link CreateReasonInactivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class CreateReasonInactivateUseCaseBean implements CreateReasonInactivateUseCase {

    private final ReasonInactivateService reasonInactivateService;

    @Override
    public ReasonInactivate execute(@NonNull CreateReasonInactivateRequest createReasonInactivateRequest) {
        log.info("Create reason inactivate: {}", createReasonInactivateRequest.code());

        ReasonInactivateInput reasonInactivateInput = ReasonInactivateInput.builder()
                .code(createReasonInactivateRequest.code())
                .description(createReasonInactivateRequest.description())
                .entityType(ReasonInactivateApiMapper.toDomainEntityType(createReasonInactivateRequest.entityType()))
                .build();

        ReasonInactivateOutput reasonInactivateOutput = reasonInactivateService.create(reasonInactivateInput);
        return ReasonInactivateApiMapper.toApiReasonInactivate(reasonInactivateOutput);
    }
}
