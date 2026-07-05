
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

import br.com.sawcunhaos.organization.api.dto.UpdateReasonActivateRequest;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateInput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link UpdateReasonActivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class UpdateReasonActivateUseCaseBean implements UpdateReasonActivateUseCase {
    private final ReasonActivateService reasonActivateService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateReasonActivateRequest updateReasonActivateRequest) {
        reasonActivateService.update(
                ReasonActivateInput.builder()
                        .id(id)
                        .code(updateReasonActivateRequest.code())
                        .description(updateReasonActivateRequest.description())
                        .entityType(ReasonActivateApiMapper.toDomainEntityType(updateReasonActivateRequest.entityType()))
                        .build()
        );
    }
}
