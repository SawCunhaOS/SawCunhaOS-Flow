
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.organization.api.dto.GetAllLoginsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.jspecify.annotations.NonNull;

/** Lista todos os Logins de um Funcionário, todos os status, sem outros filtros (UC-055). */
public interface FindAllEmployeeLoginUseCase {
    GetAllLoginsResponse execute(@NonNull Long employeeId, @NonNull PaginationFilter paginationFilter);
}
