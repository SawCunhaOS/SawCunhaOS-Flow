
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

import br.com.sawcunhaos.organization.api.controller.ReasonActivateApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateReasonActivateRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllReasonActivateResponse;
import br.com.sawcunhaos.organization.api.dto.GetReasonActivateResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonActivateRequest;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.CreateReasonActivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.DisableReasonActivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.EnableReasonActivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.FindAllReasonActivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.FindReasonActivateUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate.UpdateReasonActivateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link ReasonActivateApiDelegate} — expõe os 6 endpoints de {@code /v1/reason-activate}
 * (UC-113 a UC-118), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReasonActivateDelegate implements ReasonActivateApiDelegate {

    private final FindReasonActivateUseCase findReasonActivateUseCase;
    private final FindAllReasonActivateUseCase findAllReasonActivateUseCase;
    private final DisableReasonActivateUseCase disableReasonActivateUseCase;
    private final EnableReasonActivateUseCase enableReasonActivateUseCase;
    private final CreateReasonActivateUseCase createReasonActivateUseCase;
    private final UpdateReasonActivateUseCase updateReasonActivateUseCase;

    @Override
    public GetReasonActivateResponse getReasonActivateById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetReasonActivateResponse.builder()
                .data(findReasonActivateUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllReasonActivateResponse getAllReasonActivate(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<ReasonEntityType> entityType, Optional<Boolean> active) {
        return findAllReasonActivateUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateReasonActivate(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableReasonActivateUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createReasonActivate(CreateReasonActivateRequest createReasonActivateRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createReasonActivateUseCase.execute(createReasonActivateRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateReasonActivate(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableReasonActivateUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateReasonActivate(Long id, UpdateReasonActivateRequest updateReasonActivateRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateReasonActivateUseCase.execute(id, updateReasonActivateRequest);
        return null;
    }
}
