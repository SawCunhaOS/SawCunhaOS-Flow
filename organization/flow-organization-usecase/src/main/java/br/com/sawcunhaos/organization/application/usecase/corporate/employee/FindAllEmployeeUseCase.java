
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

package br.com.sawcunhaos.organization.application.usecase.corporate.employee;

import br.com.sawcunhaos.organization.api.dto.EmployeeStatus;
import br.com.sawcunhaos.organization.api.dto.GetAllEmployeesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.jspecify.annotations.NonNull;

/** Lista Funcionários com paginação e filtro opcional por companyId/positionId/status (UC-036). */
public interface FindAllEmployeeUseCase {
    GetAllEmployeesResponse execute(@NonNull PaginationFilter paginationFilter, Long companyId, Long positionId, EmployeeStatus status);
}
