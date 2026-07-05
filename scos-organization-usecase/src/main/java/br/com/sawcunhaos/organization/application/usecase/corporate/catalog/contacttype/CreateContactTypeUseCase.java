
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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype;

import br.com.sawcunhaos.organization.api.dto.ContactType;
import br.com.sawcunhaos.organization.api.dto.CreateContactTypeRequest;
import org.jspecify.annotations.NonNull;

/** Cria um novo tipo de contato (UC-088). */
public interface CreateContactTypeUseCase {
    ContactType execute(@NonNull CreateContactTypeRequest createContactTypeRequest);
}
