
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.foundation.validation.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.CompanyStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CnaeRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNatureRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.QCnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.QLegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
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
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_005;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_006;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_008;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_009;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_010;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_011;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_012;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_013;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_014;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_015;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_016;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_017;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_COMPANY_018;
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
    private final ReasonInactivateService reasonInactivateService;
    private final ReasonDisableService reasonDisableService;
    private final ReasonEnableService reasonEnableService;
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
        return companyRepository.findAllFiltered(status, name, pageable).map(companyMapper::toCompanyOutput);
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

    /**
     * @throws ScosException SCOS_COMPANY_001 (404) se o {@code id} não existir.
     * @throws ScosException SCOS_COMPANY_007 (422) se a Empresa não estiver {@code INACTIVE}.
     * @throws ScosException SCOS_COMPANY_008/009 (422) para motivo de ativação inativo/incompatível.
     * @throws ScosException SCOS_REASON_ACTIVATE_001 (404) se o motivo não existir.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void activate(@NonNull Long id, @NonNull Long reasonActivateId, String observation) {
        log.info("Activate Company: {}", id);
        Company company = findCompanyById(id);
        validateReasonActivate(reasonActivateId);
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.activate(reasonActivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }

    /**
     * @throws ScosException SCOS_COMPANY_001 (404) se o {@code id} não existir.
     * @throws ScosException SCOS_COMPANY_005 (422) se for a última Empresa matriz ativa do sistema.
     * @throws ScosException SCOS_COMPANY_018 (422) se existir filial ativa em algum nível da subárvore.
     * @throws ScosException SCOS_COMPANY_007 (422) se a Empresa já estiver {@code INACTIVE}.
     * @throws ScosException SCOS_COMPANY_012/013 (422) para motivo de inativação inativo/incompatível.
     * @throws ScosException SCOS_REASON_INACTIVATE_001 (404) se o motivo não existir.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void inactivate(@NonNull Long id, @NonNull Long reasonInactivateId, String observation) {
        log.info("Inactivate Company: {}", id);
        Company company = findCompanyById(id);
        assertNotLastActiveMatrix(company);
        assertNoActiveDescendant(company);
        validateReasonInactivate(reasonInactivateId);
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.inactivate(reasonInactivateId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }

    /**
     * @throws ScosException SCOS_COMPANY_001 (404) se o {@code id} não existir.
     * @throws ScosException SCOS_COMPANY_006 (422) se for a única Empresa ativa do sistema.
     * @throws ScosException SCOS_COMPANY_018 (422) se existir filial ativa em algum nível da subárvore.
     * @throws ScosException SCOS_COMPANY_007 (422) se a Empresa não estiver {@code ACTIVE}.
     * @throws ScosException SCOS_COMPANY_014/015 (422) para motivo de bloqueio inativo/incompatível.
     * @throws ScosException SCOS_REASON_DISABLE_001 (404) se o motivo não existir.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void disable(@NonNull Long id, @NonNull Long reasonDisableId, String observation) {
        log.info("Disable (block) Company: {}", id);
        Company company = findCompanyById(id);
        assertNotOnlyActiveCompany(company);
        assertNoActiveDescendant(company);
        validateReasonDisable(reasonDisableId);
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.disable(reasonDisableId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }

    /**
     * @throws ScosException SCOS_COMPANY_001 (404) se o {@code id} não existir.
     * @throws ScosException SCOS_COMPANY_007 (422) se a Empresa não estiver {@code DISABLED}.
     * @throws ScosException SCOS_COMPANY_016/017 (422) para motivo de desbloqueio inativo/incompatível.
     * @throws ScosException SCOS_REASON_ENABLE_001 (404) se o motivo não existir.
     */
    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void enable(@NonNull Long id, @NonNull Long reasonEnableId, String observation) {
        log.info("Enable (unblock) Company: {}", id);
        Company company = findCompanyById(id);
        validateReasonEnable(reasonEnableId);
        String user = scosUserAuthentication.findUserAuthentication();

        CompanyStatusHistory history = company.enable(reasonEnableId);
        history.setObservation(observation);
        history.setUserAt(user);
        companyStatusHistoryRepository.merge(history);
    }

    @Override
    public Company findCompanyById(@NonNull Long companyId) {
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

    private void assertNotLastActiveMatrix(Company company) {
        if (company.isMatrix() && company.isActive() && !companyRepository.existsOtherActiveMatrix(company.getId())) {
            throw new ScosException(SCOS_COMPANY_005);
        }
    }

    private void assertNotOnlyActiveCompany(Company company) {
        if (!companyRepository.existsByStatus(company.getId(), StatusCompany.ACTIVE)) {
            throw new ScosException(SCOS_COMPANY_006);
        }
    }

    private void assertNoActiveDescendant(Company company) {
        if (companyRepository.hasActiveDescendant(company.getId())) {
            throw new ScosException(SCOS_COMPANY_018);
        }
    }

    private void validateReasonInactivate(Long reasonInactivateId) {
        ReasonInactivateOutput reason = reasonInactivateService.findById(reasonInactivateId);
        if (!reason.active()) {
            throw new ScosException(SCOS_COMPANY_012);
        }
        if (reason.entityType() != EntityType.COMPANY) {
            throw new ScosException(SCOS_COMPANY_013);
        }
    }

    private void validateReasonDisable(Long reasonDisableId) {
        ReasonDisableOutput reason = reasonDisableService.findById(reasonDisableId);
        if (!reason.active()) {
            throw new ScosException(SCOS_COMPANY_014);
        }
        if (reason.entityType() != EntityType.COMPANY) {
            throw new ScosException(SCOS_COMPANY_015);
        }
    }

    private void validateReasonEnable(Long reasonEnableId) {
        ReasonEnableOutput reason = reasonEnableService.findById(reasonEnableId);
        if (!reason.active()) {
            throw new ScosException(SCOS_COMPANY_016);
        }
        if (reason.entityType() != EntityType.COMPANY) {
            throw new ScosException(SCOS_COMPANY_017);
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
