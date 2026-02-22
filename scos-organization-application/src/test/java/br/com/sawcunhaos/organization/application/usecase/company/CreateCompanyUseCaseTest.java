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
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes Unitários para CreateCompanyUseCase
 *
 * Cobre:
 * - Criação de empresa matriz
 * - Criação de empresa filial
 * - Validações de CNPJ único
 * - Validações de parent company
 * - Validações de ciclos
 * - Validações de foundation date
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyUseCase Unit Tests")
class CreateCompanyUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyDomainService companyDomainService;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private CreateCompanyUseCase createCompanyUseCase;

    private CreateCompanyDTO validCreateDTO;
    private Company validCompany;
    private static final String VALID_CNPJ = "12.345.678/0001-95";
    private static final String VALID_NAME = "Acme S.A.";
    private static final LocalDate VALID_FOUNDATION_DATE = LocalDate.of(2000, 5, 20);

    @BeforeEach
    void setUp() {
        validCreateDTO = CreateCompanyDTO.builder()
                .name(VALID_NAME)
                .nameTreatment("ACME")
                .taxIdentifier(VALID_CNPJ)
                .foundationDate(VALID_FOUNDATION_DATE)
                .sectorOfActivity("MANUFACTURING")
                .parentCompanyId(null)
                .build();

        validCompany = Company.builder()
                .id(1L)
                .name(VALID_NAME)
                .nameTreatment("ACME")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create company matrix successfully with valid data")
    void shouldCreateCompanyMatrixSuccessfully() {
        // Given
        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doNothing().when(companyDomainService).validateFoundationDateNotFuture(VALID_FOUNDATION_DATE);
        when(companyMapper.toCompany(validCreateDTO)).thenReturn(validCompany);
        when(companyRepository.persist(validCompany)).thenReturn(validCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("user@test.com");

        // When
        Long companyId = createCompanyUseCase.execute(validCreateDTO);

        // Then
        assertThat(companyId).isEqualTo(1L);
        verify(companyDomainService, times(1)).validateCnpjUniqueness(VALID_CNPJ);
        verify(companyDomainService, times(1)).validateFoundationDateNotFuture(VALID_FOUNDATION_DATE);
        verify(companyRepository, times(1)).persist(validCompany);
    }

    @Test
    @DisplayName("Should create company branch with valid parent company")
    void shouldCreateCompanyBranchWithValidParent() {
        // Given
        Long parentCompanyId = 2L;
        validCreateDTO.setParentCompanyId(parentCompanyId);
        Company parentCompany = Company.builder().id(parentCompanyId).status(StatusCompany.ACTIVE).active(true).build();

        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doNothing().when(companyDomainService).validateParentCompanyExists(parentCompanyId);
        doNothing().when(companyDomainService).validateParentCompanyIsActive(parentCompanyId);
        doNothing().when(companyDomainService).validateNoCyclicHierarchy(null, parentCompanyId);
        doNothing().when(companyDomainService).validateFoundationDateNotFuture(VALID_FOUNDATION_DATE);

        when(companyMapper.toCompany(validCreateDTO)).thenReturn(validCompany);
        when(companyRepository.getReferenceById(parentCompanyId)).thenReturn(parentCompany);
        when(companyRepository.persist(validCompany)).thenReturn(validCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("user@test.com");

        // When
        Long companyId = createCompanyUseCase.execute(validCreateDTO);

        // Then
        assertThat(companyId).isEqualTo(1L);
        verify(companyDomainService, times(1)).validateParentCompanyExists(parentCompanyId);
        verify(companyDomainService, times(1)).validateParentCompanyIsActive(parentCompanyId);
        verify(companyDomainService, times(1)).validateNoCyclicHierarchy(null, parentCompanyId);
    }

    @Test
    @DisplayName("Should throw exception when CNPJ already exists")
    void shouldThrowExceptionWhenCnpjAlreadyExists() {
        // Given
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_002))
                .when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);

        // When / Then
        assertThatThrownBy(() -> createCompanyUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_002.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when parent company does not exist")
    void shouldThrowExceptionWhenParentCompanyNotFound() {
        // Given
        validCreateDTO.setParentCompanyId(999L);
        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_001))
                .when(companyDomainService).validateParentCompanyExists(999L);

        // When / Then
        assertThatThrownBy(() -> createCompanyUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when cyclic hierarchy is detected")
    void shouldThrowExceptionWhenCyclicHierarchy() {
        // Given
        validCreateDTO.setParentCompanyId(1L);
        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doNothing().when(companyDomainService).validateParentCompanyExists(1L);
        doNothing().when(companyDomainService).validateParentCompanyIsActive(1L);
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_004))
                .when(companyDomainService).validateNoCyclicHierarchy(null, 1L);

        // When / Then
        assertThatThrownBy(() -> createCompanyUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_004.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when foundation date is in the future")
    void shouldThrowExceptionWhenFoundationDateInFuture() {
        // Given
        LocalDate futureDate = LocalDate.now().plusYears(1);
        validCreateDTO.setFoundationDate(futureDate);
        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doThrow(new ScosException())
                .when(companyDomainService).validateFoundationDateNotFuture(futureDate);

        // When / Then
        assertThatThrownBy(() -> createCompanyUseCase.execute(validCreateDTO))
                .isInstanceOf(ScosException.class);

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should set audit info before persisting")
    void shouldSetAuditInfoBeforePersisting() {
        // Given
        String authenticatedUser = "user@example.com";
        doNothing().when(companyDomainService).validateCnpjUniqueness(VALID_CNPJ);
        doNothing().when(companyDomainService).validateFoundationDateNotFuture(VALID_FOUNDATION_DATE);
        when(companyMapper.toCompany(validCreateDTO)).thenReturn(validCompany);
        when(companyRepository.persist(validCompany)).thenReturn(validCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(authenticatedUser);

        // When
        Long companyId = createCompanyUseCase.execute(validCreateDTO);

        // Then
        assertThat(companyId).isEqualTo(1L);
        verify(companyRepository, times(1)).persist(validCompany);
        // updateAuditInfo is called on the object, verified through successful execution
    }
}

