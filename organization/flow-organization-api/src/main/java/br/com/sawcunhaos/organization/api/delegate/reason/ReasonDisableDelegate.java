
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

import br.com.sawcunhaos.organization.api.controller.ReasonDisableApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateReasonDisableRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllReasonDisableResponse;
import br.com.sawcunhaos.organization.api.dto.GetReasonDisableResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonDisableRequest;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.CreateReasonDisableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.DisableReasonDisableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.EnableReasonDisableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.FindAllReasonDisableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.FindReasonDisableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable.UpdateReasonDisableUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link ReasonDisableApiDelegate} — expõe os 6 endpoints de {@code /v1/reason-disable}
 * (UC-125 a UC-130), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReasonDisableDelegate implements ReasonDisableApiDelegate {

    private final FindReasonDisableUseCase findReasonDisableUseCase;
    private final FindAllReasonDisableUseCase findAllReasonDisableUseCase;
    private final DisableReasonDisableUseCase disableReasonDisableUseCase;
    private final EnableReasonDisableUseCase enableReasonDisableUseCase;
    private final CreateReasonDisableUseCase createReasonDisableUseCase;
    private final UpdateReasonDisableUseCase updateReasonDisableUseCase;

    @Override
    public GetReasonDisableResponse getReasonDisableById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetReasonDisableResponse.builder()
                .data(findReasonDisableUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllReasonDisableResponse getAllReasonDisable(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<ReasonEntityType> entityType, Optional<Boolean> active) {
        return findAllReasonDisableUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateReasonDisable(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableReasonDisableUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createReasonDisable(CreateReasonDisableRequest createReasonDisableRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createReasonDisableUseCase.execute(createReasonDisableRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateReasonDisable(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableReasonDisableUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateReasonDisable(Long id, UpdateReasonDisableRequest updateReasonDisableRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateReasonDisableUseCase.execute(id, updateReasonDisableRequest);
        return null;
    }
}
