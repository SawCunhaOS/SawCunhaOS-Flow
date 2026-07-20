
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

package br.com.sawcunhaos.organization.api.delegate.company;

import br.com.sawcunhaos.organization.api.controller.LegalNatureApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateLegalNatureRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllLegalNaturesResponse;
import br.com.sawcunhaos.organization.api.dto.GetLegalNatureResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateLegalNatureRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature.CreateLegalNatureUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature.DeleteLegalNatureUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature.FindAllLegalNatureUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature.FindLegalNatureUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature.UpdateLegalNatureUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link LegalNatureApiDelegate} — expõe os 5 endpoints de {@code /v1/legal-natures}
 * (UC-093 a UC-097), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegalNatureDelegate implements LegalNatureApiDelegate {

    private final FindLegalNatureUseCase findLegalNatureUseCase;
    private final FindAllLegalNatureUseCase findAllLegalNatureUseCase;
    private final CreateLegalNatureUseCase createLegalNatureUseCase;
    private final UpdateLegalNatureUseCase updateLegalNatureUseCase;
    private final DeleteLegalNatureUseCase deleteLegalNatureUseCase;

    @Override
    public GetLegalNatureResponse getLegalNatureById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetLegalNatureResponse.builder()
                .data(findLegalNatureUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllLegalNaturesResponse getAllLegalNatures(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return findAllLegalNatureUseCase.execute(paginationFilter);
    }

    @Override
    public CreateResponse createLegalNature(CreateLegalNatureRequest createLegalNatureRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createLegalNatureUseCase.execute(createLegalNatureRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void updateLegalNature(Long id, UpdateLegalNatureRequest updateLegalNatureRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateLegalNatureUseCase.execute(id, updateLegalNatureRequest);
        return null;
    }

    @Override
    public Void deleteLegalNature(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        deleteLegalNatureUseCase.execute(id);
        return null;
    }
}
