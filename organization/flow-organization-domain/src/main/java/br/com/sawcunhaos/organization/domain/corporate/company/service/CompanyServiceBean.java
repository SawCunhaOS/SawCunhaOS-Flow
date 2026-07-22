
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
import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.QCnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.QCompany;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.QLegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CnaeRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNatureRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_008;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_009;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_010;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_011;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONFIGURATION_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_001;

/**
 * Implementação de {@link CompanyService} — regras de negócio da criação/atualização da Empresa que
 * dependem do banco ou de outros agregados. Validações de formato/DV ficam no contrato; guardas de
 * estado ficam no agregado {@link Company}. Ordem de erro: unicidade (409) → FK (404) → regra (422).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CompanyServiceBean implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyStatusHistoryRepository companyStatusHistoryRepository;
    private final LegalNatureRepository legalNatureRepository;
    private final CnaeRepository cnaeRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final ReasonActivateService reasonActivateService;
    private final ScosUserAuthentication scosUserAuthentication;
    private final CompanyMapper companyMapper;

    /**
     * @throws ScosException SCOS_COMPANY_002 se o {@code taxIdentifier} já existir (qualquer status).
     * @throws ScosException SCOS_REASON_ACTIVATE_001 / SCOS_LEGAL_NATURE_001 / SCOS_CNAE_001 / SCOS_COMPANY_001 (404) se alguma FK não existir.
     * @throws ScosException SCOS_COMPANY_008/009/010/011 (422) para motivo inativo/incompatível, mãe inativa ou profundidade excedida.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public CompanyOutput create(@NonNull CompanyInput companyInput) {
        log.info("Create Company: {}", companyInput.taxIdentifier());

        if (companyRepository.existsByTaxIdentifier(companyInput.taxIdentifier())) {
            throw new ScosException(SCOS_COMPANY_002);
        }

        validateReasonActivate(companyInput.reasonActivateId());
        validateLegalNature(companyInput.legalNatureId());
        validateCnae(companyInput.cnaePrincipalId());
        Company parentCompany = resolveParentCompany(companyInput.parentCompanyId());

        String user = scosUserAuthentication.findUserAuthentication();

        Company company = Company.builder()
                .name(companyInput.name())
                .nameTreatment(companyInput.nameTreatment())
                .taxIdentifier(new Cnpj(companyInput.taxIdentifier()))
                .foundationDate(companyInput.foundationDate())
                .sectorOfActivity(companyInput.sectorOfActivity())
                .observation(companyInput.observation())
                .status(StatusCompany.ACTIVE)
                .stateRegistration(companyInput.stateRegistration())
                .municipalRegistration(companyInput.municipalRegistration())
                .legalNature(legalNatureReference(companyInput.legalNatureId()))
                .cnaePrincipal(cnaeReference(companyInput.cnaePrincipalId()))
                .parentCompany(parentCompany)
                .build();
        company.updateAuditInfo(user);

        company = companyRepository.merge(company);

        companyStatusHistoryRepository.merge(
                CompanyStatusHistory.builder()
                        .company(company)
                        .status(StatusCompany.ACTIVE)
                        .reasonActivate(ReasonActivate.builder().id(companyInput.reasonActivateId()).build())
                        .userAt(user)
                        .build()
        );

        return companyMapper.toCompanyOutput(company);
    }

    /**
     * @throws ScosException SCOS_COMPANY_001 (404) se o {@code id} não existir.
     * @throws ScosException SCOS_COMPANY_002 (409) se o novo {@code taxIdentifier} colidir com outra empresa.
     * @throws ScosException SCOS_LEGAL_NATURE_001 / SCOS_CNAE_001 (404) se a FL fiscal informada não existir.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void update(@NonNull CompanyInput companyInput) {
        log.info("Update Company: {}", companyInput.id());
        Company company = findCompanyById(companyInput.id());

        if (companyRepository.existsByTaxIdentifierAndNotId(companyInput.taxIdentifier(), companyInput.id())) {
            throw new ScosException(SCOS_COMPANY_002);
        }

        validateLegalNature(companyInput.legalNatureId());
        validateCnae(companyInput.cnaePrincipalId());

        company.setName(companyInput.name());
        company.setNameTreatment(companyInput.nameTreatment());
        company.setFoundationDate(companyInput.foundationDate());
        company.setSectorOfActivity(companyInput.sectorOfActivity());
        company.setObservation(companyInput.observation());
        company.setStateRegistration(companyInput.stateRegistration());
        company.setMunicipalRegistration(companyInput.municipalRegistration());
        company.setLegalNature(legalNatureReference(companyInput.legalNatureId()));
        company.setCnaePrincipal(cnaeReference(companyInput.cnaePrincipalId()));
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        companyRepository.update(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyOutput findById(@NonNull Long companyId) {
        log.info("Find Company by Id: {}", companyId);
        return companyMapper.toCompanyOutput(findCompanyById(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyOutput> findAll(StatusCompany status, String name, @NonNull Pageable pageable) {
        log.info("Find All Companies, Status: {}, Name: {}", status, name);

        BooleanBuilder predicate = new BooleanBuilder();
        if (status != null) {
            predicate.and(QCompany.company.status.eq(status));
        }
        if (name != null && !name.isBlank()) {
            predicate.and(QCompany.company.name.containsIgnoreCase(name));
        }
        if (!predicate.hasValue()) {
            predicate.and(QCompany.company.id.isNotNull());
        }

        return companyRepository.findAll(predicate, pageable).map(companyMapper::toCompanyOutput);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneId resolveEffectiveZoneId(@NonNull Long companyId) {
        Company current = findCompanyById(companyId);
        while (current != null) {
            if (current.getTimeZone() != null) {
                return current.getTimeZone();
            }
            current = current.getParentCompany();
        }
        return Company.DEFAULT_TIME_ZONE;
    }

    /**
     * @throws ScosException SCOS_COMPANY_004 (422) se atribuir {@code candidateParentCompanyId} como
     *                        pai de {@code companyId} fechar um ciclo (direto ou indireto).
     */
    @Override
    @Transactional(readOnly = true)
    public void assertNoCycle(@NonNull Long companyId, @NonNull Long candidateParentCompanyId) {
        if (companyRepository.wouldCreateCycle(companyId, candidateParentCompanyId)) {
            throw new ScosException(SCOS_COMPANY_004);
        }
    }

    private Company findCompanyById(@NonNull Long companyId) {
        return companyRepository.findById(companyId).orElseThrow(
                () -> new ScosException(SCOS_COMPANY_001)
        );
    }

    private void validateReasonActivate(Long reasonActivateId) {
        ReasonActivateOutput reasonActivate = reasonActivateService.findById(reasonActivateId);
        if (!reasonActivate.active()) {
            throw new ScosException(SCOS_COMPANY_008);
        }
        if (reasonActivate.entityType() != EntityType.COMPANY) {
            throw new ScosException(SCOS_COMPANY_009);
        }
    }

    private void validateLegalNature(Long legalNatureId) {
        if (legalNatureId != null && !legalNatureRepository.exists(QLegalNature.legalNature.id.eq(legalNatureId))) {
            throw new ScosException(SCOS_LEGAL_NATURE_001);
        }
    }

    private void validateCnae(Long cnaeId) {
        if (cnaeId != null && !cnaeRepository.exists(QCnae.cnae.id.eq(cnaeId))) {
            throw new ScosException(SCOS_CNAE_001);
        }
    }

    private Company resolveParentCompany(Long parentCompanyId) {
        if (parentCompanyId == null) {
            return null;
        }
        Company parent = findCompanyById(parentCompanyId);
        if (!parent.isActive()) {
            throw new ScosException(SCOS_COMPANY_010);
        }
        if (depthOf(parent) + 1 > maxHierarchyDepth()) {
            throw new ScosException(SCOS_COMPANY_011);
        }
        return parent;
    }

    private int depthOf(Company company) {
        int depth = 1;
        Company current = company;
        while (current.getParentCompany() != null) {
            current = current.getParentCompany();
            depth++;
        }
        return depth;
    }

    private int maxHierarchyDepth() {
        OrganizationConfiguration configuration = organizationConfigurationRepository
                .findById(ConfigurationKey.COMPANY_HIERARCHY_MAX_DEPTH)
                .orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_001));
        return Integer.parseInt(configuration.getValue());
    }

    private LegalNature legalNatureReference(Long legalNatureId) {
        return legalNatureId == null ? null : LegalNature.builder().id(legalNatureId).build();
    }

    private Cnae cnaeReference(Long cnaeId) {
        return cnaeId == null ? null : Cnae.builder().id(cnaeId).build();
    }
}
