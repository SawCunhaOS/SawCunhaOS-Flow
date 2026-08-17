
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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDayCalculatorTest {

    private final BusinessDayCalculator calculator = new BusinessDayCalculator();

    @Test
    void plusBusinessDaysShouldAddOneDayWhenNoWeekendInBetween() {
        // 2026-08-17 é segunda-feira
        Instant monday = Instant.parse("2026-08-17T10:00:00Z");

        Instant result = calculator.plusBusinessDays(monday, 1, ZoneOffset.UTC);

        assertThat(result).isEqualTo(Instant.parse("2026-08-18T10:00:00Z"));
    }

    @Test
    void plusBusinessDaysShouldSkipWeekendWhenLandingOnSaturday() {
        // 2026-08-14 é sexta-feira -> +1 dia útil pula sábado/domingo, cai na segunda 2026-08-17
        Instant friday = Instant.parse("2026-08-14T10:00:00Z");

        Instant result = calculator.plusBusinessDays(friday, 1, ZoneOffset.UTC);

        assertThat(result).isEqualTo(Instant.parse("2026-08-17T10:00:00Z"));
    }

    @Test
    void plusBusinessDaysShouldSkipWeekendWhenStartingOnSaturday() {
        // 2026-08-15 é sábado -> +1 dia útil pula domingo, cai na segunda 2026-08-17
        Instant saturday = Instant.parse("2026-08-15T10:00:00Z");

        Instant result = calculator.plusBusinessDays(saturday, 1, ZoneOffset.UTC);

        assertThat(result).isEqualTo(Instant.parse("2026-08-17T10:00:00Z"));
    }
}
