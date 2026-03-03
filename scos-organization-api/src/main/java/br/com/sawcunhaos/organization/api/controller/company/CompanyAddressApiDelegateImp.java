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

import br.com.sawcunhaos.organization.api.controller.CompanyAddressApiDelegate;
import br.com.sawcunhaos.organization.api.dto.CompanyAddress;
import br.com.sawcunhaos.organization.api.dto.CompanyAddresses;
import br.com.sawcunhaos.organization.api.dto.Create;
import br.com.sawcunhaos.organization.api.dto.CreateCompanyAddressRequest;
import br.com.sawcunhaos.organization.api.dto.CreateResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllCompanyAddressResponse;
import br.com.sawcunhaos.organization.api.dto.GetCompanyAddressResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateCompanyAddressRequest;
import br.com.sawcunhaos.organization.api.enumeration.CompanyOrder;
import br.com.sawcunhaos.organization.application.dto.CompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.DeleteCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.GetCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.ListCompanyAddressesDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.usecase.company.address.CreateCompanyAddressUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.address.DeleteCompanyAddressUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.address.GetCompanyAddressByIdUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.address.ListCompanyAddressesUseCase;
import br.com.sawcunhaos.organization.application.usecase.company.address.UpdateCompanyAddressUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createPageable;
import static br.com.sawcunhaos.organization.api.utils.PaginatioUtils.createScosPaginated;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyAddressApiDelegateImp implements CompanyAddressApiDelegate {

    private final CreateCompanyAddressUseCase createCompanyAddressUseCase;
    private final GetCompanyAddressByIdUseCase getCompanyAddressByIdUseCase;
    private final ListCompanyAddressesUseCase listCompanyAddressesUseCase;
    private final UpdateCompanyAddressUseCase updateCompanyAddressUseCase;
    private final DeleteCompanyAddressUseCase deleteCompanyAddressUseCase;

    @Override
    public CreateResponse createCompanyAddress(Long companyId, CreateCompanyAddressRequest createCompanyAddressRequest,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Creating address for company: {}", companyId);

        CreateCompanyAddressDTO dto = CreateCompanyAddressDTO.builder()
                .companyId(companyId)
                .addressId(createCompanyAddressRequest.addressId())
                .type(createCompanyAddressRequest.type())
                .number(createCompanyAddressRequest.number())
                .complement(createCompanyAddressRequest.complement())
                .latitude(createCompanyAddressRequest.latitude() != null ? createCompanyAddressRequest.latitude().doubleValue() : null)
                .longitude(createCompanyAddressRequest.longitude() != null ? createCompanyAddressRequest.longitude().doubleValue() : null)
                .build();

        Long addressId = createCompanyAddressUseCase.execute(dto);

        return CreateResponse.builder()
                .data(Create.builder().id(addressId).build())
                .build();
    }

    @Override
    public GetAllCompanyAddressResponse getAllCompanyAddress(PaginationFilter paginationFilter, Long companyId,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting all addresses for company: {}", companyId);

        ListCompanyAddressesDTO listDto = ListCompanyAddressesDTO.builder()
                .companyId(companyId)
                .pageable(createPageable(paginationFilter, CompanyOrder.ID))
                .build();

        Page<CompanyAddressDTO> addressPage = listCompanyAddressesUseCase.execute(listDto);

        return GetAllCompanyAddressResponse.builder()
                .data(
                        addressPage.getContent().stream().map(a ->
                                CompanyAddresses.builder()
                                        .id(a.getId())
                                        .type(a.getType())
                                        .number(BigDecimal.valueOf(a.getNumber()))
                                        .complement(a.getComplement())
                                        .build()
                        ).toList()
                )
                .paginatedDTO(createScosPaginated(addressPage))
                .build();
    }

    @Override
    public GetCompanyAddressResponse getCompanyAddressById(Long companyId, Long id,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Getting address {} for company: {}", id, companyId);

        CompanyAddressDTO dto = getCompanyAddressByIdUseCase.execute(
                GetCompanyAddressDTO.builder().companyId(companyId).addressId(id).build()
        );

        return GetCompanyAddressResponse.builder()
                .data(CompanyAddress.builder()
                        .id(dto.getId())
                        .addressId(dto.getAddressId())
                        .type(dto.getType())
                        .number(BigDecimal.valueOf(dto.getNumber()))
                        .complement(dto.getComplement())
                        .latitude(dto.getLatitude() != null ? BigDecimal.valueOf(dto.getLatitude()) : null)
                        .longitude(dto.getLongitude() != null ? BigDecimal.valueOf(dto.getLongitude()) : null)
                        .build())
                .build();
    }

    @Override
    public Void updateCompanyAddress(Long companyId, Long id, UpdateCompanyAddressRequest updateCompanyAddressRequest,
            Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Updating address {} for company: {}", id, companyId);

        UpdateCompanyAddressDTO dto = UpdateCompanyAddressDTO.builder()
                .companyId(companyId)
                .companyAddressId(id)
                .type(updateCompanyAddressRequest.type())
                .number(updateCompanyAddressRequest.number())
                .complement(updateCompanyAddressRequest.complement())
                .latitude(updateCompanyAddressRequest.latitude() != null ? updateCompanyAddressRequest.latitude().doubleValue() : null)
                .longitude(updateCompanyAddressRequest.longitude() != null ? updateCompanyAddressRequest.longitude().doubleValue() : null)
                .build();

        updateCompanyAddressUseCase.execute(dto);

        return null;
    }

    @Override
    public Void deleteCompanyAddress(Long companyId, Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        log.info("Deleting address {} for company: {}", id, companyId);

        deleteCompanyAddressUseCase.execute(
                DeleteCompanyAddressDTO.builder().companyId(companyId).addressId(id).build()
        );

        return null;
    }
}
