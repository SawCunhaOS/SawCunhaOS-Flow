
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonDisableRequest;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableInput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateReasonDisableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateReasonDisableUseCaseBean implements UpdateReasonDisableUseCase {
    private final ReasonDisableService reasonDisableService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateReasonDisableRequest updateReasonDisableRequest) {
        reasonDisableService.update(
                ReasonDisableInput.builder()
                        .id(id)
                        .code(updateReasonDisableRequest.code())
                        .description(updateReasonDisableRequest.description())
                        .entityType(ReasonDisableApiMapper.toDomainEntityType(updateReasonDisableRequest.entityType()))
                        .build()
        );
    }
}
