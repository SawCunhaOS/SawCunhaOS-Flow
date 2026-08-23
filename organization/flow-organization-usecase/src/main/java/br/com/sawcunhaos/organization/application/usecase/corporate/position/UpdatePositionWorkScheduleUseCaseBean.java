
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
import br.com.sawcunhaos.organization.api.dto.UpdatePositionWorkScheduleRequest;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleInput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdatePositionWorkScheduleUseCase} — delega a {@link PositionWorkScheduleService#update}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdatePositionWorkScheduleUseCaseBean implements UpdatePositionWorkScheduleUseCase {

    private final PositionWorkScheduleService positionWorkScheduleService;

    @Override
    public void execute(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek, @NonNull UpdatePositionWorkScheduleRequest request) {
        log.info("Update PositionWorkSchedule: {} / {}", positionId, dayOfWeek);

        PositionWorkScheduleInput input = PositionWorkScheduleInput.builder()
                .startTime(request.startTime())
                .lunchStart(request.lunchStart())
                .lunchEnd(request.lunchEnd())
                .endTime(request.endTime())
                .build();

        positionWorkScheduleService.update(
                positionId,
                br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek.valueOf(dayOfWeek.name()),
                input
        );
    }
}
