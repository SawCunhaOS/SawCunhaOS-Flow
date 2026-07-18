
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

import br.com.sawcunhaos.organization.api.controller.CnaeApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateCnaeRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllCnaesResponse;
import br.com.sawcunhaos.organization.api.dto.GetCnaeResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateCnaeRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae.CreateCnaeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae.DeleteCnaeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae.FindAllCnaeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae.FindCnaeUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae.UpdateCnaeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link CnaeApiDelegate} — expõe os 5 endpoints de {@code /v1/cnaes}
 * (UC-098 a UC-102), delegando a validação de negócio aos respectivos Use Cases.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CnaeDelegate implements CnaeApiDelegate {

    private final FindCnaeUseCase findCnaeUseCase;
    private final FindAllCnaeUseCase findAllCnaeUseCase;
    private final CreateCnaeUseCase createCnaeUseCase;
    private final UpdateCnaeUseCase updateCnaeUseCase;
    private final DeleteCnaeUseCase deleteCnaeUseCase;

    @Override
    public GetCnaeResponse getCnaeById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetCnaeResponse.builder()
                .data(findCnaeUseCase.execute(id))
                .build();
    }

    @Override
    public GetAllCnaesResponse getAllCnaes(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return findAllCnaeUseCase.execute(paginationFilter);
    }

    @Override
    public CreateResponse createCnae(CreateCnaeRequest createCnaeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createCnaeUseCase.execute(createCnaeRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public Void updateCnae(Long id, UpdateCnaeRequest updateCnaeRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateCnaeUseCase.execute(id, updateCnaeRequest);
        return null;
    }

    @Override
    public Void deleteCnae(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        deleteCnaeUseCase.execute(id);
        return null;
    }
}
