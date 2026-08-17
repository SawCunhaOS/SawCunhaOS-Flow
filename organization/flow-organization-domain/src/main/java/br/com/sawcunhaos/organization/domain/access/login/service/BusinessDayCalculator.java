
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Soma dias úteis (pula sábado/domingo) a um {@link Instant}. Usa {@code java.time.DayOfWeek} -
 * não confundir com {@code corporate.position.internal.DayOfWeek} (enum próprio do projeto para
 * jornada de trabalho). Sem calendário de feriados - fora de escopo desta story.
 */
@Component
public class BusinessDayCalculator {

    public Instant plusBusinessDays(Instant from, int days, ZoneId zone) {
        ZonedDateTime current = from.atZone(zone);
        int added = 0;
        while (added < days) {
            current = current.plusDays(1);
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return current.toInstant();
    }
}
