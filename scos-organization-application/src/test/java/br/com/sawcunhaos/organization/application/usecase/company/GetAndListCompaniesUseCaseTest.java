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

package br.com.sawcunhaos.organization.application.usecase.company;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes Unitários para Read Operations
 * - GetCompanyByIdUseCase
 * - ListCompaniesUseCase
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetCompanyByIdUseCase Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class GetCompanyByIdUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private GetCompanyByIdUseCase getCompanyByIdUseCase;

    private Company existingCompany;
    private CompanyDTO expectedDTO;
    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        existingCompany = Company.builder()
                .id(COMPANY_ID)
                .name("Acme S.A.")
                .nameTreatment("ACME")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .foundationDate(LocalDate.of(2000, 5, 20))
                .build();

        expectedDTO = CompanyDTO.builder()
                .id(COMPANY_ID)
                .name("Acme S.A.")
                .nameTreatment("ACME")
                .status("ACTIVE")
                .active(true)
                .foundationDate(LocalDate.of(2000, 5, 20))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should get company by id successfully")
    void shouldGetCompanyByIdSuccessfully() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        when(companyMapper.toCompanyDTO(existingCompany)).thenReturn(expectedDTO);

        // When
        CompanyDTO result = getCompanyByIdUseCase.execute(COMPANY_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(COMPANY_ID);
        assertThat(result.getName()).isEqualTo("Acme S.A.");
        verify(companyRepository, times(1)).findNotDeletedById(COMPANY_ID);
        verify(companyMapper, times(1)).toCompanyDTO(existingCompany);
    }

    @Test
    @DisplayName("Should throw exception when company not found")
    void shouldThrowExceptionWhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> getCompanyByIdUseCase.execute(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompaniesUseCase Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class ListCompaniesUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private ListCompaniesUseCase listCompaniesUseCase;

    private Company company1;
    private Company company2;
    private CompanyDTO dto1;
    private CompanyDTO dto2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);

        company1 = Company.builder()
                .id(1L)
                .name("Acme S.A.")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();

        company2 = Company.builder()
                .id(2L)
                .name("Beta Corp")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();

        dto1 = CompanyDTO.builder()
                .id(1L)
                .name("Acme S.A.")
                .status("ACTIVE")
                .build();

        dto2 = CompanyDTO.builder()
                .id(2L)
                .name("Beta Corp")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Should list all companies with pagination")
    void shouldListAllCompaniesWithPagination() {
        // Given
        Page<Company> companiesPage = new PageImpl<>(List.of(company1, company2), pageable, 2);
        when(companyRepository.findAllNotDeleted(pageable)).thenReturn(companiesPage);
        when(companyMapper.toCompanyDTO(company1)).thenReturn(dto1);
        when(companyMapper.toCompanyDTO(company2)).thenReturn(dto2);

        // When
        Page<CompanyDTO> result = listCompaniesUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Acme S.A.");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Beta Corp");
        verify(companyRepository, times(1)).findAllNotDeleted(pageable);
    }

    @Test
    @DisplayName("Should return empty page when no companies found")
    void shouldReturnEmptyPageWhenNoCompaniesFound() {
        // Given
        Page<Company> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(companyRepository.findAllNotDeleted(pageable)).thenReturn(emptyPage);

        // When
        Page<CompanyDTO> result = listCompaniesUseCase.execute(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should exclude deleted companies from list")
    void shouldExcludeDeletedCompaniesFromList() {
        // Given
        Company activeCompany = Company.builder()
                .id(1L)
                .name("Active Company")
                .status(StatusCompany.ACTIVE)
                .build();

        Page<Company> page = new PageImpl<>(List.of(activeCompany), pageable, 1);
        when(companyRepository.findAllNotDeleted(pageable)).thenReturn(page);
        when(companyMapper.toCompanyDTO(activeCompany)).thenReturn(
                CompanyDTO.builder().id(1L).name("Active Company").build()
        );

        // When
        Page<CompanyDTO> result = listCompaniesUseCase.execute(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Active Company");
    }
}

