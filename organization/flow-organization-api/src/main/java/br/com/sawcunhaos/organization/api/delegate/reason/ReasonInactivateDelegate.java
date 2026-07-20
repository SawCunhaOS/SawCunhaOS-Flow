
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

package br.com.sawcunhaos.organization.api.delegate.reason;

import br.com.sawcunhaos.organization.api.controller.ReasonInactivateApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateReasonInactivateRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllReasonInactivateResponse;
import br.com.sawcunhaos.organization.api.dto.GetReasonInactivateResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonInactivateRequest;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.CreateReasonInactivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.DisableReasonInactivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.EnableReasonInactivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.FindAllReasonInactivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.FindReasonInactivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate.UpdateReasonInactivateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link ReasonInactivateApiDelegate} — expõe os 6 endpoints de {@code /v1/reason-inactivate}
 * (UC-119 a UC-124), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReasonInactivateDelegate implements ReasonInactivateApiDelegate {

    private final FindReasonInactivateUseCase findReasonInactivateUseCase;
    private final FindAllReasonInactivateUseCase findAllReasonInactivateUseCase;
    private final DisableReasonInactivateUseCase disableReasonInactivateUseCase;
    private final EnableReasonInactivateUseCase enableReasonInactivateUseCase;
    private final CreateReasonInactivateUseCase createReasonInactivateUseCase;
    private final UpdateReasonInactivateUseCase updateReasonInactivateUseCase;

    @Override
    public GetReasonInactivateResponse getReasonInactivateById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetReasonInactivateResponse.builder()
                .data(findReasonInactivateUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllReasonInactivateResponse getAllReasonInactivate(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<ReasonEntityType> entityType, Optional<Boolean> active) {
        return findAllReasonInactivateUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateReasonInactivate(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableReasonInactivateUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createReasonInactivate(CreateReasonInactivateRequest createReasonInactivateRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createReasonInactivateUseCase.execute(createReasonInactivateRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateReasonInactivate(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableReasonInactivateUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateReasonInactivate(Long id, UpdateReasonInactivateRequest updateReasonInactivateRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateReasonInactivateUseCase.execute(id, updateReasonInactivateRequest);
        return null;
    }
}
