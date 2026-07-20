
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate;

import br.com.sawcunhaos.organization.api.dto.CreateReasonInactivateRequest;
import br.com.sawcunhaos.organization.api.dto.ReasonInactivate;
import org.jspecify.annotations.NonNull;

/** Cria um novo motivo de inativação (UC-120). */
public interface CreateReasonInactivateUseCase {
    ReasonInactivate execute(@NonNull CreateReasonInactivateRequest createReasonInactivateRequest);
}
