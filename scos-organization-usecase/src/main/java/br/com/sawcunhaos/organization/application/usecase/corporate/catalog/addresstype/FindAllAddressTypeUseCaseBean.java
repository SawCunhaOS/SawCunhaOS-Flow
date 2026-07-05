
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

import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.GetAllAddressTypesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindAllAddressTypeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindAllAddressTypeUseCaseBean implements FindAllAddressTypeUseCase {

    private final AddressTypeService addressTypeService;

    @Override
    public GetAllAddressTypesResponse execute(@NonNull PaginationFilter paginationFilter,
                                              CatalogEntityType entityType,
                                              Boolean active
    ) {
        log.info("Find All AddressType, Page: {}, Size: {}, Direction: {}, EntityType: {}, Active: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction(),
                entityType,
                active
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<AddressTypeOutput> addressTypeOutput = addressTypeService.findAll(
                AddressTypeApiMapper.toDomainEntityType(entityType),
                active,
                pageable
        );

        return GetAllAddressTypesResponse.builder()
                .data(
                        addressTypeOutput.getContent().stream()
                                .map(AddressTypeApiMapper::toApiAddressType)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(addressTypeOutput)
                )
                .build();
    }
}
