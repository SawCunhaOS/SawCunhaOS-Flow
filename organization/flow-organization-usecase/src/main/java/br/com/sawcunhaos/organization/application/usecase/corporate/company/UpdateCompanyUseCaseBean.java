
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
import br.com.sawcunhaos.organization.api.dto.UpdateCompanyRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateCompanyUseCase} — {@code parentCompanyId} não é editável. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateCompanyUseCaseBean implements UpdateCompanyUseCase {

    private final CompanyService companyService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateCompanyRequest updateCompanyRequest) {
        log.info("Update company: {}", id);

        CompanyInput companyInput = CompanyInput.builder()
                .id(id)
                .name(updateCompanyRequest.name())
                .nameTreatment(updateCompanyRequest.nameTreatment())
                .foundationDate(updateCompanyRequest.foundationDate())
                .sectorOfActivity(updateCompanyRequest.sectorOfActivity())
                .observation(updateCompanyRequest.observation())
                .legalNatureId(updateCompanyRequest.legalNatureId())
                .cnaePrincipalId(updateCompanyRequest.cnaePrincipalId())
                .stateRegistration(updateCompanyRequest.stateRegistration())
                .municipalRegistration(updateCompanyRequest.municipalRegistration())
                .build();

        companyService.update(companyInput);
    }
}
