
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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de {@link ShiftWindowEvaluatorBean}. Sem Mockito — a Bean não tem dependência nenhuma,
 * instanciada direto. Instantes construídos via {@code ZonedDateTime.of(...).toInstant()} para
 * evitar aritmética manual de offset UTC.
 */
class ShiftWindowEvaluatorBeanTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 19);
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId MANAUS = ZoneId.of("America/Manaus");

    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(17, 0);

    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0);

    private final ShiftWindowEvaluatorBean evaluator = new ShiftWindowEvaluatorBean();

    private static Instant instantAt(ZoneId zone, LocalTime localTime) {
        return ZonedDateTime.of(DAY, localTime, zone).toInstant();
    }

    @Test
    void sameInstantInDifferentTimeZonesYieldsDifferentDecision() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(8, 30));

        boolean withinShiftSaoPaulo = evaluator.isWithinShift(
                instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END);
        boolean withinShiftManaus = evaluator.isWithinShift(
                instant, MANAUS, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END);

        assertThat(instant.atZone(SAO_PAULO).toLocalTime()).isNotEqualTo(instant.atZone(MANAUS).toLocalTime());
        assertThat(withinShiftSaoPaulo).isTrue();
        assertThat(withinShiftManaus).isFalse();
    }

    @Test
    void normalShiftAcceptsTimeWithinWindow() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(10, 0));

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isTrue();
    }

    @Test
    void normalShiftRejectsTimeBeforeStart() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(7, 0));

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isFalse();
    }

    @Test
    void normalShiftRejectsTimeAfterEnd() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(18, 0));

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isFalse();
    }

    @Test
    void boundaryAtExactStartTimeIsWithinShift() {
        Instant instant = instantAt(SAO_PAULO, SHIFT_START);

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isTrue();
    }

    @Test
    void boundaryAtExactEndTimeIsWithinShift() {
        Instant instant = instantAt(SAO_PAULO, SHIFT_END);

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isTrue();
    }

    @Test
    void lunchWindowIsExcludedFromShiftEvenWhenWithinShiftBounds() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(12, 30));

        assertThat(evaluator.isWithinShift(instant, SAO_PAULO, SHIFT_START, LUNCH_START, LUNCH_END, SHIFT_END))
                .isFalse();
    }

    @Test
    void nightShiftCrossingMidnightAcceptsLateEveningTime() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(23, 30));

        assertThat(evaluator.isWithinShift(
                instant, SAO_PAULO, NIGHT_SHIFT_START, LUNCH_START, LUNCH_END, NIGHT_SHIFT_END))
                .isTrue();
    }

    @Test
    void nightShiftCrossingMidnightRejectsMorningTimeAfterEnd() {
        Instant instant = instantAt(SAO_PAULO, LocalTime.of(7, 0));

        assertThat(evaluator.isWithinShift(
                instant, SAO_PAULO, NIGHT_SHIFT_START, LUNCH_START, LUNCH_END, NIGHT_SHIFT_END))
                .isFalse();
    }
}
