
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature;

import br.com.sawcunhaos.organization.api.dto.LegalNature;
import org.jspecify.annotations.NonNull;

/** Busca uma natureza jurídica pelo id (UC-095). */
public interface FindLegalNatureUseCase {
    LegalNature execute(@NonNull Long id);
}
