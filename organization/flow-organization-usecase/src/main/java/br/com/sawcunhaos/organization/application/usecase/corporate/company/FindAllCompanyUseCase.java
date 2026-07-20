
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company;

import br.com.sawcunhaos.organization.api.dto.GetAllCompaniesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import org.jspecify.annotations.NonNull;

/** Lista empresas com paginação e filtro por status e/ou nome (UC-004). */
public interface FindAllCompanyUseCase {
    GetAllCompaniesResponse execute(@NonNull PaginationFilter paginationFilter, StatusCompany status, String name);
}
