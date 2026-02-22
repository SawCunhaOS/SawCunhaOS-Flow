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

package br.com.sawcunhaos.organization.application.mapper.company;

import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes Unitários para CompanyMapper
 *
 * Cobre:
 * - Conversão CreateCompanyDTO → Company
 * - Conversão UpdateCompanyDTO + Company → Company
 * - Conversão Company → CompanyDTO
 */
@DisplayName("CompanyMapper Unit Tests")
class CompanyMapperTest {

    private CompanyMapper companyMapper;
    private static final String VALID_CNPJ = "11222333000181"; // CNPJ válido gerado pelo algoritmo
    private static final String VALID_NAME = "Acme S.A.";
    private static final String VALID_NAME_TREATMENT = "ACME";
    private static final LocalDate VALID_FOUNDATION_DATE = LocalDate.of(2000, 5, 20);

    @BeforeEach
    void setUp() {
        companyMapper = new CompanyMapper();
    }

    @Test
    @DisplayName("Should map CreateCompanyDTO to Company entity")
    void shouldMapCreateDTOToCompany() {
        // Given
        CreateCompanyDTO createDTO = CreateCompanyDTO.builder()
                .name(VALID_NAME)
                .nameTreatment(VALID_NAME_TREATMENT)
                .taxIdentifier(VALID_CNPJ)
                .foundationDate(VALID_FOUNDATION_DATE)
                .sectorOfActivity("MANUFACTURING")
                .observation("Test observation")
                .build();

        // When
        Company company = companyMapper.toCompany(createDTO);

        // Then
        assertThat(company).isNotNull();
        assertThat(company.getName()).isEqualTo(VALID_NAME);
        assertThat(company.getNameTreatment()).isEqualTo(VALID_NAME_TREATMENT);
        assertThat(company.getFoundationDate()).isEqualTo(VALID_FOUNDATION_DATE);
        assertThat(company.getSectorOfActivity()).isEqualTo("MANUFACTURING");
        assertThat(company.getObservation()).isEqualTo("Test observation");
        assertThat(company.getStatus()).isEqualTo(StatusCompany.ACTIVE);
        assertThat(company.isActive()).isTrue();
        assertThat(company.getParentCompany()).isNull();
    }

    @Test
    @DisplayName("Should map UpdateCompanyDTO to existing Company entity")
    void shouldMapUpdateDTOToCompany() {
        // Given
        Company existingCompany = Company.builder()
                .id(1L)
                .name("Old Name")
                .nameTreatment("OLD")
                .status(StatusCompany.ACTIVE)
                .build();

        UpdateCompanyDTO updateDTO = UpdateCompanyDTO.builder()
                .name("New Name")
                .nameTreatment("NEW")
                .foundationDate(VALID_FOUNDATION_DATE)
                .sectorOfActivity("RETAIL")
                .observation("Updated observation")
                .status(StatusCompany.INACTIVE)
                .build();

        // When
        Company updatedCompany = companyMapper.updateCompany(updateDTO, existingCompany);

        // Then
        assertThat(updatedCompany).isNotNull();
        assertThat(updatedCompany.getId()).isEqualTo(1L); // ID remains unchanged
        assertThat(updatedCompany.getName()).isEqualTo("New Name");
        assertThat(updatedCompany.getNameTreatment()).isEqualTo("NEW");
        assertThat(updatedCompany.getFoundationDate()).isEqualTo(VALID_FOUNDATION_DATE);
        assertThat(updatedCompany.getSectorOfActivity()).isEqualTo("RETAIL");
        assertThat(updatedCompany.getObservation()).isEqualTo("Updated observation");
        assertThat(updatedCompany.getStatus()).isEqualTo(StatusCompany.INACTIVE);
    }

    @Test
    @DisplayName("Should partially update Company with null fields")
    void shouldPartiallyUpdateCompanyWithNullFields() {
        // Given
        Company existingCompany = Company.builder()
                .id(1L)
                .name("Original Name")
                .nameTreatment("ORIG")
                .foundationDate(LocalDate.of(2020, 1, 1))
                .sectorOfActivity("MANUFACTURING")
                .build();

        UpdateCompanyDTO partialUpdate = UpdateCompanyDTO.builder()
                .name("Updated Name")
                // Other fields are null
                .build();

        // When
        Company updated = companyMapper.updateCompany(partialUpdate, existingCompany);

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getNameTreatment()).isEqualTo("ORIG"); // Unchanged
        assertThat(updated.getFoundationDate()).isEqualTo(LocalDate.of(2020, 1, 1)); // Unchanged
        assertThat(updated.getSectorOfActivity()).isEqualTo("MANUFACTURING"); // Unchanged
    }

    @Test
    @DisplayName("Should map Company entity to CompanyDTO")
    void shouldMapCompanyToDTO() {
        // Given
        Company company = Company.builder()
                .id(1L)
                .name(VALID_NAME)
                .nameTreatment(VALID_NAME_TREATMENT)
                .taxIdentifier(new Cnpj(VALID_CNPJ))
                .foundationDate(VALID_FOUNDATION_DATE)
                .sectorOfActivity("MANUFACTURING")
                .observation("Test observation")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();

        // When
        CompanyDTO dto = companyMapper.toCompanyDTO(company);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo(VALID_NAME);
        assertThat(dto.getNameTreatment()).isEqualTo(VALID_NAME_TREATMENT);
        assertThat(dto.getTaxIdentifier()).isEqualTo(VALID_CNPJ);
        assertThat(dto.getFoundationDate()).isEqualTo(VALID_FOUNDATION_DATE);
        assertThat(dto.getSectorOfActivity()).isEqualTo("MANUFACTURING");
        assertThat(dto.getObservation()).isEqualTo("Test observation");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should map Company with parent company to DTO")
    void shouldMapCompanyWithParentToDTO() {
        // Given
        Company parentCompany = Company.builder()
                .id(2L)
                .name("Parent Company")
                .build();

        Company company = Company.builder()
                .id(1L)
                .name("Child Company")
                .parentCompany(parentCompany)
                .status(StatusCompany.ACTIVE)
                .build();

        // When
        CompanyDTO dto = companyMapper.toCompanyDTO(company);

        // Then
        assertThat(dto.getParentCompanyId()).isEqualTo(2L);
        assertThat(dto.getParentCompanyName()).isEqualTo("Parent Company");
    }

    @Test
    @DisplayName("Should map Company without parent company to DTO")
    void shouldMapCompanyWithoutParentToDTO() {
        // Given
        Company company = Company.builder()
                .id(1L)
                .name("Matrix Company")
                .parentCompany(null)
                .status(StatusCompany.ACTIVE)
                .build();

        // When
        CompanyDTO dto = companyMapper.toCompanyDTO(company);

        // Then
        assertThat(dto.getParentCompanyId()).isNull();
        assertThat(dto.getParentCompanyName()).isNull();
    }

    @Test
    @DisplayName("Should handle null DTO values gracefully")
    void shouldHandleNullDTOValuesGracefully() {
        // Given
        CreateCompanyDTO nullDTO = null;
        UpdateCompanyDTO nullUpdateDTO = null;
        Company nullCompany = null;

        // When / Then
        assertThat(companyMapper.toCompany(nullDTO)).isNull();
        assertThat(companyMapper.updateCompany(nullUpdateDTO, nullCompany)).isNull();
        assertThat(companyMapper.toCompanyDTO(nullCompany)).isNull();
    }

    @Test
    @DisplayName("Should preserve Company ID during mapping")
    void shouldPreserveCompanyIdDuringMapping() {
        // Given
        Company company = Company.builder()
                .id(123L)
                .name(VALID_NAME)
                .status(StatusCompany.ACTIVE)
                .build();

        // When
        CompanyDTO dto = companyMapper.toCompanyDTO(company);

        // Then
        assertThat(dto.getId()).isEqualTo(123L);
    }
}

