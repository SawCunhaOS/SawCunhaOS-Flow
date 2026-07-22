
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
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionWorkScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_WORK_SCHEDULE_003;

/**
 * Implementação de {@link PositionWorkScheduleService} — template de Jornada de Trabalho por Cargo.
 * Configuração direta, sem histórico/transição de status. Ordem de guarda deliberada: cronológica
 * (422, sem round-trip de banco) antes de existência (404) e unicidade (409) — ver UC-144..147.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class PositionWorkScheduleServiceBean implements PositionWorkScheduleService {

    private final PositionWorkScheduleRepository positionWorkScheduleRepository;
    private final PositionService positionService;
    private final PositionWorkScheduleMapper positionWorkScheduleMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    @Transactional(readOnly = true)
    public List<PositionWorkScheduleOutput> findAllByPositionId(@NonNull Long positionId) {
        log.info("Find All PositionWorkSchedule by PositionId: {}", positionId);
        positionService.findPositionById(positionId);
        return positionWorkScheduleRepository.findAllByPositionId(positionId).stream()
                .map(positionWorkScheduleMapper::toOutput)
                .toList();
    }

    /**
     * @throws ScosException SCOS_POSITION_WORK_SCHEDULE_003 (422) se a ordem cronológica for inválida.
     * @throws ScosException SCOS_POSITION_001 (404) se o Cargo não existir.
     * @throws ScosException SCOS_POSITION_WORK_SCHEDULE_002 (409) se já existir horário pro par.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public PositionWorkScheduleOutput create(@NonNull Long positionId, @NonNull PositionWorkScheduleInput input) {
        log.info("Create PositionWorkSchedule: {} / {}", positionId, input.dayOfWeek());
        assertChronologicalOrder(input);
        Position position = positionService.findPositionById(positionId);
        if (positionWorkScheduleRepository.existsByPositionIdAndDayOfWeek(positionId, input.dayOfWeek())) {
            throw new ScosException(SCOS_POSITION_WORK_SCHEDULE_002);
        }

        PositionWorkSchedule schedule = PositionWorkSchedule.builder()
                .position(position)
                .dayOfWeek(input.dayOfWeek())
                .startTime(input.startTime())
                .lunchStart(input.lunchStart())
                .lunchEnd(input.lunchEnd())
                .endTime(input.endTime())
                .build();
        schedule.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return positionWorkScheduleMapper.toOutput(positionWorkScheduleRepository.merge(schedule));
    }

    /**
     * @throws ScosException SCOS_POSITION_WORK_SCHEDULE_003 (422) se a ordem cronológica for inválida.
     * @throws ScosException SCOS_POSITION_WORK_SCHEDULE_001 (404) se não existir horário pro par.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek, @NonNull PositionWorkScheduleInput input) {
        log.info("Update PositionWorkSchedule: {} / {}", positionId, dayOfWeek);
        assertChronologicalOrder(input);
        PositionWorkSchedule schedule = findScheduleOrThrow(positionId, dayOfWeek);

        schedule.setStartTime(input.startTime());
        schedule.setLunchStart(input.lunchStart());
        schedule.setLunchEnd(input.lunchEnd());
        schedule.setEndTime(input.endTime());
        schedule.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        positionWorkScheduleRepository.update(schedule);
    }

    /**
     * @throws ScosException SCOS_POSITION_WORK_SCHEDULE_001 (404) se não existir horário pro par.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void delete(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek) {
        log.info("Delete PositionWorkSchedule: {} / {}", positionId, dayOfWeek);
        positionWorkScheduleRepository.delete(findScheduleOrThrow(positionId, dayOfWeek));
    }

    private PositionWorkSchedule findScheduleOrThrow(Long positionId, DayOfWeek dayOfWeek) {
        return positionWorkScheduleRepository.findByPositionIdAndDayOfWeek(positionId, dayOfWeek)
                .orElseThrow(() -> new ScosException(SCOS_POSITION_WORK_SCHEDULE_001));
    }

    private void assertChronologicalOrder(PositionWorkScheduleInput input) {
        boolean inOrder = input.startTime().isBefore(input.lunchStart())
                && input.lunchStart().isBefore(input.lunchEnd())
                && input.lunchEnd().isBefore(input.endTime());
        if (!inOrder) {
            throw new ScosException(SCOS_POSITION_WORK_SCHEDULE_003);
        }
    }
}
