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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.GetCompanyContactDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyContactMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_COMPANY_006;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetCompanyContactByIdUseCase implements ScosBaseUseCase<GetCompanyContactDTO, CompanyContactDTO> {

    private final CompanyContactRepository companyContactRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyContactMapper companyContactMapper;

    @Override
    public CompanyContactDTO execute(GetCompanyContactDTO getCompanyContactDTO) {
        log.info("Getting contact {} for company: {}", getCompanyContactDTO.getContactId(), getCompanyContactDTO.getCompanyId());

        companyDomainService.validateCompanyContactExistsValidation(
                getCompanyContactDTO.getCompanyId(), getCompanyContactDTO.getContactId()
        );

        CompanyContact companyContact = companyContactRepository
                .findCompanyContactByCompany(getCompanyContactDTO.getCompanyId(), getCompanyContactDTO.getContactId())
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_006));

        return companyContactMapper.toCompanyContactDTO(companyContact);
    }
}
