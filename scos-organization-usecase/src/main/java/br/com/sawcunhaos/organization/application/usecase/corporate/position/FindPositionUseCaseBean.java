
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

import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class FindPositionUseCaseBean implements FindPositionUseCase {

    private final PositionService positionService;

    @Override
    public Position execute(@NonNull Long id) {
        log.info("Find Position : {}", id);
        PositionOutput positionOutput = positionService.findById(id);
        return PositionApiMapper.toApiPosition(positionOutput);
    }
}
