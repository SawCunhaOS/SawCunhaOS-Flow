
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
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.ParentCompanyOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Testes de {@link CompanyApiMapper}: mapeamento completo/resumo e conversão de status. */
class CompanyApiMapperTest {

    private CompanyOutput fullOutput() {
        return CompanyOutput.builder()
                .id(100L)
                .name("SawCunha Tecnologia LTDA")
                .nameTreatment("SawCunha")
                .taxIdentifier("11222333000181")
                .foundationDate(LocalDate.of(2020, 5, 10))
                .sectorOfActivity("Tecnologia da Informação")
                .observation("Matriz")
                .status(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.ACTIVE)
                .parentCompany(ParentCompanyOutput.builder().id(1L).name("Matriz").nameTreatment("M").taxIdentifier("52508598049801").build())
                .legalNatureId(3L)
                .cnaePrincipalId(42L)
                .stateRegistration("ISENTO")
                .municipalRegistration("123")
                .build();
    }

    @Test
    void toApiCompanyShouldMapAllFieldsIncludingParentAndStatus() {
        Company company = CompanyApiMapper.toApiCompany(fullOutput());

        assertThat(company.id()).isEqualTo(100L);
        assertThat(company.taxIdentifier()).isEqualTo("11222333000181");
        assertThat(company.sectorOfActivity()).isEqualTo("Tecnologia da Informação");
        assertThat(company.status()).isEqualTo(StatusCompany.ACTIVE);
        assertThat(company.legalNatureId()).isEqualTo(3L);
        assertThat(company.cnaePrincipalId()).isEqualTo(42L);
        assertThat(company.stateRegistration()).isEqualTo("ISENTO");
        assertThat(company.parentCompany()).isNotNull();
        assertThat(company.parentCompany().id()).isEqualTo(1L);
        assertThat(company.parentCompany().taxIdentifier()).isEqualTo("52508598049801");
    }

    @Test
    void toApiCompaniesShouldMapSummaryFieldsOnly() {
        Companies companies = CompanyApiMapper.toApiCompanies(fullOutput());

        assertThat(companies.id()).isEqualTo(100L);
        assertThat(companies.name()).isEqualTo("SawCunha Tecnologia LTDA");
        assertThat(companies.nameTreatment()).isEqualTo("SawCunha");
        assertThat(companies.sectorOfActivity()).isEqualTo("Tecnologia da Informação");
        assertThat(companies.status()).isEqualTo(StatusCompany.ACTIVE);
    }

    @Test
    void toApiParentCompanyShouldBeNullWhenMatrix() {
        CompanyOutput matrix = CompanyOutput.builder()
                .id(100L)
                .name("SawCunha Tecnologia LTDA")
                .status(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.ACTIVE)
                .parentCompany(null)
                .build();

        Company company = CompanyApiMapper.toApiCompany(matrix);
        assertThat(company.parentCompany()).isNull();
    }

    @Test
    void statusConversionShouldBeNullSafeAndSymmetric() {
        assertThat(CompanyApiMapper.toApiStatus(null)).isNull();
        assertThat(CompanyApiMapper.toDomainStatus(null)).isNull();
        assertThat(CompanyApiMapper.toDomainStatus(StatusCompany.DISABLED))
                .isEqualTo(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.DISABLED);
        assertThat(CompanyApiMapper.toApiStatus(br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany.INACTIVE))
                .isEqualTo(StatusCompany.INACTIVE);
    }
}
