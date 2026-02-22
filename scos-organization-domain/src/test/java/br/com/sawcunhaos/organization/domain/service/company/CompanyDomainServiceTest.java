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

package br.com.sawcunhaos.organization.domain.service.company;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.repository.employee.EmployeeQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyDomainService Unit Tests")
class CompanyDomainServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;

    @InjectMocks
    private CompanyDomainService companyDomainService;

    private static final String VALID_CNPJ = "12.345.678/0001-95";
    private static final Long VALID_COMPANY_ID = 1L;
    private static final Long VALID_PARENT_COMPANY_ID = 2L;
    private static final LocalDate VALID_FOUNDATION_DATE = LocalDate.now().minusYears(10);

    @BeforeEach
    void setUp() {
        // Setup comum se necessário
    }

    // ===== CNPJ Validation Tests =====

    @Test
    @DisplayName("Should validate successfully when CNPJ does not exist")
    void shouldValidateSuccessfullyCnpjUniqueness() {
        // Given
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);

        // When / Then
        assertThatCode(() -> companyDomainService.validateCnpjUniqueness(VALID_CNPJ))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).existsByTaxIdentifier(VALID_CNPJ);
    }

    @Test
    @DisplayName("Should throw ScosException when CNPJ already exists")
    void shouldThrowExceptionWhenCnpjAlreadyExists() {
        // Given
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateCnpjUniqueness(VALID_CNPJ))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_002.getCode());

        verify(companyRepository, times(1)).existsByTaxIdentifier(VALID_CNPJ);
    }

    // ===== Parent Company Validation Tests =====

    @Test
    @DisplayName("Should validate successfully when parent company exists")
    void shouldValidateSuccessfullyParentCompanyExists() {
        // Given
        when(companyRepository.existsById(VALID_PARENT_COMPANY_ID)).thenReturn(true);

        // When / Then
        assertThatCode(() -> companyDomainService.validateParentCompanyExists(VALID_PARENT_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).existsById(VALID_PARENT_COMPANY_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when parent company does not exist")
    void shouldThrowExceptionWhenParentCompanyNotExists() {
        // Given
        when(companyRepository.existsById(VALID_PARENT_COMPANY_ID)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateParentCompanyExists(VALID_PARENT_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, times(1)).existsById(VALID_PARENT_COMPANY_ID);
    }

    @Test
    @DisplayName("Should validate successfully when parent company is active")
    void shouldValidateSuccessfullyParentCompanyIsActive() {
        // Given
        Company activeParent = Company.builder()
                .id(VALID_PARENT_COMPANY_ID)
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();

        when(companyRepository.findById(VALID_PARENT_COMPANY_ID)).thenReturn(Optional.of(activeParent));

        // When / Then
        assertThatCode(() -> companyDomainService.validateParentCompanyIsActive(VALID_PARENT_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).findById(VALID_PARENT_COMPANY_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when parent company is not active")
    void shouldThrowExceptionWhenParentCompanyNotActive() {
        // Given
        Company inactiveParent = Company.builder()
                .id(VALID_PARENT_COMPANY_ID)
                .status(StatusCompany.INACTIVE)
                .active(false)
                .build();

        when(companyRepository.findById(VALID_PARENT_COMPANY_ID)).thenReturn(Optional.of(inactiveParent));

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateParentCompanyIsActive(VALID_PARENT_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_001.getCode());

        verify(companyRepository, times(1)).findById(VALID_PARENT_COMPANY_ID);
    }

    // ===== Cyclic Hierarchy Validation Tests =====

    @Test
    @DisplayName("Should throw ScosException when company is its own parent")
    void shouldThrowExceptionWhenCompanyIsOwnParent() {
        // Given / When / Then
        assertThatThrownBy(() -> companyDomainService.validateNoCyclicHierarchy(VALID_COMPANY_ID, VALID_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_004.getCode());
    }

    @Test
    @DisplayName("Should validate successfully when no cyclic hierarchy exists")
    void shouldValidateSuccessfullyNoCyclicHierarchy() {
        // Given
        Company parentCompany = Company.builder()
                .id(VALID_PARENT_COMPANY_ID)
                .parentCompany(null)
                .build();

        when(companyRepository.findById(VALID_PARENT_COMPANY_ID)).thenReturn(Optional.of(parentCompany));

        // When / Then
        assertThatCode(() -> companyDomainService.validateNoCyclicHierarchy(VALID_COMPANY_ID, VALID_PARENT_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).findById(VALID_PARENT_COMPANY_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when cyclic hierarchy is detected")
    void shouldThrowExceptionWhenCyclicHierarchyDetected() {
        // Given - Company 1 -> Parent 2 -> Parent 3 (where 3 is the new company being assigned as parent of 1)
        Company grandparentCompany = Company.builder()
                .id(VALID_COMPANY_ID)
                .parentCompany(null)
                .build();

        Company parentCompany = Company.builder()
                .id(VALID_PARENT_COMPANY_ID)
                .parentCompany(grandparentCompany)
                .build();

        when(companyRepository.findById(VALID_PARENT_COMPANY_ID)).thenReturn(Optional.of(parentCompany));

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateNoCyclicHierarchy(VALID_COMPANY_ID, VALID_PARENT_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_004.getCode());

        verify(companyRepository, times(1)).findById(VALID_PARENT_COMPANY_ID);
    }

    // ===== Foundation Date Validation Tests =====

    @Test
    @DisplayName("Should validate successfully when foundation date is not in future")
    void shouldValidateSuccessfullyFoundationDate() {
        // Given / When / Then
        assertThatCode(() -> companyDomainService.validateFoundationDateNotFuture(VALID_FOUNDATION_DATE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw ScosException when foundation date is in future")
    void shouldThrowExceptionWhenFoundationDateInFuture() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(1);

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateFoundationDateNotFuture(futureDate))
                .isInstanceOf(ScosException.class);
    }

    // ===== Delete Validation Tests =====

    @Test
    @DisplayName("Should validate successfully when company can be deleted")
    void shouldValidateSuccessfullyCanDelete() {
        // Given
        when(companyRepository.existsByParentCompanyId(VALID_COMPANY_ID)).thenReturn(false);
        when(companyRepository.existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE)).thenReturn(true);

        // When / Then
        assertThatCode(() -> companyDomainService.validateCanDelete(VALID_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).existsByParentCompanyId(VALID_COMPANY_ID);
        verify(companyRepository, times(1)).existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE);
    }

    @Test
    @DisplayName("Should throw ScosException when company has branches")
    void shouldThrowExceptionWhenCompanyHasBranches() {
        // Given
        when(companyRepository.existsByParentCompanyId(VALID_COMPANY_ID)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateCanDelete(VALID_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_003.getCode());

        verify(companyRepository, times(1)).existsByParentCompanyId(VALID_COMPANY_ID);
    }

    // ===== Inactivate Validation Tests =====

    @Test
    @DisplayName("Should validate successfully when company can be inactivated")
    void shouldValidateSuccessfullyCanInactivate() {
        // Given - Branch company (has parent), so can be inactivated
        Company branchCompany = Company.builder()
                .id(VALID_COMPANY_ID)
                .parentCompany(Company.builder().id(VALID_PARENT_COMPANY_ID).build())
                .build();

        when(companyRepository.findById(VALID_COMPANY_ID)).thenReturn(Optional.of(branchCompany));

        // When / Then
        assertThatCode(() -> companyDomainService.validateCanInactivate(VALID_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).findById(VALID_COMPANY_ID);
    }

    @Test
    @DisplayName("Should throw ScosException when trying to inactivate last active matrix")
    void shouldThrowExceptionWhenInactivatingLastActiveMatrix() {
        // Given - Matrix company (no parent) and it's the last active one
        Company matrixCompany = Company.builder()
                .id(VALID_COMPANY_ID)
                .parentCompany(null)
                .build();

        when(companyRepository.findById(VALID_COMPANY_ID)).thenReturn(Optional.of(matrixCompany));
        when(companyRepository.existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> companyDomainService.validateCanInactivate(VALID_COMPANY_ID))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", ExceptionCodeError.SCOS_COMPANY_005.getCode());

        verify(companyRepository, times(1)).findById(VALID_COMPANY_ID);
        verify(companyRepository, times(1)).existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE);
    }

    @Test
    @DisplayName("Should validate successfully when inactivating matrix with other active matrix")
    void shouldValidateSuccessfullyInactivatingMatrixWithAlternative() {
        // Given - Matrix company (no parent) but there are other active matrices
        Company matrixCompany = Company.builder()
                .id(VALID_COMPANY_ID)
                .parentCompany(null)
                .build();

        when(companyRepository.findById(VALID_COMPANY_ID)).thenReturn(Optional.of(matrixCompany));
        when(companyRepository.existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE)).thenReturn(true);

        // When / Then
        assertThatCode(() -> companyDomainService.validateCanInactivate(VALID_COMPANY_ID))
                .doesNotThrowAnyException();

        verify(companyRepository, times(1)).findById(VALID_COMPANY_ID);
        verify(companyRepository, times(1)).existsByStatus(VALID_COMPANY_ID, StatusCompany.ACTIVE);
    }

    // ===== Active Company Validation Tests =====

    @Test
    @DisplayName("Should return true when there is an active company")
    void shouldReturnTrueWhenActiveCompanyExists() {
        // Given
        Long companyId = 1L;
        when(companyRepository.existsByStatus(companyId, StatusCompany.ACTIVE)).thenReturn(true);

        // When
        boolean result = companyDomainService.hasActiveCompany(companyId);

        // Then
        assert result;
        verify(companyRepository, times(1)).existsByStatus(companyId, StatusCompany.ACTIVE);
    }

    @Test
    @DisplayName("Should return false when there is no active company")
    void shouldReturnFalseWhenNoActiveCompanyExists() {
        // Given
        Long companyId = 1L;
        when(companyRepository.existsByStatus(companyId, StatusCompany.ACTIVE)).thenReturn(false);

        // When
        boolean result = companyDomainService.hasActiveCompany(companyId);

        // Then
        assert !result;
        verify(companyRepository, times(1)).existsByStatus(companyId, StatusCompany.ACTIVE);
    }
}

