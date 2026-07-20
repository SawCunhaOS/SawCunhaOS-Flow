
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.AddressType;
import br.com.sawcunhaos.organization.api.dto.CreateAddressTypeRequest;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link CreateAddressTypeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateAddressTypeUseCaseBean implements CreateAddressTypeUseCase {

    private final AddressTypeService addressTypeService;

    @Override
    public AddressType execute(@NonNull CreateAddressTypeRequest createAddressTypeRequest) {
        log.info("Create address type: {}", createAddressTypeRequest.code());

        AddressTypeInput addressTypeInput = AddressTypeInput.builder()
                .code(createAddressTypeRequest.code())
                .description(createAddressTypeRequest.description())
                .entityType(AddressTypeApiMapper.toDomainEntityType(createAddressTypeRequest.entityType()))
                .build();

        AddressTypeOutput addressTypeOutput = addressTypeService.create(addressTypeInput);
        return AddressTypeApiMapper.toApiAddressType(addressTypeOutput);
    }
}
