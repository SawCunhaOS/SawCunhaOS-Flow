
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

package br.com.sawcunhaos.organization.application.mapper.department;


import br.com.sawcunhaos.organization.application.dto.CreateDepartmentDTO;
import br.com.sawcunhaos.organization.application.dto.DepartmentDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateDepartmentDTO;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    Department toDepartment(CreateDepartmentDTO createDepartmentDTO);
    Department toDepartment(UpdateDepartmentDTO updateDepartmentDTO);
    DepartmentDTO toDepartmentDTO(Department department);

}
