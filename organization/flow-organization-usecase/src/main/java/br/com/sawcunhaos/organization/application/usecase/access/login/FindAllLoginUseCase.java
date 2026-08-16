
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
import br.com.sawcunhaos.organization.api.dto.LoginStatus;
import br.com.sawcunhaos.organization.api.dto.LoginType;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.jspecify.annotations.NonNull;

/** Lista Logins com paginação e filtro opcional por type/status/employeeId (UC-058). */
public interface FindAllLoginUseCase {
    GetAllLoginsResponse execute(@NonNull PaginationFilter paginationFilter, LoginType type, LoginStatus status, Long employeeId);
}
