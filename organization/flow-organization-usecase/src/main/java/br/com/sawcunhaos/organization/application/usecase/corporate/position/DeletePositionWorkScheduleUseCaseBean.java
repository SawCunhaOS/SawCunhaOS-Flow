
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
import br.com.sawcunhaos.organization.api.dto.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link DeletePositionWorkScheduleUseCase} — delega a {@link PositionWorkScheduleService#delete}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class DeletePositionWorkScheduleUseCaseBean implements DeletePositionWorkScheduleUseCase {

    private final PositionWorkScheduleService positionWorkScheduleService;

    @Override
    public void execute(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek) {
        log.info("Delete PositionWorkSchedule: {} / {}", positionId, dayOfWeek);
        positionWorkScheduleService.delete(
                positionId,
                br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek.valueOf(dayOfWeek.name())
        );
    }
}
