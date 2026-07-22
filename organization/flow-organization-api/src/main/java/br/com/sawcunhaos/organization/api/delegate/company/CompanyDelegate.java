
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

import br.com.sawcunhaos.organization.api.controller.CompanyApiDelegate;
import br.com.sawcunhaos.organization.api.dto.CompanyStatusTransitionRequest;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllCompaniesResponse;
import br.com.sawcunhaos.organization.api.dto.GetCompanyResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import br.com.sawcunhaos.organization.api.dto.UpdateCompanyRequest;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.ActivateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.BlockCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.CreateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.FindAllCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.FindCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.InactivateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.UnblockCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.corporate.company.UpdateCompanyUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação de {@link CompanyApiDelegate} — expõe o CRUD cadastral de {@code /v1/companies}
 * (UC-001..005) e as 4 transições de status (UC-006: enable/disable/block/unblock), delegando a
 * validação de negócio aos respectivos Use Cases. Sub-recursos (ex.: status-history) permanecem
 * com o comportamento default.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyDelegate implements CompanyApiDelegate {

    private final FindCompanyUseCase findCompanyUseCase;
    private final FindAllCompanyUseCase findAllCompanyUseCase;
    private final CreateCompanyUseCase createCompanyUseCase;
    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final ActivateCompanyUseCase activateCompanyUseCase;
    private final InactivateCompanyUseCase inactivateCompanyUseCase;
    private final BlockCompanyUseCase blockCompanyUseCase;
    private final UnblockCompanyUseCase unblockCompanyUseCase;

    @Override
    public CreateResponse createCompany(CreateCompanyRequest createCompanyRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return CreateResponse.builder()
                .data(Create.builder()
                        .id(
                                createCompanyUseCase.execute(createCompanyRequest).id()
                        )
                        .build())
                .build();
    }

    @Override
    public GetAllCompaniesResponse getAllCompanies(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<StatusCompany> status, Optional<String> name) {
        return findAllCompanyUseCase.execute(paginationFilter, status.orElse(null), name.orElse(null));
    }

    @Override
    public GetCompanyResponse getCompanyById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetCompanyResponse.builder()
                .data(findCompanyUseCase.execute(id))
                .build();
    }

    @Override
    public Void updateCompany(Long id, UpdateCompanyRequest updateCompanyRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateCompanyUseCase.execute(id, updateCompanyRequest);
        return null;
    }

    @Override
    public Void activateCompany(Long id, CompanyStatusTransitionRequest companyStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        activateCompanyUseCase.execute(id, companyStatusTransitionRequest);
        return null;
    }

    @Override
    public Void inactivateCompany(Long id, CompanyStatusTransitionRequest companyStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        inactivateCompanyUseCase.execute(id, companyStatusTransitionRequest);
        return null;
    }

    @Override
    public Void blockCompany(Long id, CompanyStatusTransitionRequest companyStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        blockCompanyUseCase.execute(id, companyStatusTransitionRequest);
        return null;
    }

    @Override
    public Void unblockCompany(Long id, CompanyStatusTransitionRequest companyStatusTransitionRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        unblockCompanyUseCase.execute(id, companyStatusTransitionRequest);
        return null;
    }
}
