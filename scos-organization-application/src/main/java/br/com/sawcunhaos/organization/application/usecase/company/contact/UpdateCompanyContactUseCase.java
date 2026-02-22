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
import br.com.sawcunhaos.foundation.utils.valueobjects.Email;
import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Atualizar contato de empresa
 *
 * PUT /v1/companies/{companyId}/contacts/{contactId}
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdateCompanyContactUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;

    public void execute(Long companyId, Long contactId, CompanyContactDTO contactDTO) {
        log.info("Updating company contact: companyId={}, contactId={}", companyId, contactId);

        // 1. Validar que company existe
        if (!companyRepository.existsById(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        // 2. Buscar contato
        CompanyContact contact = companyContactRepository.findById(contactId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // 3. Validar que contato pertence à company
        if (!contact.getCompany().getId().equals(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        // 4. Atualizar campos
        contact.setType(contactDTO.getType());
        if (contactDTO.getEmail() != null) {
            contact.setEmail(new Email(contactDTO.getEmail()));
        }
        if (contactDTO.getPhone() != null) {
            contact.setPhone(contactDTO.getPhone());
        }
        contact.setResponsiblePerson(contactDTO.getResponsiblePerson());

        // 5. Persistir
        companyContactRepository.persist(contact);

        log.info("Company contact updated successfully: id={}", contactId);
    }
}

