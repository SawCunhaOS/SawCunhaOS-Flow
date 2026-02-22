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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

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
 * Testes Unitários para DeleteCompanyUseCase
 *
 * Cobre:
 * - Deleção de empresa (soft-delete)
 * - Validação de dependências
 * - Empresa não encontrada
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCompanyUseCase Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class DeleteCompanyUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyDomainService companyDomainService;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private DeleteCompanyUseCase deleteCompanyUseCase;

    private Company existingCompany;
    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        existingCompany = Company.builder()
                .id(COMPANY_ID)
                .name("Acme S.A.")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    @Test
    @DisplayName("Should delete company successfully with soft-delete")
    void shouldDeleteCompanySuccessfully() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doNothing().when(companyDomainService).validateCanDelete(COMPANY_ID);
        when(companyRepository.update(existingCompany)).thenReturn(existingCompany);

        // When
        deleteCompanyUseCase.execute(COMPANY_ID);

        // Then
        assertThat(existingCompany.getStatus()).isEqualTo(StatusCompany.DELETED);
        assertThat(existingCompany.isActive()).isFalse();
        verify(companyRepository, times(1)).findNotDeletedById(COMPANY_ID);
        verify(companyDomainService, times(1)).validateCanDelete(COMPANY_ID);
        verify(companyRepository, times(1)).update(existingCompany);
    }

    @Test
    @DisplayName("Should throw exception when company not found")
    void shouldThrowExceptionWhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> deleteCompanyUseCase.execute(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when company has dependencies")
    void shouldThrowExceptionWhenCompanyHasDependencies() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_003))
                .when(companyDomainService).validateCanDelete(COMPANY_ID);

        // When / Then
        assertThatThrownBy(() -> deleteCompanyUseCase.execute(COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_003.getCode());

        verify(companyRepository, never()).persist(any());
    }
}

/**
 * Testes Unitários para ChangeCompanyStatusUseCase
 *
 * Cobre:
 * - Ativação de empresa
 * - Inativação de empresa
 * - Validação de última matriz ativa
 * - Empresa não encontrada
 * - Transições de status válidas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeCompanyStatusUseCase Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class ChangeCompanyStatusUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyDomainService companyDomainService;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ChangeCompanyStatusUseCase changeCompanyStatusUseCase;

    private Company existingCompany;
    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        existingCompany = Company.builder()
                .id(COMPANY_ID)
                .name("Acme S.A.")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    @Test
    @DisplayName("Should inactivate company successfully")
    void shouldInactivateCompanySuccessfully() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doNothing().when(companyDomainService).validateCanInactivate(COMPANY_ID);
        when(companyRepository.persist(existingCompany)).thenReturn(existingCompany);

        // When
        changeCompanyStatusUseCase.execute(COMPANY_ID, "INACTIVE");

        // Then
        assertThat(existingCompany.getStatus()).isEqualTo(StatusCompany.INACTIVE);
        assertThat(existingCompany.isActive()).isFalse();
        verify(companyDomainService, times(1)).validateCanInactivate(COMPANY_ID);
        verify(companyRepository, times(1)).update(existingCompany);
    }

    @Test
    @DisplayName("Should activate company successfully")
    void shouldActivateCompanySuccessfully() {
        // Given
        existingCompany.setStatus(StatusCompany.INACTIVE);
        existingCompany.setActive(false);

        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        when(companyRepository.persist(existingCompany)).thenReturn(existingCompany);

        // When
        changeCompanyStatusUseCase.execute(COMPANY_ID, "ACTIVE");

        // Then
        assertThat(existingCompany.getStatus()).isEqualTo(StatusCompany.ACTIVE);
        assertThat(existingCompany.isActive()).isTrue();
        verify(companyRepository, times(1)).update(existingCompany);
    }

    @Test
    @DisplayName("Should throw exception when company not found")
    void shouldThrowExceptionWhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> changeCompanyStatusUseCase.execute(999L, "INACTIVE"))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when inactivating last active matrix")
    void shouldThrowExceptionWhenInactivatingLastActiveMatrix() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_005))
                .when(companyDomainService).validateCanInactivate(COMPANY_ID);

        // When / Then
        assertThatThrownBy(() -> changeCompanyStatusUseCase.execute(COMPANY_ID, "INACTIVE"))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_005.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should not validate when activating company")
    void shouldNotValidateWhenActivatingCompany() {
        // Given
        existingCompany.setStatus(StatusCompany.INACTIVE);
        existingCompany.setActive(false);

        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        when(companyRepository.persist(existingCompany)).thenReturn(existingCompany);

        // When
        changeCompanyStatusUseCase.execute(COMPANY_ID, "ACTIVE");

        // Then - validateCanInactivate should NOT be called when activating
        verify(companyDomainService, never()).validateCanInactivate(COMPANY_ID);
    }
}

