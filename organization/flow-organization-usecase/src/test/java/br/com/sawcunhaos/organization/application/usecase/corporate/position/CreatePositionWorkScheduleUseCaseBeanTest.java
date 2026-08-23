
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
import br.com.sawcunhaos.organization.api.dto.CreatePositionWorkScheduleRequest;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link CreatePositionWorkScheduleUseCaseBean}: mapeamento request→input e retorno do id. */
@ExtendWith(MockitoExtension.class)
class CreatePositionWorkScheduleUseCaseBeanTest {

    @Mock
    private PositionWorkScheduleService positionWorkScheduleService;

    @InjectMocks
    private CreatePositionWorkScheduleUseCaseBean useCase;

    private CreatePositionWorkScheduleRequest request() {
        return new CreatePositionWorkScheduleRequest(
                br.com.sawcunhaos.organization.api.dto.DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                LocalTime.of(17, 0)
        );
    }

    @Test
    @DisplayName("mapeia request para input e retorna o id gerado")
    void executes_mapsRequestToInputAndReturnsId() {
        PositionWorkScheduleOutput output = PositionWorkScheduleOutput.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();
        given(positionWorkScheduleService.create(eq(7L), any(PositionWorkScheduleInput.class))).willReturn(output);

        Long result = useCase.execute(7L, request());

        ArgumentCaptor<PositionWorkScheduleInput> captor = ArgumentCaptor.forClass(PositionWorkScheduleInput.class);
        then(positionWorkScheduleService).should().create(eq(7L), captor.capture());
        PositionWorkScheduleInput input = captor.getValue();
        assertThat(input.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(input.startTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(input.lunchStart()).isEqualTo(LocalTime.of(12, 0));
        assertThat(input.lunchEnd()).isEqualTo(LocalTime.of(13, 0));
        assertThat(input.endTime()).isEqualTo(LocalTime.of(17, 0));

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(positionWorkScheduleService.create(eq(7L), any(PositionWorkScheduleInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(7L, request())).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(7L, null)).isInstanceOf(NullPointerException.class);
        then(positionWorkScheduleService).shouldHaveNoInteractions();
    }
}
