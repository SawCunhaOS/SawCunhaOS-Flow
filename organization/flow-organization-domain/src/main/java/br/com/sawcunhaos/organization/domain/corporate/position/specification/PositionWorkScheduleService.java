
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

package br.com.sawcunhaos.organization.domain.corporate.position.specification;

import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.DayOfWeek;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Casos de uso de domínio do template de Jornada de Trabalho por Cargo (UC-144..147).
 */
public interface PositionWorkScheduleService {

    /** Lista o template de jornada do Cargo, ordenado por {@code dayOfWeek}. */
    List<PositionWorkScheduleOutput> findAllByPositionId(@NonNull Long positionId);

    /** Cria um horário para um dia da semana ainda não cadastrado para o Cargo. */
    PositionWorkScheduleOutput create(@NonNull Long positionId, @NonNull PositionWorkScheduleInput input);

    /** Atualiza o horário já cadastrado para o par {@code (positionId, dayOfWeek)}. */
    void update(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek, @NonNull PositionWorkScheduleInput input);

    /** Remove o horário cadastrado para o par {@code (positionId, dayOfWeek)}. */
    void delete(@NonNull Long positionId, @NonNull DayOfWeek dayOfWeek);

}
