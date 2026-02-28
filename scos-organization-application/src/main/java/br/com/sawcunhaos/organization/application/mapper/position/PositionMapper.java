
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

package br.com.sawcunhaos.organization.application.mapper.position;

import br.com.sawcunhaos.organization.application.dto.CreatePositionDTO;
import br.com.sawcunhaos.organization.application.dto.PositionDTO;
import br.com.sawcunhaos.organization.application.dto.UpdatePositionDTO;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.model.department.Position;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PositionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", expression = "java(mapDepartmentFromId(createPositionDTO.getDepartmentId()))")
    Position toPosition(CreatePositionDTO createPositionDTO);

    @Mapping(target = "id", source = "positionId")
    @Mapping(target = "department", expression = "java(mapDepartmentFromId(updatePositionDTO.getDepartmentId()))")
    Position toPosition(UpdatePositionDTO updatePositionDTO);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentCode", source = "department.code")
    @Mapping(target = "departmentDescription", source = "department.description")
    @Mapping(target = "departmentActive", source = "department.active")
    PositionDTO toPositionDTO(Position position);

    default Department mapDepartmentFromId(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return Department.builder().id(departmentId).build();
    }

}

