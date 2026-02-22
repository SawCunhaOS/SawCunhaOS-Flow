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
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Alterar status de uma empresa (ACTIVE <-> INACTIVE)
 *
 * Fluxo:
 * 1. Buscar empresa por ID
 * 2. Validar transição de status
 * 3. Se inativando: validar se é última matriz ativa
 * 4. Atualizar status
 * 5. Persistir
 *
 * Erros esperados:
 * - SCOS_COMPANY_001: Company não encontrada
 * - SCOS_COMPANY_005: Última matriz ativa não pode ser inativada
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class ChangeCompanyStatusUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final ScosUserAuthentication scosUserAuthentication;

    public void execute(Long companyId, String status) {
        log.info("Changing company status: id={}, newStatus={}", companyId, status);

        StatusCompany newStatus = StatusCompany.valueOf(status);

        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // Validar transições de status
        if (newStatus == StatusCompany.INACTIVE) {
            companyDomainService.validateCanInactivate(companyId);
            company.inactivate();
        } else if (newStatus == StatusCompany.ACTIVE) {
            company.activate();
        }

        // Adicionar informações de auditoria
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        // Persistir
        companyRepository.update(company);

        log.info("Company status changed successfully: id={}, newStatus={}", companyId, newStatus);
    }
}

