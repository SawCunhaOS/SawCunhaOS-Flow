
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

import br.com.sawcunhaos.organization.api.controller.ContactTypeApiDelegate;
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateContactTypeRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllContactTypesResponse;
import br.com.sawcunhaos.organization.api.dto.GetContactTypeResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateContactTypeRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.CreateContactTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.DisableContactTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.EnableContactTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.FindAllContactTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.FindContactTypeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype.UpdateContactTypeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link ContactTypeApiDelegate} — expõe os 6 endpoints de {@code /v1/contact-types}
 * (UC-087 a UC-092), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContactTypeDelegate implements ContactTypeApiDelegate {

    private final FindContactTypeUseCase findContactTypeUseCase;
    private final FindAllContactTypeUseCase findAllContactTypeUseCase;
    private final DisableContactTypeUseCase disableContactTypeUseCase;
    private final EnableContactTypeUseCase enableContactTypeUseCase;
    private final CreateContactTypeUseCase createContactTypeUseCase;
    private final UpdateContactTypeUseCase updateContactTypeUseCase;

    @Override
    public GetContactTypeResponse getContactTypeById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetContactTypeResponse.builder()
                .data(findContactTypeUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllContactTypesResponse getAllContactTypes(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<CatalogEntityType> entityType, Optional<Boolean> active) {
        return findAllContactTypeUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateContactType(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableContactTypeUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createContactType(CreateContactTypeRequest createContactTypeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createContactTypeUseCase.execute(createContactTypeRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateContactType(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableContactTypeUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateContactType(Long id, UpdateContactTypeRequest updateContactTypeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateContactTypeUseCase.execute(id, updateContactTypeRequest);
        return null;
    }
}
