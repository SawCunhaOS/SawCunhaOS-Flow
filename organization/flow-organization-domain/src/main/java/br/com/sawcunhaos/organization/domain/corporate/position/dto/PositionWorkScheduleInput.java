
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

package br.com.sawcunhaos.organization.domain.corporate.position.dto;

import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record PositionWorkScheduleInput(
        Long positionId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime lunchStart,
        LocalTime lunchEnd,
        LocalTime endTime
) {
}
