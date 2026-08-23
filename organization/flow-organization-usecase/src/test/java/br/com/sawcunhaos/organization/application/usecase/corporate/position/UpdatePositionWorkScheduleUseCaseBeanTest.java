
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
import br.com.sawcunhaos.organization.api.dto.UpdatePositionWorkScheduleRequest;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleInput;
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
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link UpdatePositionWorkScheduleUseCaseBean}: mapeamento request→input e conversão do enum de dia. */
@ExtendWith(MockitoExtension.class)
class UpdatePositionWorkScheduleUseCaseBeanTest {

    @Mock
    private PositionWorkScheduleService positionWorkScheduleService;

    @InjectMocks
    private UpdatePositionWorkScheduleUseCaseBean useCase;

    private UpdatePositionWorkScheduleRequest request() {
        return new UpdatePositionWorkScheduleRequest(
                LocalTime.of(9, 0),
                LocalTime.of(12, 30),
                LocalTime.of(13, 30),
                LocalTime.of(18, 0)
        );
    }

    @Test
    @DisplayName("mapeia request para input e converte o dayOfWeek para o enum de domínio")
    void executes_mapsRequestToInputAndConvertsDayOfWeek() {
        useCase.execute(7L, br.com.sawcunhaos.organization.api.dto.DayOfWeek.TUESDAY, request());

        ArgumentCaptor<PositionWorkScheduleInput> captor = ArgumentCaptor.forClass(PositionWorkScheduleInput.class);
        then(positionWorkScheduleService).should().update(eq(7L), eq(DayOfWeek.TUESDAY), captor.capture());
        PositionWorkScheduleInput input = captor.getValue();
        assertThat(input.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(input.lunchStart()).isEqualTo(LocalTime.of(12, 30));
        assertThat(input.lunchEnd()).isEqualTo(LocalTime.of(13, 30));
        assertThat(input.endTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(positionWorkScheduleService)
                .update(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(7L, br.com.sawcunhaos.organization.api.dto.DayOfWeek.TUESDAY, request()))
                .isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(7L, br.com.sawcunhaos.organization.api.dto.DayOfWeek.TUESDAY, null))
                .isInstanceOf(NullPointerException.class);
        then(positionWorkScheduleService).shouldHaveNoInteractions();
    }
}
