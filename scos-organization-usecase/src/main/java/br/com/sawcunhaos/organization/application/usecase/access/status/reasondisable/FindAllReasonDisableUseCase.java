
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable;

import br.com.sawcunhaos.organization.api.dto.GetAllReasonDisableResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import org.jspecify.annotations.NonNull;

/** Lista paginada de motivos de bloqueio, com filtro opcional por {@code entityType} (UC-125). */
public interface FindAllReasonDisableUseCase {
    GetAllReasonDisableResponse execute(@NonNull PaginationFilter paginationFilter,
                                        ReasonEntityType entityType,
                                        Boolean active
    );
}
