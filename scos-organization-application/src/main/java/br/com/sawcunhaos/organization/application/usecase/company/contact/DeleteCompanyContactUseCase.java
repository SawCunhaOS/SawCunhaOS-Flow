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
import br.com.sawcunhaos.organization.application.dto.DeleteCompanyContactDTO;
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
public class DeleteCompanyContactUseCase implements ScosBaseUseCase<DeleteCompanyContactDTO, Void> {

    private final CompanyContactRepository companyContactRepository;
    private final CompanyDomainService companyDomainService;

    @Override
    public Void execute(DeleteCompanyContactDTO deleteCompanyContactDTO) {
        log.info("Deleting contact {} for company: {}", deleteCompanyContactDTO.getContactId(), deleteCompanyContactDTO.getCompanyId());

        companyDomainService.validateCompanyContactExistsValidation(
                deleteCompanyContactDTO.getCompanyId(), deleteCompanyContactDTO.getContactId()
        );

        companyContactRepository.deleteByCompanyIdAndId(
                deleteCompanyContactDTO.getCompanyId(), deleteCompanyContactDTO.getContactId()
        );

        log.info("Contact deleted: {}", deleteCompanyContactDTO.getContactId());
        return null;
    }
}
