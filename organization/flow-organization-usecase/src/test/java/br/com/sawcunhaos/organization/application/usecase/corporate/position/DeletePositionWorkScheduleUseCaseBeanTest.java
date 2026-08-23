
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
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link DeletePositionWorkScheduleUseCaseBean}: delegação e conversão do enum de dia. */
@ExtendWith(MockitoExtension.class)
class DeletePositionWorkScheduleUseCaseBeanTest {

    @Mock
    private PositionWorkScheduleService positionWorkScheduleService;

    @InjectMocks
    private DeletePositionWorkScheduleUseCaseBean useCase;

    @Test
    @DisplayName("delega ao service convertendo o dayOfWeek para o enum de domínio")
    void executes_delegatesToServiceWithConvertedDayOfWeek() {
        useCase.execute(7L, br.com.sawcunhaos.organization.api.dto.DayOfWeek.WEDNESDAY);

        then(positionWorkScheduleService).should().delete(7L, DayOfWeek.WEDNESDAY);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(positionWorkScheduleService).delete(any(), any());

        assertThatThrownBy(() -> useCase.execute(7L, br.com.sawcunhaos.organization.api.dto.DayOfWeek.WEDNESDAY))
                .isInstanceOf(ScosException.class);
    }
}
