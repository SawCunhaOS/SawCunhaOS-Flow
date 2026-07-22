
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

import br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementação de {@link GetAllPositionWorkScheduleUseCase} — delega a {@link PositionWorkScheduleService#findAllByPositionId}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class GetAllPositionWorkScheduleUseCaseBean implements GetAllPositionWorkScheduleUseCase {

    private final PositionWorkScheduleService positionWorkScheduleService;

    @Override
    public List<PositionWorkSchedule> execute(@NonNull Long positionId) {
        log.info("Get all PositionWorkSchedule: {}", positionId);
        return positionWorkScheduleService.findAllByPositionId(positionId).stream()
                .map(PositionApiMapper::toApiPositionWorkSchedule)
                .toList();
    }
}
