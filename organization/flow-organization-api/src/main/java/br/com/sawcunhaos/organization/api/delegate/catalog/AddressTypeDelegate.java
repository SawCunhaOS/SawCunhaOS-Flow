
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

package br.com.sawcunhaos.organization.api.delegate.catalog;

import br.com.sawcunhaos.organization.api.controller.AddressTypeApiDelegate;
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateAddressTypeRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAddressTypeResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllAddressTypesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateAddressTypeRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.CreateAddressTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.DisableAddressTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.EnableAddressTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.FindAddressTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.FindAllAddressTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype.UpdateAddressTypeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link AddressTypeApiDelegate} — expõe os 6 endpoints de {@code /v1/address-types}
 * (UC-081 a UC-086), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AddressTypeDelegate implements AddressTypeApiDelegate {

    private final FindAddressTypeUseCase findAddressTypeUseCase;
    private final FindAllAddressTypeUseCase findAllAddressTypeUseCase;
    private final DisableAddressTypeUseCase disableAddressTypeUseCase;
    private final EnableAddressTypeUseCase enableAddressTypeUseCase;
    private final CreateAddressTypeUseCase createAddressTypeUseCase;
    private final UpdateAddressTypeUseCase updateAddressTypeUseCase;

    @Override
    public GetAddressTypeResponse getAddressTypeById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetAddressTypeResponse.builder()
                .data(findAddressTypeUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllAddressTypesResponse getAllAddressTypes(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<CatalogEntityType> entityType, Optional<Boolean> active) {
        return findAllAddressTypeUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateAddressType(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableAddressTypeUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createAddressType(CreateAddressTypeRequest createAddressTypeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createAddressTypeUseCase.execute(createAddressTypeRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateAddressType(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableAddressTypeUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateAddressType(Long id, UpdateAddressTypeRequest updateAddressTypeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateAddressTypeUseCase.execute(id, updateAddressTypeRequest);
        return null;
    }
}
