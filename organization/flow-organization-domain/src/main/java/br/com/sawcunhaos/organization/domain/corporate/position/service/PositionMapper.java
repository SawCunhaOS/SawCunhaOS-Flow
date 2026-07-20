
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

import br.com.sawcunhaos.organization.domain.corporate.department.service.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.internal.Position;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = DepartmentMapper.class)
public interface PositionMapper {

    @Mapping(target = "isTrustPosition", source = "trustPosition")
    @Mapping(target = "department", ignore = true)
    PositionOutput toPositionOutput(Position position);

}
