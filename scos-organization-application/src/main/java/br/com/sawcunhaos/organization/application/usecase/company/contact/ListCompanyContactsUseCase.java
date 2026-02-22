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
import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Listar contatos de empresa
 *
 * GET /v1/companies/{companyId}/contacts
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListCompanyContactsUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;

    public Page<CompanyContactDTO> execute(Long companyId, Pageable pageable) {
        log.info("Listing company contacts: companyId={}", companyId);

        // 1. Validar que company existe
        if (!companyRepository.existsById(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        // 2. Listar contatos
        Page<CompanyContact> contacts = companyContactRepository.findAll(companyId, pageable);

        // 3. Mapear para DTOs
        return contacts.map(contact -> CompanyContactDTO.builder()
                .id(contact.getId())
                .responsiblePerson(contact.getResponsiblePerson())
                .type(contact.getType())
                .email(contact.getEmail() != null ? contact.getEmail().getEmail() : null)
                .phone(contact.getPhone())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build());
    }
}

