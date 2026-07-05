
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

import br.com.sawcunhaos.organization.api.dto.UpdateReasonInactivateRequest;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateInput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link UpdateReasonInactivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class UpdateReasonInactivateUseCaseBean implements UpdateReasonInactivateUseCase {
    private final ReasonInactivateService reasonInactivateService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateReasonInactivateRequest updateReasonInactivateRequest) {
        reasonInactivateService.update(
                ReasonInactivateInput.builder()
                        .id(id)
                        .code(updateReasonInactivateRequest.code())
                        .description(updateReasonInactivateRequest.description())
                        .entityType(ReasonInactivateApiMapper.toDomainEntityType(updateReasonInactivateRequest.entityType()))
                        .build()
        );
    }
}
