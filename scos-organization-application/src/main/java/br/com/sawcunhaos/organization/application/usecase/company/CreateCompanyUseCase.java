
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
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Criar nova empresa (matriz ou filial)
 *
 * Fluxo:
 * 1. Validar entrada (field-level) - contrato de API
 * 2. Validar domínio (CNPJ único, parent válido, sem ciclos, foundationDate válida)
 * 3. Criar entidade Company
 * 4. Persistir em BD
 * 5. Registrar auditoria
 * 6. Retornar ID da empresa criada
 *
 * Erros esperados:
 * - SCOS_COMPANY_001: Parent company não encontrada
 * - SCOS_COMPANY_002: CNPJ já existe
 * - SCOS_COMPANY_004: Ciclo detectado em hierarquia
 * - Erros de validação de campo (SCOS-001, SCOS-003, SCOS-004, SCOS-010)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateCompanyUseCase implements ScosBaseUseCase<CreateCompanyDTO, Long> {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyMapper companyMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Long execute(CreateCompanyDTO createCompanyDTO) {
        log.info("Creating new company: name={}, cnpj={}",
            createCompanyDTO.getName(),
            createCompanyDTO.getTaxIdentifier());

        // 1. Validar CNPJ única
        companyDomainService.validateCnpjUniqueness(createCompanyDTO.getTaxIdentifier());

        // 2. Validar parent company se informada
        if (createCompanyDTO.getParentCompanyId() != null) {
            companyDomainService.validateParentCompanyExists(createCompanyDTO.getParentCompanyId());
            companyDomainService.validateParentCompanyIsActive(createCompanyDTO.getParentCompanyId());
            companyDomainService.validateNoCyclicHierarchy(null, createCompanyDTO.getParentCompanyId());
        }

        // 3. Validar foundation date
        companyDomainService.validateFoundationDateNotFuture(createCompanyDTO.getFoundationDate());

        // 4. Mapear DTO para entidade
        Company company = companyMapper.toCompany(createCompanyDTO);

        // 5. Definir parent company se informada
        if (createCompanyDTO.getParentCompanyId() != null) {
            Company parentCompany = companyRepository.getReferenceById(createCompanyDTO.getParentCompanyId());
            company.setParentCompany(parentCompany);
        }

        // 6. Adicionar informações de auditoria
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        // 7. Definir data de criação
        company.defineDateCreated();

        // 8. Persistir
        Company savedCompany = companyRepository.persist(company);

        log.info("Company created successfully with id: {}", savedCompany.getId());
        return savedCompany.getId();
    }
}
