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
import br.com.sawcunhaos.foundation.utils.valueobjects.Email;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyContactDTO;
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
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdateCompanyContactUseCase implements ScosBaseUseCase<UpdateCompanyContactDTO, Void> {

    private final CompanyContactRepository companyContactRepository;
    private final CompanyDomainService companyDomainService;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Void execute(UpdateCompanyContactDTO updateCompanyContactDTO) {
        log.info("Updating contact {} for company: {}", updateCompanyContactDTO.getContactId(), updateCompanyContactDTO.getCompanyId());

        companyDomainService.validateCompanyContactExistsValidation(
                updateCompanyContactDTO.getCompanyId(), updateCompanyContactDTO.getContactId()
        );

        CompanyContact contact = companyContactRepository
                .findCompanyContactByCompany(updateCompanyContactDTO.getCompanyId(), updateCompanyContactDTO.getContactId())
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_006));

        contact.setType(updateCompanyContactDTO.getType());
        contact.setPhone(updateCompanyContactDTO.getPhone());
        contact.setEmail(new Email(updateCompanyContactDTO.getEmail()));
        contact.setResponsiblePerson(updateCompanyContactDTO.getResponsiblePerson());
        contact.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return null;
    }
}
