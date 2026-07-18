
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

package br.com.sawcunhaos.organization.application.usecase.corporate.position;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdatePositionRequest;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdatePositionUseCaseBean implements UpdatePositionUseCase {
    private final PositionService positionService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdatePositionRequest updatePositionRequest) {
        positionService.update(
                PositionInput.builder()
                        .id(id)
                        .code(updatePositionRequest.code())
                        .description(updatePositionRequest.description())
                        .departmentId(updatePositionRequest.departmentId())
                        .isTrustPosition(Boolean.TRUE.equals(updatePositionRequest.isTrustPosition()))
                        .build()
        );
    }
}
