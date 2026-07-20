
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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype;

import br.com.sawcunhaos.organization.api.dto.UpdateAddressTypeRequest;
import org.jspecify.annotations.NonNull;

/** Atualiza um tipo de endereço existente (UC-084). */
public interface UpdateAddressTypeUseCase {
    void execute(@NonNull Long id, @NonNull UpdateAddressTypeRequest updateAddressTypeRequest);
}
