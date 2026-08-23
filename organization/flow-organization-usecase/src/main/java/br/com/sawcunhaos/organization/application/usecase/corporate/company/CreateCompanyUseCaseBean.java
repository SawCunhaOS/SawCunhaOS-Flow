
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Company;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link CreateCompanyUseCase} — orquestra, não valida. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateCompanyUseCaseBean implements CreateCompanyUseCase {

    private final CompanyService companyService;

    @Override
    public Company execute(@NonNull CreateCompanyRequest createCompanyRequest) {
        log.info("Create company: {}", createCompanyRequest.taxIdentifier());

        CompanyInput companyInput = CompanyInput.builder()
                .name(createCompanyRequest.name())
                .nameTreatment(createCompanyRequest.nameTreatment())
                .taxIdentifier(createCompanyRequest.taxIdentifier())
                .foundationDate(createCompanyRequest.foundationDate())
                .sectorOfActivity(createCompanyRequest.sectorOfActivity())
                .observation(createCompanyRequest.observation())
                .parentCompanyId(createCompanyRequest.parentCompanyId())
                .reasonActivateId(createCompanyRequest.reasonActivateId())
                .legalNatureId(createCompanyRequest.legalNatureId())
                .cnaePrincipalId(createCompanyRequest.cnaePrincipalId())
                .stateRegistration(createCompanyRequest.stateRegistration())
                .municipalRegistration(createCompanyRequest.municipalRegistration())
                .build();

        return CompanyApiMapper.toApiCompany(companyService.create(companyInput));
    }
}
