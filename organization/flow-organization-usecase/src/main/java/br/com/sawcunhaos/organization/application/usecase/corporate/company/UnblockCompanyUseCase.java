
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

import br.com.sawcunhaos.organization.api.dto.CompanyStatusTransitionRequest;
import org.jspecify.annotations.NonNull;

/** Desbloqueia uma Empresa DISABLED (rota {@code PUT /v1/companies/{id}/unblock}, UC-006). */
public interface UnblockCompanyUseCase {
    void execute(@NonNull Long id, @NonNull CompanyStatusTransitionRequest request);
}
