
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

import br.com.sawcunhaos.organization.api.dto.Companies;
import br.com.sawcunhaos.organization.api.dto.Company;
import br.com.sawcunhaos.organization.api.dto.ParentCompany;
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.ParentCompanyOutput;

/**
 * Conversão entre os dtos de domínio ({@link CompanyOutput}) e os dtos gerados do contrato OpenAPI
 * ({@link Company} completo e {@link Companies} resumido).
 */
final class CompanyApiMapper {

    private CompanyApiMapper() {
    }

    /** Monta o {@code Company} completo (UC-003) a partir da saída do domínio. */
    static Company toApiCompany(CompanyOutput companyOutput) {
        return Company.builder()
                .id(companyOutput.id())
                .name(companyOutput.name())
                .nameTreatment(companyOutput.nameTreatment())
                .taxIdentifier(companyOutput.taxIdentifier())
                .foundationDate(companyOutput.foundationDate())
                .sectorOfActivity(companyOutput.sectorOfActivity())
                .observation(companyOutput.observation())
                .parentCompany(toApiParentCompany(companyOutput.parentCompany()))
                .status(toApiStatus(companyOutput.status()))
                .legalNatureId(companyOutput.legalNatureId())
                .cnaePrincipalId(companyOutput.cnaePrincipalId())
                .stateRegistration(companyOutput.stateRegistration())
                .municipalRegistration(companyOutput.municipalRegistration())
                .build();
    }

    /** Monta o {@code Companies} resumido (UC-004) a partir da saída do domínio. */
    static Companies toApiCompanies(CompanyOutput companyOutput) {
        return Companies.builder()
                .id(companyOutput.id())
                .name(companyOutput.name())
                .nameTreatment(companyOutput.nameTreatment())
                .sectorOfActivity(companyOutput.sectorOfActivity())
                .observation(companyOutput.observation())
                .status(toApiStatus(companyOutput.status()))
                .build();
    }

    private static ParentCompany toApiParentCompany(ParentCompanyOutput parentCompanyOutput) {
        if (parentCompanyOutput == null) {
            return null;
        }
        return ParentCompany.builder()
                .id(parentCompanyOutput.id())
                .name(parentCompanyOutput.name())
                .nameTreatment(parentCompanyOutput.nameTreatment())
                .taxIdentifier(parentCompanyOutput.taxIdentifier())
                .build();
    }

    static StatusCompany toApiStatus(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany status) {
        return status == null ? null : StatusCompany.valueOf(status.name());
    }

    static br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany toDomainStatus(StatusCompany status) {
        return status == null ? null
                : br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.valueOf(status.name());
    }
}
