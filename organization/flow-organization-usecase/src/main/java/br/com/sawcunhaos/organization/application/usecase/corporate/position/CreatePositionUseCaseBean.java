
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreatePositionRequest;
import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
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
class CreatePositionUseCaseBean implements CreatePositionUseCase {

    private final PositionService positionService;

    @Override
    public Position execute(@NonNull CreatePositionRequest createPositionRequest) {
        log.info("Create position: {}", createPositionRequest.code());

        PositionInput positionInput = PositionInput.builder()
                .code(createPositionRequest.code())
                .description(createPositionRequest.description())
                .departmentId(createPositionRequest.departmentId())
                .isTrustPosition(Boolean.TRUE.equals(createPositionRequest.isTrustPosition()))
                .build();

        PositionOutput positionOutput = positionService.create(positionInput);

        return PositionApiMapper.toApiPosition(positionOutput);
    }
}
