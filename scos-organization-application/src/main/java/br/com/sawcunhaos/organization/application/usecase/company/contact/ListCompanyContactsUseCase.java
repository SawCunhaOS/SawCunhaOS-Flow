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

import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.ListCompanyContactsDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyContactMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListCompanyContactsUseCase implements ScosBaseUseCase<ListCompanyContactsDTO, Page<CompanyContactDTO>> {

    private final CompanyContactRepository companyContactRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyContactMapper companyContactMapper;

    @Override
    public Page<CompanyContactDTO> execute(ListCompanyContactsDTO listCompanyContactsDTO) {
        log.info("Listing contacts for company: {}", listCompanyContactsDTO.getCompanyId());

        companyDomainService.validateCompanyExistsValidation(listCompanyContactsDTO.getCompanyId());

        Page<CompanyContact> contactsPage = companyContactRepository.findAll(
                listCompanyContactsDTO.getCompanyId(), listCompanyContactsDTO.getPageable()
        );
        return contactsPage.map(companyContactMapper::toCompanyContactDTO);
    }
}
