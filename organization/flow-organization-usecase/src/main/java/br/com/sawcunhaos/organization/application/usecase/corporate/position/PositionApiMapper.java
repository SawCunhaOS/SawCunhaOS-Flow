
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

import br.com.sawcunhaos.organization.api.dto.DayOfWeek;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.api.dto.PositionWorkSchedule;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionWorkScheduleOutput;

final class PositionApiMapper {

    private PositionApiMapper() {
    }

    static Position toApiPosition(PositionOutput positionOutput) {
        return Position.builder()
                .id(positionOutput.id())
                .code(positionOutput.code())
                .description(positionOutput.description())
                .active(positionOutput.active())
                .isTrustPosition(positionOutput.isTrustPosition())
                .department(toApiDepartment(positionOutput.department()))
                .build();
    }

    static PositionWorkSchedule toApiPositionWorkSchedule(PositionWorkScheduleOutput output) {
        return PositionWorkSchedule.builder()
                .dayOfWeek(DayOfWeek.valueOf(output.dayOfWeek().name()))
                .startTime(output.startTime())
                .lunchStart(output.lunchStart())
                .lunchEnd(output.lunchEnd())
                .endTime(output.endTime())
                .build();
    }

    static Department toApiDepartment(DepartmentOutput departmentOutput) {
        if (departmentOutput == null) {
            return null;
        }
        return Department.builder()
                .id(departmentOutput.id())
                .code(departmentOutput.code())
                .description(departmentOutput.description())
                .active(departmentOutput.active())
                .build();
    }
}
