
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

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Atualizar dados de uma empresa existente
 *
 * Fluxo:
 * 1. Buscar empresa por ID
 * 2. Validar domínio (parent válido, sem ciclos, foundationDate válida)
 * 3. Atualizar campos
 * 4. Persistir
 * 5. Registrar auditoria
 *
 * Restrições:
 * - CNPJ é imutável (não pode ser alterado)
 * - Status deve ser alterado via endpoint específico (PUT /companies/{id}/status)
 *
 * Erros esperados:
 * - SCOS_COMPANY_001: Company não encontrada
 * - SCOS_COMPANY_004: Ciclo detectado em hierarquia
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdateCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyMapper companyMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    public void execute(Long companyId, UpdateCompanyDTO updateCompanyDTO) {
        log.info("Updating company: id={}", companyId);

        // 1. Buscar empresa
        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // 2. Validar parent company se alterado
        if (updateCompanyDTO.getParentCompanyId() != null &&
            (company.getParentCompany() == null || !company.getParentCompany().getId().equals(updateCompanyDTO.getParentCompanyId()))) {

            companyDomainService.validateParentCompanyExists(updateCompanyDTO.getParentCompanyId());
            companyDomainService.validateParentCompanyIsActive(updateCompanyDTO.getParentCompanyId());
            companyDomainService.validateNoCyclicHierarchy(companyId, updateCompanyDTO.getParentCompanyId());

            // Atualizar parent company
            Company newParent = companyRepository.getReferenceById(updateCompanyDTO.getParentCompanyId());
            company.setParentCompany(newParent);
        }

        // 3. Validar foundation date
        if (updateCompanyDTO.getFoundationDate() != null) {
            companyDomainService.validateFoundationDateNotFuture(updateCompanyDTO.getFoundationDate());
        }

        // 4. Atualizar campos
        companyMapper.updateCompany(updateCompanyDTO, company);

        // 5. Adicionar informações de auditoria
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        // 6. Persistir
        companyRepository.update(company);

        log.info("Company updated successfully: id={}", companyId);
    }
}
