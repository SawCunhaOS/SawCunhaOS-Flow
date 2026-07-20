
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
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;

/**
 * Conversão entre o dto de domínio {@link AddressTypeOutput} e os dtos gerados do contrato OpenAPI
 * de {@code AddressType}, incluindo o enum {@code entityType} (domínio tem 3 valores, contrato só 2).
 */
final class AddressTypeApiMapper {

    private AddressTypeApiMapper() {
    }

    /** Monta o {@code AddressType} do contrato a partir da saída do domínio. */
    static AddressType toApiAddressType(AddressTypeOutput addressTypeOutput) {
        return AddressType.builder()
                .id(addressTypeOutput.id())
                .code(addressTypeOutput.code())
                .description(addressTypeOutput.description())
                .entityType(toApiEntityType(addressTypeOutput.entityType()))
                .active(addressTypeOutput.active())
                .build();
    }

    /** {@link EntityType} de domínio → {@link CatalogEntityType} do contrato. */
    static CatalogEntityType toApiEntityType(EntityType entityType) {
        if (entityType == null) {
            return null;
        }
        return CatalogEntityType.valueOf(entityType.name());
    }

    /** {@link CatalogEntityType} do contrato → {@link EntityType} de domínio. */
    static EntityType toDomainEntityType(CatalogEntityType catalogEntityType) {
        if (catalogEntityType == null) {
            return null;
        }
        return EntityType.valueOf(catalogEntityType.name());
    }
}
