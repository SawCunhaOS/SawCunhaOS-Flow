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

package br.com.sawcunhaos.organization.application.usecase.company.address;

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyAddressMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateCompanyAddressUseCase implements ScosBaseUseCase<CreateCompanyAddressDTO, Long> {

    private final CompanyAddressRepository companyAddressRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyAddressMapper companyAddressMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Long execute(CreateCompanyAddressDTO createCompanyAddressDTO) {
        log.info("Creating address for company: {}", createCompanyAddressDTO.getCompanyId());

        companyDomainService.validateCompanyExistsValidation(createCompanyAddressDTO.getCompanyId());
        companyDomainService.validateCompanyIsActiveForSubResourceValidation(createCompanyAddressDTO.getCompanyId());

        CompanyAddress companyAddress = companyAddressMapper.toCompanyAddress(createCompanyAddressDTO);
        companyAddress.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return companyAddressRepository.persist(companyAddress).getId();
    }
}
