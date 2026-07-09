
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonEnableRequest;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateReasonEnableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateReasonEnableUseCaseBean implements UpdateReasonEnableUseCase {
    private final ReasonEnableService reasonEnableService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateReasonEnableRequest updateReasonEnableRequest) {
        reasonEnableService.update(
                ReasonEnableInput.builder()
                        .id(id)
                        .code(updateReasonEnableRequest.code())
                        .description(updateReasonEnableRequest.description())
                        .entityType(ReasonEnableApiMapper.toDomainEntityType(updateReasonEnableRequest.entityType()))
                        .build()
        );
    }
}
