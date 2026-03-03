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

package br.com.sawcunhaos.organization.application.usecase.company.address;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.CompanyAddressDTO;
import br.com.sawcunhaos.organization.application.dto.GetCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyAddressMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
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
public class GetCompanyAddressByIdUseCase implements ScosBaseUseCase<GetCompanyAddressDTO, CompanyAddressDTO> {

    private final CompanyAddressRepository companyAddressRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyAddressMapper companyAddressMapper;

    @Override
    public CompanyAddressDTO execute(GetCompanyAddressDTO getCompanyAddressDTO) {
        log.info("Getting address {} for company: {}", getCompanyAddressDTO.getAddressId(), getCompanyAddressDTO.getCompanyId());

        companyDomainService.validateCompanyAddressExistsValidation(
                getCompanyAddressDTO.getCompanyId(), getCompanyAddressDTO.getAddressId()
        );

        CompanyAddress companyAddress = companyAddressRepository
                .findCompanyContactByCompany(getCompanyAddressDTO.getCompanyId(), getCompanyAddressDTO.getAddressId())
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_006));

        return companyAddressMapper.toCompanyAddressDTO(companyAddress);
    }
}
