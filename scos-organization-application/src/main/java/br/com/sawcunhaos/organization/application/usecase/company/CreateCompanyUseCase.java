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
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        log.info("Creating company: {}", createCompanyDTO.getName());

        // Normalize CNPJ (remove formatting) before uniqueness check
        String cnpjNormalized = createCompanyDTO.getTaxIdentifier().replaceAll("[^0-9]", "");
        createCompanyDTO.setTaxIdentifier(cnpjNormalized);

        // Validate CNPJ uniqueness
        companyDomainService.validateTaxIdentifierUniqueValidation(cnpjNormalized);

        // Validate parent company if provided
        if (createCompanyDTO.getParentCompanyId() != null) {
            companyDomainService.validateParentCompanyExistsAndActiveValidation(createCompanyDTO.getParentCompanyId());
        }

        Company company = companyMapper.toCompany(createCompanyDTO);
        company.setStatus(StatusCompany.ACTIVE);
        company.setActive(true);
        company.defineDateCreated();
        company.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return companyRepository.persist(company).getId();
    }
}
