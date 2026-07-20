
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae;

import br.com.sawcunhaos.organization.api.dto.GetAllCnaesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.jspecify.annotations.NonNull;

/** Lista paginada de CNAEs (UC-098). */
public interface FindAllCnaeUseCase {
    GetAllCnaesResponse execute(@NonNull PaginationFilter paginationFilter);
}
