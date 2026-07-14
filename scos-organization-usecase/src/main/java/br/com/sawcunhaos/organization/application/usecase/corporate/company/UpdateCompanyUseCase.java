
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

import br.com.sawcunhaos.organization.api.dto.UpdateCompanyRequest;
import org.jspecify.annotations.NonNull;

/** Atualiza os dados cadastrais de uma empresa (UC-005). */
public interface UpdateCompanyUseCase {
    void execute(@NonNull Long id, @NonNull UpdateCompanyRequest updateCompanyRequest);
}
