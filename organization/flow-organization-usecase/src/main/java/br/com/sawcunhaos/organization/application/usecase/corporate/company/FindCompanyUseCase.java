
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
import org.jspecify.annotations.NonNull;

/** Busca uma empresa pelo id (UC-003). */
public interface FindCompanyUseCase {
    Company execute(@NonNull Long id);
}
