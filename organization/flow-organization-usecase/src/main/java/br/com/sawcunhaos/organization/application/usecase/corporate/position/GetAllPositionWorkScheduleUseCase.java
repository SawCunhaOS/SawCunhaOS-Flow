
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

import br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** Lista o template de Jornada de Trabalho do Cargo (UC-144). */
public interface GetAllPositionWorkScheduleUseCase {
    List<PositionWorkSchedule> execute(@NonNull Long positionId);
}
