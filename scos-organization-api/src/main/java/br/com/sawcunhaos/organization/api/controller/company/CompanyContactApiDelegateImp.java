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

package br.com.sawcunhaos.organization.api.controller.company;

import br.com.sawcunhaos.organization.api.controller.CompanyContactApiDelegate;
import br.com.sawcunhaos.organization.api.dto.CompanyContact;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyContactRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllCompanyContactsResponse;
import br.com.sawcunhaos.organization.api.dto.GetCompanyContactResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateCompanyContactRequest;
import br.com.sawcunhaos.organization.api.enumeration.CompanyOrder;
import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.DeleteCompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.GetCompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.ListCompanyContactsDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyContactDTO;
import br.com.sawcunhaos.organization.application.usecase.company.contact.CreateCompanyContactUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.contact.DeleteCompanyContactUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.contact.GetCompanyContactByIdUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.contact.ListCompanyContactsUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.contact.UpdateCompanyContactUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createPageable;
import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createScosPaginated;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyContactApiDelegateImp implements CompanyContactApiDelegate {

    private final CreateCompanyContactUseCase createCompanyContactUseCase;
    private final GetCompanyContactByIdUseCase getCompanyContactByIdUseCase;
    private final ListCompanyContactsUseCase listCompanyContactsUseCase;
    private final UpdateCompanyContactUseCase updateCompanyContactUseCase;
    private final DeleteCompanyContactUseCase deleteCompanyContactUseCase;

    @Override
    public CreateResponse createCompanyContact(Long companyId, CreateCompanyContactRequest createCompanyContactRequest,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Creating contact for company: {}", companyId);

        CreateCompanyContactDTO dto = CreateCompanyContactDTO.builder()
                .companyId(companyId)
                .type(createCompanyContactRequest.type())
                .phone(createCompanyContactRequest.phone())
                .email(createCompanyContactRequest.email())
                .responsiblePerson(createCompanyContactRequest.responsiblePerson())
                .build();

        Long contactId = createCompanyContactUseCase.execute(dto);

        return CreateResponse.builder()
                .data(Create.builder().id(contactId).build())
                .build();
    }

    @Override
    public GetAllCompanyContactsResponse getAllCompanyContacts(PaginationFilter paginationFilter, Long companyId,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting all contacts for company: {}", companyId);

        ListCompanyContactsDTO listDto = ListCompanyContactsDTO.builder()
                .companyId(companyId)
                .pageable(createPageable(paginationFilter, CompanyOrder.ID))
                .build();

        Page<CompanyContactDTO> contactPage = listCompanyContactsUseCase.execute(listDto);

        return GetAllCompanyContactsResponse.builder()
                .data(
                        contactPage.getContent().stream().map(c ->
                                CompanyContact.builder()
                                        .id(c.getId())
                                        .type(c.getType())
                                        .phone(c.getPhone())
                                        .email(c.getEmail())
                                        .responsiblePerson(c.getResponsiblePerson())
                                        .build()
                        ).toList()
                )
                .paginatedDTO(createScosPaginated(contactPage))
                .build();
    }

    @Override
    public GetCompanyContactResponse getCompanyContactById(Long companyId, Long id,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting contact {} for company: {}", id, companyId);

        CompanyContactDTO dto = getCompanyContactByIdUseCase.execute(
                GetCompanyContactDTO.builder().companyId(companyId).contactId(id).build()
        );

        return GetCompanyContactResponse.builder()
                .data(CompanyContact.builder()
                        .id(dto.getId())
                        .type(dto.getType())
                        .phone(dto.getPhone())
                        .email(dto.getEmail())
                        .responsiblePerson(dto.getResponsiblePerson())
                        .build())
                .build();
    }

    @Override
    public Void updateCompanyContact(Long companyId, Long id, UpdateCompanyContactRequest updateCompanyContactRequest,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Updating contact {} for company: {}", id, companyId);

        UpdateCompanyContactDTO dto = UpdateCompanyContactDTO.builder()
                .companyId(companyId)
                .contactId(id)
                .type(updateCompanyContactRequest.type())
                .phone(updateCompanyContactRequest.phone())
                .email(updateCompanyContactRequest.email())
                .responsiblePerson(updateCompanyContactRequest.responsiblePerson())
                .build();

        updateCompanyContactUseCase.execute(dto);

        return null;
    }

    @Override
    public Void deleteCompanyContact(Long companyId, Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Deleting contact {} for company: {}", id, companyId);

        deleteCompanyContactUseCase.execute(
                DeleteCompanyContactDTO.builder().companyId(companyId).contactId(id).build()
        );

        return null;
    }
}
