
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

import br.com.sawcunhaos.organization.api.dto.Company;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyRequest;
import org.jspecify.annotations.NonNull;

/** Cria uma empresa matriz ou filial (UC-001/UC-002). */
public interface CreateCompanyUseCase {
    Company execute(@NonNull CreateCompanyRequest createCompanyRequest);
}
