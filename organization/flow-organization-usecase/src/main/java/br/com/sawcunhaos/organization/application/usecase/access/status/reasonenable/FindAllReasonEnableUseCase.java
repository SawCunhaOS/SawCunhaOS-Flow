
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable;

import br.com.sawcunhaos.organization.api.dto.GetAllReasonEnableResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import org.jspecify.annotations.NonNull;

/** Lista paginada de motivos de desbloqueio, com filtro opcional por {@code entityType} (UC-131). */
public interface FindAllReasonEnableUseCase {
    GetAllReasonEnableResponse execute(@NonNull PaginationFilter paginationFilter,
                                       ReasonEntityType entityType,
                                       Boolean active
    );
}
