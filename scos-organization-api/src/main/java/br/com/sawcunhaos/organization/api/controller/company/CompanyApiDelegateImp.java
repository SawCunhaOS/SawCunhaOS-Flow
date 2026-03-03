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

import br.com.sawcunhaos.organization.api.controller.CompanyApiDelegate;
import br.com.sawcunhaos.organization.api.dto.Companies;
import br.com.sawcunhaos.organization.api.dto.Company;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllCompaniesResponse;
import br.com.sawcunhaos.organization.api.dto.GetCompanyResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ParentCompany;
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import br.com.sawcunhaos.organization.api.dto.UpdateCompanyRequest;
import br.com.sawcunhaos.organization.api.enumeration.CompanyOrder;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.application.usecase.company.ActivateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.CreateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.DeleteCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.GetCompanyByIdUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.InactivateCompanyUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.ListCompaniesUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.UpdateCompanyUseCase;
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
public class CompanyApiDelegateImp implements CompanyApiDelegate {

    private final ListCompaniesUseCase listCompaniesUseCase;
    private final CreateCompanyUseCase createCompanyUseCase;
    private final GetCompanyByIdUseCase getCompanyByIdUseCase;
    private final UpdateCompanyUseCase updateCompanyUseCase;
    private final DeleteCompanyUseCase deleteCompanyUseCase;
    private final ActivateCompanyUseCase activateCompanyUseCase;
    private final InactivateCompanyUseCase inactivateCompanyUseCase;

    @Override
    public GetAllCompaniesResponse getAllCompanies(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting all companies with pagination: {}", paginationFilter);

        Page<CompanyDTO> companyPage = listCompaniesUseCase.execute(
                createPageable(paginationFilter, CompanyOrder.ID)
        );

        return GetAllCompaniesResponse.builder()
                .data(
                        companyPage.getContent().stream().map(dto ->
                                Companies.builder()
                                        .id(dto.getId())
                                        .name(dto.getName())
                                        .nameTreatment(dto.getNameTreatment())
                                        .sectorOfActivity(dto.getSectorOfActivity())
                                        .observation(dto.getObservation())
                                        .active(dto.isActive())
                                        .status(StatusCompany.fromValue(dto.getStatus()))
                                        .build()
                        ).toList()
                )
                .paginatedDTO(createScosPaginated(companyPage))
                .build();
    }

    @Override
    public CreateResponse createCompany(CreateCompanyRequest createCompanyRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Creating company: {}", createCompanyRequest.name());

        CreateCompanyDTO createCompanyDTO = CreateCompanyDTO.builder()
                .name(createCompanyRequest.name())
                .nameTreatment(createCompanyRequest.nameTreatment())
                .taxIdentifier(createCompanyRequest.taxIdentifier())
                .foundationDate(createCompanyRequest.foundationDate())
                .sectorOfActivity(createCompanyRequest.sectorOfActivity())
                .observation(createCompanyRequest.observation())
                .parentCompanyId(createCompanyRequest.parentCompanyId())
                .build();

        Long companyId = createCompanyUseCase.execute(createCompanyDTO);

        return CreateResponse.builder()
                .data(Create.builder().id(companyId).build())
                .build();
    }

    @Override
    public GetCompanyResponse getCompanyById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting company by id: {}", id);

        CompanyDTO dto = getCompanyByIdUseCase.execute(id);

        ParentCompany parentCompany = dto.getParentCompanyId() != null
                ? ParentCompany.builder()
                        .id(dto.getParentCompanyId())
                        .name(dto.getParentCompanyName())
                        .nameTreatment(dto.getParentCompanyNameTreatment())
                        .taxIdentifier(dto.getParentCompanyTaxIdentifier())
                        .build()
                : null;

        return GetCompanyResponse.builder()
                .data(Company.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .nameTreatment(dto.getNameTreatment())
                        .taxIdentifier(dto.getTaxIdentifier())
                        .foundationDate(dto.getFoundationDate())
                        .dateCreated(dto.getDateCreated())
                        .sectorOfActivity(dto.getSectorOfActivity())
                        .observation(dto.getObservation())
                        .active(dto.isActive())
                        .status(StatusCompany.fromValue(dto.getStatus()))
                        .parentCompany(parentCompany)
                        .build())
                .build();
    }

    @Override
    public Void updateCompany(Long id, UpdateCompanyRequest updateCompanyRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Updating company with id: {}", id);

        UpdateCompanyDTO updateCompanyDTO = UpdateCompanyDTO.builder()
                .companyId(id)
                .name(updateCompanyRequest.name())
                .nameTreatment(updateCompanyRequest.nameTreatment())
                .foundationDate(updateCompanyRequest.foundationDate())
                .sectorOfActivity(updateCompanyRequest.sectorOfActivity())
                .observation(updateCompanyRequest.observation())
                .parentCompanyId(updateCompanyRequest.parentCompanyId())
                .build();

        updateCompanyUseCase.execute(updateCompanyDTO);

        return null;
    }

    @Override
    public Void deleteCompany(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Deleting company with id: {}", id);

        deleteCompanyUseCase.execute(id);

        return null;
    }

    @Override
    public Void activateCompany(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Activating company with id: {}", id);

        activateCompanyUseCase.execute(id);

        return null;
    }

    @Override
    public Void inactivateCompany(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Inactivating company with id: {}", id);

        inactivateCompanyUseCase.execute(id);

        return null;
    }
}
