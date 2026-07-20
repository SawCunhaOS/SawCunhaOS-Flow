
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

import br.com.sawcunhaos.organization.api.dto.ReasonInactivate;
import org.jspecify.annotations.NonNull;

/** Busca um motivo de inativação pelo id (UC-121). */
public interface FindReasonInactivateUseCase {
    ReasonInactivate execute(@NonNull Long id);
}
