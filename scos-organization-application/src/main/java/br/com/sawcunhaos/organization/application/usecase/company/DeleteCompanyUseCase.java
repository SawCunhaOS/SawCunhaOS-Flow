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
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Deletar uma empresa
 *
 * Fluxo:
 * 1. Buscar empresa por ID
 * 2. Validar se pode ser deletada (sem dependências)
 * 3. Marcar como DELETED (soft-delete)
 * 4. Persistir
 *
 * Erros esperados:
 * - SCOS_COMPANY_001: Company não encontrada
 * - SCOS_COMPANY_003: Company tem dependências ativas
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class DeleteCompanyUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final ScosUserAuthentication scosUserAuthentication;

    public void execute(Long companyId) {
        log.info("Deleting company: id={}", companyId);

        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // Validar se pode ser deletada
        companyDomainService.validateCanDelete(companyId);

        // Marcar como deletada (soft-delete)
        company.delete();

        // Adicionar informações de auditoria
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        // Persistir
        companyRepository.update(company);

        log.info("Company deleted successfully (soft-delete): id={}", companyId);
    }
}

