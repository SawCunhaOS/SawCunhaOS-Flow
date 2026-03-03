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

package br.com.sawcunhaos.organization.application.usecase.company.contact;

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyContactDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyContactMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateCompanyContactUseCase implements ScosBaseUseCase<CreateCompanyContactDTO, Long> {

    private final CompanyContactRepository companyContactRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyContactMapper companyContactMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Long execute(CreateCompanyContactDTO createCompanyContactDTO) {
        log.info("Creating contact for company: {}", createCompanyContactDTO.getCompanyId());

        companyDomainService.validateCompanyExistsValidation(createCompanyContactDTO.getCompanyId());
        companyDomainService.validateCompanyIsActiveForSubResourceValidation(createCompanyContactDTO.getCompanyId());

        CompanyContact companyContact = companyContactMapper.toCompanyContact(createCompanyContactDTO);
        companyContact.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return companyContactRepository.persist(companyContact).getId();
    }
}
