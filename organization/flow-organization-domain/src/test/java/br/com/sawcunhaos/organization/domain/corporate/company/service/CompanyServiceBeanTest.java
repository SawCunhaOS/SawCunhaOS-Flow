
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

package br.com.sawcunhaos.organization.domain.corporate.company.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CnaeRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNatureRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_008;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_009;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_010;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_011;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link CompanyServiceBean}: unicidade de CNPJ, integridade de FKs, compatibilidade do
 * motivo de ativação, empresa mãe ativa e limite de profundidade da hierarquia. A ordem de erro
 * documentada é {@code 409 → 404 → 422}.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceBeanTest {

    private static final String VALID_CNPJ = "11222333000181";
    private static final String OTHER_VALID_CNPJ = "52508598049801";

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyStatusHistoryRepository companyStatusHistoryRepository;
    @Mock
    private LegalNatureRepository legalNatureRepository;
    @Mock
    private CnaeRepository cnaeRepository;
    @Mock
    private OrganizationConfigurationRepository organizationConfigurationRepository;
    @Mock
    private ReasonActivateService reasonActivateService;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;
    @Mock
    private CompanyMapper companyMapper;

    @InjectMocks
    private CompanyServiceBean companyServiceBean;

    private CompanyInput.CompanyInputBuilder validMatrixInput() {
        return CompanyInput.builder()
                .name("SawCunha Tecnologia LTDA")
                .nameTreatment("SawCunha")
                .taxIdentifier(VALID_CNPJ)
                .foundationDate(LocalDate.of(2020, 5, 10))
                .sectorOfActivity("Tecnologia da Informação")
                .reasonActivateId(10L);
    }

    private ReasonActivateOutput reason(boolean active, EntityType entityType) {
        return ReasonActivateOutput.builder().id(10L).code("ABERTURA").active(active).entityType(entityType).build();
    }

    // ---- create: unicidade (409) ----

    @Test
    void createShouldThrowWhenTaxIdentifierAlreadyExists() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(true);

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_002.getCode());

        verify(companyRepository, never()).merge(any());
    }

    // ---- create: motivo (422) ----

    @Test
    void createShouldThrowWhenReasonIsInactive() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(false, EntityType.COMPANY));

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_008.getCode());

        verify(companyRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenReasonEntityTypeIsIncompatible() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.EMPLOYEE));

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_009.getCode());
    }

    // ---- create: FKs (404) ----

    @Test
    void createShouldThrowWhenLegalNatureDoesNotExist() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(legalNatureRepository.exists(any(Predicate.class))).thenReturn(false);

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().legalNatureId(3L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_001.getCode());
    }

    @Test
    void createShouldThrowWhenCnaeDoesNotExist() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(cnaeRepository.exists(any(Predicate.class))).thenReturn(false);

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().cnaePrincipalId(42L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_001.getCode());
    }

    // ---- create: filial (404 / 422) ----

    @Test
    void createShouldThrowWhenParentCompanyDoesNotExist() {
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().parentCompanyId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_001.getCode());
    }

    @Test
    void createShouldThrowWhenParentCompanyIsNotActive() {
        Company parent = Company.builder().id(99L).status(StatusCompany.INACTIVE).build();
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(companyRepository.findById(99L)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().parentCompanyId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_010.getCode());
    }

    @Test
    void createShouldThrowWhenHierarchyDepthExceeded() {
        Company parent = Company.builder().id(99L).status(StatusCompany.ACTIVE).build();
        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(companyRepository.findById(99L)).thenReturn(Optional.of(parent));
        when(organizationConfigurationRepository.findById(ConfigurationKey.COMPANY_HIERARCHY_MAX_DEPTH))
                .thenReturn(Optional.of(OrganizationConfiguration.builder().value("1").build()));

        assertThatThrownBy(() -> companyServiceBean.create(validMatrixInput().parentCompanyId(99L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_011.getCode());
    }

    // ---- create: happy path ----

    @Test
    void createShouldPersistMatrixAndInitialStatusHistoryWhenValid() {
        Company persisted = Company.builder().id(1L).status(StatusCompany.ACTIVE).build();
        CompanyOutput output = CompanyOutput.builder().id(1L).status(StatusCompany.ACTIVE).build();

        when(companyRepository.existsByTaxIdentifier(VALID_CNPJ)).thenReturn(false);
        when(reasonActivateService.findById(10L)).thenReturn(reason(true, EntityType.COMPANY));
        when(companyRepository.merge(any(Company.class))).thenReturn(persisted);
        when(companyMapper.toCompanyOutput(persisted)).thenReturn(output);

        CompanyOutput result = companyServiceBean.create(validMatrixInput().build());

        assertThat(result).isEqualTo(output);
        verify(companyStatusHistoryRepository).merge(any(CompanyStatusHistory.class));
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenCompanyDoesNotExist() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyServiceBean.update(validMatrixInput().id(999L).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_001.getCode());
    }

    @Test
    void updateShouldThrowWhenTaxIdentifierBelongsToAnotherCompany() {
        Company existing = Company.builder().id(1L).status(StatusCompany.ACTIVE).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByTaxIdentifierAndNotId(OTHER_VALID_CNPJ, 1L)).thenReturn(true);

        assertThatThrownBy(() -> companyServiceBean.update(validMatrixInput().id(1L).taxIdentifier(OTHER_VALID_CNPJ).build()))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValidAndNotTouchParent() {
        Company existing = Company.builder().id(1L).status(StatusCompany.ACTIVE)
                .parentCompany(Company.builder().id(50L).build()).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByTaxIdentifierAndNotId(VALID_CNPJ, 1L)).thenReturn(false);

        companyServiceBean.update(validMatrixInput().id(1L).name("Novo Nome").build());

        assertThat(existing.getName()).isEqualTo("Novo Nome");
        assertThat(existing.getParentCompany().getId()).isEqualTo(50L);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        Company existing = Company.builder().id(1L).status(StatusCompany.ACTIVE).build();
        CompanyOutput output = CompanyOutput.builder().id(1L).status(StatusCompany.ACTIVE).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyMapper.toCompanyOutput(existing)).thenReturn(output);

        assertThat(companyServiceBean.findById(1L)).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnMappedPage() {
        Pageable pageable = Pageable.unpaged();
        Company existing = Company.builder().id(1L).status(StatusCompany.ACTIVE).build();
        CompanyOutput output = CompanyOutput.builder().id(1L).status(StatusCompany.ACTIVE).build();

        when(companyRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing)));
        when(companyMapper.toCompanyOutput(existing)).thenReturn(output);

        Page<CompanyOutput> result = companyServiceBean.findAll(StatusCompany.ACTIVE, "Saw", pageable);

        assertThat(result.getContent()).containsExactly(output);
    }

    // ---- resolveEffectiveZoneId (D2) ----

    @Test
    void resolveEffectiveZoneIdShouldReturnOwnTimeZoneWhenPresent() {
        Company company = Company.builder().id(1L).timeZone(ZoneId.of("America/Manaus")).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThat(companyServiceBean.resolveEffectiveZoneId(1L)).isEqualTo(ZoneId.of("America/Manaus"));
    }

    @Test
    void resolveEffectiveZoneIdShouldInheritFromParentWhenOwnIsNull() {
        Company parent = Company.builder().id(2L).timeZone(ZoneId.of("America/Manaus")).build();
        Company child = Company.builder().id(1L).timeZone(null).parentCompany(parent).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(child));

        assertThat(companyServiceBean.resolveEffectiveZoneId(1L)).isEqualTo(ZoneId.of("America/Manaus"));
    }

    @Test
    void resolveEffectiveZoneIdShouldInheritTwoLevelsUp() {
        Company grandparent = Company.builder().id(3L).timeZone(ZoneId.of("America/Manaus")).build();
        Company parent = Company.builder().id(2L).timeZone(null).parentCompany(grandparent).build();
        Company child = Company.builder().id(1L).timeZone(null).parentCompany(parent).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(child));

        assertThat(companyServiceBean.resolveEffectiveZoneId(1L)).isEqualTo(ZoneId.of("America/Manaus"));
    }

    @Test
    void resolveEffectiveZoneIdShouldFallbackToDefaultWhenWholeChainIsNull() {
        Company matrix = Company.builder().id(2L).timeZone(null).build();
        Company child = Company.builder().id(1L).timeZone(null).parentCompany(matrix).build();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(child));

        assertThat(companyServiceBean.resolveEffectiveZoneId(1L)).isEqualTo(Company.DEFAULT_TIME_ZONE);
    }

    // ---- assertNoCycle (AD-7) ----

    @Test
    void assertNoCycleShouldThrowWhenWouldCreateCycle() {
        when(companyRepository.wouldCreateCycle(3L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> companyServiceBean.assertNoCycle(3L, 1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_COMPANY_004.getCode());
    }

    @Test
    void assertNoCycleShouldNotThrowWhenNoCycle() {
        when(companyRepository.wouldCreateCycle(1L, 99L)).thenReturn(false);

        assertThatCode(() -> companyServiceBean.assertNoCycle(1L, 99L))
                .doesNotThrowAnyException();
    }
}
