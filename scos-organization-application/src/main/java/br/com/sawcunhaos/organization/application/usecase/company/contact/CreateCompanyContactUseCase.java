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
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UseCase: Criar contato de empresa
 *
 * POST /v1/companies/{companyId}/contacts
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateCompanyContactUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;

    public Long execute(Long companyId, CompanyContactDTO contactDTO) {
        log.info("Creating company contact for company: {}", companyId);

        // 1. Validar que company existe
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // 2. Criar contato
        CompanyContact contact = CompanyContact.builder()
                .company(company)
                .type(contactDTO.getType())
                .email(Optional.ofNullable(contactDTO.getEmail()).map(Email::new).orElse(null))
                .phone(contactDTO.getPhone())
                .responsiblePerson(contactDTO.getResponsiblePerson())
                .build();

        // 3. Persistir
        CompanyContact savedContact = companyContactRepository.persist(contact);

        log.info("Company contact created successfully with id: {}", savedContact.getId());
        return savedContact.getId();
    }
}

