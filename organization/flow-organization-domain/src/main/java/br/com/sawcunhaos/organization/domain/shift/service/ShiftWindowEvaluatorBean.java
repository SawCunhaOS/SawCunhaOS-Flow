
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

package br.com.sawcunhaos.organization.domain.shift.service;

import br.com.sawcunhaos.organization.domain.shift.specification.ShiftWindowEvaluator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
class ShiftWindowEvaluatorBean implements ShiftWindowEvaluator {

    @Override
    public boolean isWithinShift(
            Instant instant, ZoneId zoneId,
            LocalTime startTime, LocalTime lunchStart, LocalTime lunchEnd, LocalTime endTime
    ) {
        LocalTime localTime = instant.atZone(zoneId).toLocalTime();
        return isWithinWindow(localTime, startTime, endTime)
                && !isWithinWindow(localTime, lunchStart, lunchEnd);
    }

    private boolean isWithinWindow(LocalTime time, LocalTime start, LocalTime end) {
        if (!start.isAfter(end)) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
        // cruza meia-noite (ex.: 22:00 -> 06:00)
        return !time.isBefore(start) || !time.isAfter(end);
    }
}
