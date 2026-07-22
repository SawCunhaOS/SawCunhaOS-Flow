
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

package br.com.sawcunhaos.organization.domain.corporate.position.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionWorkScheduleRepository;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_003;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes de {@link PositionWorkScheduleServiceBean}: ordem cronológica, existência do Cargo, unicidade do par. */
@ExtendWith(MockitoExtension.class)
class PositionWorkScheduleServiceBeanTest {

    @Mock
    private PositionWorkScheduleRepository positionWorkScheduleRepository;
    @Mock
    private PositionService positionService;
    @Mock
    private PositionWorkScheduleMapper positionWorkScheduleMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private PositionWorkScheduleServiceBean positionWorkScheduleServiceBean;

    private PositionWorkScheduleInput.PositionWorkScheduleInputBuilder validInput() {
        return PositionWorkScheduleInput.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .lunchStart(LocalTime.of(12, 0))
                .lunchEnd(LocalTime.of(13, 0))
                .endTime(LocalTime.of(17, 0));
    }

    // ---- findAllByPositionId ----

    @Test
    void findAllByPositionIdShouldReturnMappedListWhenPositionExists() {
        Position position = Position.builder().id(1L).build();
        PositionWorkSchedule entity = PositionWorkSchedule.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();
        PositionWorkScheduleOutput output = PositionWorkScheduleOutput.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();

        when(positionService.findPositionById(1L)).thenReturn(position);
        when(positionWorkScheduleRepository.findAllByPositionId(1L)).thenReturn(List.of(entity));
        when(positionWorkScheduleMapper.toOutput(entity)).thenReturn(output);

        List<PositionWorkScheduleOutput> result = positionWorkScheduleServiceBean.findAllByPositionId(1L);

        assertThat(result).containsExactly(output);
    }

    @Test
    void findAllByPositionIdShouldThrowWhenPositionDoesNotExist() {
        when(positionService.findPositionById(999L)).thenThrow(new ScosException(SCOS_POSITION_001));

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.findAllByPositionId(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenValid() {
        Position position = Position.builder().id(1L).build();
        PositionWorkSchedule persisted = PositionWorkSchedule.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();
        PositionWorkScheduleOutput output = PositionWorkScheduleOutput.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();

        when(positionService.findPositionById(1L)).thenReturn(position);
        when(positionWorkScheduleRepository.existsByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(false);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");
        when(positionWorkScheduleRepository.merge(any(PositionWorkSchedule.class))).thenReturn(persisted);
        when(positionWorkScheduleMapper.toOutput(persisted)).thenReturn(output);

        PositionWorkScheduleOutput result = positionWorkScheduleServiceBean.create(1L, validInput().build());

        assertThat(result).isEqualTo(output);

        ArgumentCaptor<PositionWorkSchedule> captor = ArgumentCaptor.forClass(PositionWorkSchedule.class);
        verify(positionWorkScheduleRepository).merge(captor.capture());
        assertThat(captor.getValue().getPosition()).isEqualTo(position);
        assertThat(captor.getValue().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void createShouldThrowWhenChronologicalOrderIsInvalid() {
        PositionWorkScheduleInput input = validInput().lunchStart(LocalTime.of(13, 30)).build();

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.create(1L, input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_003.getCode());

        verify(positionService, never()).findPositionById(any());
        verify(positionWorkScheduleRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenPositionDoesNotExist() {
        when(positionService.findPositionById(999L)).thenThrow(new ScosException(SCOS_POSITION_001));

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.create(999L, validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_001.getCode());

        verify(positionWorkScheduleRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenPairAlreadyExists() {
        Position position = Position.builder().id(1L).build();
        when(positionService.findPositionById(1L)).thenReturn(position);
        when(positionWorkScheduleRepository.existsByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(true);

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.create(1L, validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_002.getCode());

        verify(positionWorkScheduleRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldPersistWhenValid() {
        PositionWorkSchedule schedule = PositionWorkSchedule.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();
        when(positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(Optional.of(schedule));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("tester");

        PositionWorkScheduleInput input = validInput().startTime(LocalTime.of(9, 0)).build();
        positionWorkScheduleServiceBean.update(1L, DayOfWeek.MONDAY, input);

        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.getUserAt()).isEqualTo("tester");
        verify(positionWorkScheduleRepository).update(schedule);
    }

    @Test
    void updateShouldThrowWhenChronologicalOrderIsInvalid() {
        PositionWorkScheduleInput input = validInput().lunchEnd(LocalTime.of(11, 0)).build();

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.update(1L, DayOfWeek.MONDAY, input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_003.getCode());

        verify(positionWorkScheduleRepository, never()).findByPositionIdAndDayOfWeek(any(), any());
        verify(positionWorkScheduleRepository, never()).update(any(PositionWorkSchedule.class));
    }

    @Test
    void updateShouldThrowWhenPairDoesNotExist() {
        when(positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.update(1L, DayOfWeek.MONDAY, validInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_001.getCode());

        verify(positionWorkScheduleRepository, never()).update(any(PositionWorkSchedule.class));
    }

    // ---- delete ----

    @Test
    void deleteShouldRemoveWhenPairExists() {
        PositionWorkSchedule schedule = PositionWorkSchedule.builder().id(10L).dayOfWeek(DayOfWeek.MONDAY).build();
        when(positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(Optional.of(schedule));

        positionWorkScheduleServiceBean.delete(1L, DayOfWeek.MONDAY);

        verify(positionWorkScheduleRepository).delete(schedule);
    }

    @Test
    void deleteShouldThrowWhenPairDoesNotExist() {
        when(positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(1L, DayOfWeek.MONDAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> positionWorkScheduleServiceBean.delete(1L, DayOfWeek.MONDAY))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_POSITION_WORK_SCHEDULE_001.getCode());

        verify(positionWorkScheduleRepository, never()).delete(any(PositionWorkSchedule.class));
    }
}
