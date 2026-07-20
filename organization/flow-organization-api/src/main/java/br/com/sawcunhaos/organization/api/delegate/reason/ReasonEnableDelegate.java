
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

import br.com.sawcunhaos.organization.api.controller.ReasonEnableApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateReasonEnableRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllReasonEnableResponse;
import br.com.sawcunhaos.organization.api.dto.GetReasonEnableResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonEnableRequest;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.CreateReasonEnableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.DisableReasonEnableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.EnableReasonEnableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.FindAllReasonEnableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.FindReasonEnableUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable.UpdateReasonEnableUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link ReasonEnableApiDelegate} — expõe os 6 endpoints de {@code /v1/reason-enable}
 * (UC-131 a UC-136), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReasonEnableDelegate implements ReasonEnableApiDelegate {

    private final FindReasonEnableUseCase findReasonEnableUseCase;
    private final FindAllReasonEnableUseCase findAllReasonEnableUseCase;
    private final DisableReasonEnableUseCase disableReasonEnableUseCase;
    private final EnableReasonEnableUseCase enableReasonEnableUseCase;
    private final CreateReasonEnableUseCase createReasonEnableUseCase;
    private final UpdateReasonEnableUseCase updateReasonEnableUseCase;

    @Override
    public GetReasonEnableResponse getReasonEnableById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetReasonEnableResponse.builder()
                .data(findReasonEnableUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllReasonEnableResponse getAllReasonEnable(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<ReasonEntityType> entityType, Optional<Boolean> active) {
        return findAllReasonEnableUseCase.execute(paginationFilter, entityType.orElse(null), active.orElse(null));
    }

    @Override
    public Void activateReasonEnable(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        enableReasonEnableUseCase.execute(id);
        return null;
    }

    @Override
    public CreateResponse createReasonEnable(CreateReasonEnableRequest createReasonEnableRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createReasonEnableUseCase.execute(createReasonEnableRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void inactivateReasonEnable(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        disableReasonEnableUseCase.execute(id);
        return null;
    }

    @Override
    public Void updateReasonEnable(Long id, UpdateReasonEnableRequest updateReasonEnableRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateReasonEnableUseCase.execute(id, updateReasonEnableRequest);
        return null;
    }
}
