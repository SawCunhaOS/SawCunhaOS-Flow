
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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype;

import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.GetAllAddressTypesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.jspecify.annotations.NonNull;

/** Lista paginada de tipos de endereço, com filtro opcional por {@code entityType} (UC-081). */
public interface FindAllAddressTypeUseCase {
    GetAllAddressTypesResponse execute(@NonNull PaginationFilter paginationFilter, CatalogEntityType entityType);
}
