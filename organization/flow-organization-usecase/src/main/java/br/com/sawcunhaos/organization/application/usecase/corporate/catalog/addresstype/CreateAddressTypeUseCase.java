
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

import br.com.sawcunhaos.organization.api.dto.AddressType;
import br.com.sawcunhaos.organization.api.dto.CreateAddressTypeRequest;
import org.jspecify.annotations.NonNull;

/** Cria um novo tipo de endereço (UC-082). */
public interface CreateAddressTypeUseCase {
    AddressType execute(@NonNull CreateAddressTypeRequest createAddressTypeRequest);
}
