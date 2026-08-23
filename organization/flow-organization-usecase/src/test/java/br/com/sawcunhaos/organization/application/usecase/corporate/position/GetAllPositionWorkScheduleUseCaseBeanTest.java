
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
import br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link GetAllPositionWorkScheduleUseCaseBean}: mapeamento output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class GetAllPositionWorkScheduleUseCaseBeanTest {

    @Mock
    private PositionWorkScheduleService positionWorkScheduleService;

    @InjectMocks
    private GetAllPositionWorkScheduleUseCaseBean useCase;

    @Test
    @DisplayName("mapeia a lista de output para api.dto.PositionWorkSchedule")
    void executes_mapsOutputListToApi() {
        PositionWorkScheduleOutput output = PositionWorkScheduleOutput.builder()
                .id(10L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .lunchStart(LocalTime.of(12, 0))
                .lunchEnd(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0))
                .build();
        given(positionWorkScheduleService.findAllByPositionId(7L)).willReturn(List.of(output));

        List<PositionWorkSchedule> result = useCase.execute(7L);

        then(positionWorkScheduleService).should().findAllByPositionId(7L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).dayOfWeek().name()).isEqualTo("MONDAY");
        assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(positionWorkScheduleService.findAllByPositionId(7L)).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(7L)).isInstanceOf(ScosException.class);
    }
}
