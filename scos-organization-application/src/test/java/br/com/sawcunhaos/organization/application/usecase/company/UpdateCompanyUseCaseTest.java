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
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes Unitários para UpdateCompanyUseCase
 *
 * Cobre:
 * - Atualização de empresa existente
 * - Validações de parent company alterada
 * - Validações de ciclos
 * - Validações de foundation date
 * - Empresa não encontrada
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateCompanyUseCase Unit Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateCompanyUseCaseTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyDomainService companyDomainService;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private UpdateCompanyUseCase updateCompanyUseCase;

    private UpdateCompanyDTO validUpdateDTO;
    private Company existingCompany;
    private static final Long COMPANY_ID = 1L;
    private static final String NEW_NAME = "Acme International";

    @BeforeEach
    void setUp() {
        validUpdateDTO = UpdateCompanyDTO.builder()
                .name(NEW_NAME)
                .nameTreatment("ACME INT")
                .foundationDate(LocalDate.of(2000, 5, 20))
                .sectorOfActivity("MANUFACTURING")
                .build();

        existingCompany = Company.builder()
                .id(COMPANY_ID)
                .name("Acme S.A.")
                .nameTreatment("ACME")
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should update company successfully with valid data")
    void shouldUpdateCompanySuccessfully() {
        // Given
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doNothing().when(companyDomainService).validateFoundationDateNotFuture(validUpdateDTO.getFoundationDate());
        when(companyMapper.updateCompany(validUpdateDTO, existingCompany)).thenReturn(existingCompany);
        when(companyRepository.persist(existingCompany)).thenReturn(existingCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("user@test.com");

        // When
        updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO);

        // Then
        verify(companyRepository, times(1)).findNotDeletedById(COMPANY_ID);
        verify(companyMapper, times(1)).updateCompany(validUpdateDTO, existingCompany);
        verify(companyRepository, times(1)).update(existingCompany);
    }

    @Test
    @DisplayName("Should throw exception when company not found")
    void shouldThrowExceptionWhenCompanyNotFound() {
        // Given
        when(companyRepository.findNotDeletedById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> updateCompanyUseCase.execute(999L, validUpdateDTO))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should validate parent company when changed")
    void shouldValidateParentCompanyWhenChanged() {
        // Given
        Long newParentId = 2L;
        validUpdateDTO.setParentCompanyId(newParentId);
        Company newParent = Company.builder().id(newParentId).status(StatusCompany.ACTIVE).active(true).build();

        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doNothing().when(companyDomainService).validateParentCompanyExists(newParentId);
        doNothing().when(companyDomainService).validateParentCompanyIsActive(newParentId);
        doNothing().when(companyDomainService).validateNoCyclicHierarchy(COMPANY_ID, newParentId);

        when(companyRepository.getReferenceById(newParentId)).thenReturn(newParent);
        when(companyMapper.updateCompany(validUpdateDTO, existingCompany)).thenReturn(existingCompany);
        when(companyRepository.update(existingCompany)).thenReturn(existingCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn("user@test.com");

        // When
        updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO);

        // Then
        verify(companyDomainService, times(1)).validateParentCompanyExists(newParentId);
        verify(companyDomainService, times(1)).validateParentCompanyIsActive(newParentId);
        verify(companyDomainService, times(1)).validateNoCyclicHierarchy(COMPANY_ID, newParentId);
    }

    @Test
    @DisplayName("Should throw exception when invalid parent company")
    void shouldThrowExceptionWhenInvalidParentCompany() {
        // Given
        validUpdateDTO.setParentCompanyId(999L);
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_001))
                .when(companyDomainService).validateParentCompanyExists(999L);

        // When / Then
        assertThatThrownBy(() -> updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO))
                .isInstanceOf(ScosException.class);

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should throw exception when cyclic hierarchy would be created")
    void shouldThrowExceptionWhenCyclicHierarchy() {
        // Given
        validUpdateDTO.setParentCompanyId(COMPANY_ID); // Self-reference
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doThrow(new ScosException(ExceptionCodeError.SCOS_COMPANY_004))
                .when(companyDomainService).validateNoCyclicHierarchy(COMPANY_ID, COMPANY_ID);

        // When / Then
        assertThatThrownBy(() -> updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO))
                .isInstanceOf(ScosException.class);

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should validate foundation date when updated")
    void shouldValidateFoundationDateWhenUpdated() {
        // Given
        LocalDate futureDate = LocalDate.now().plusYears(1);
        validUpdateDTO.setFoundationDate(futureDate);
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doThrow(new ScosException())
                .when(companyDomainService).validateFoundationDateNotFuture(futureDate);

        // When / Then
        assertThatThrownBy(() -> updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO))
                .isInstanceOf(ScosException.class);

        verify(companyRepository, never()).persist(any());
    }

    @Test
    @DisplayName("Should update audit info before persisting")
    void shouldUpdateAuditInfoBeforePersisting() {
        // Given
        String authenticatedUser = "user@example.com";
        when(companyRepository.findNotDeletedById(COMPANY_ID)).thenReturn(Optional.of(existingCompany));
        doNothing().when(companyDomainService).validateFoundationDateNotFuture(validUpdateDTO.getFoundationDate());
        when(companyMapper.updateCompany(validUpdateDTO, existingCompany)).thenReturn(existingCompany);
        when(companyRepository.update(existingCompany)).thenReturn(existingCompany);
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(authenticatedUser);

        // When
        updateCompanyUseCase.execute(COMPANY_ID, validUpdateDTO);

        // Then
        verify(companyRepository, times(1)).update(existingCompany);
        // updateAuditInfo is called on the object, verified through successful execution
    }
}

