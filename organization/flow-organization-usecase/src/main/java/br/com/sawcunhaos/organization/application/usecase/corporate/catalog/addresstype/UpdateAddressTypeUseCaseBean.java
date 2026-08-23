
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateAddressTypeRequest;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateAddressTypeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateAddressTypeUseCaseBean implements UpdateAddressTypeUseCase {
    private final AddressTypeService addressTypeService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateAddressTypeRequest updateAddressTypeRequest) {
        addressTypeService.update(
                AddressTypeInput.builder()
                        .id(id)
                        .code(updateAddressTypeRequest.code())
                        .description(updateAddressTypeRequest.description())
                        .entityType(AddressTypeApiMapper.toDomainEntityType(updateAddressTypeRequest.entityType()))
                        .build()
        );
    }
}
