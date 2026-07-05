
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

import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/** Implementação de {@link DisableAddressTypeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class DisableAddressTypeUseCaseBean implements DisableAddressTypeUseCase {

    private final AddressTypeService addressTypeService;

    @Override
    public void execute(@NonNull Long id) {
        log.info("Disable AddressType : {}", id);
        addressTypeService.disable(id);
    }
}
