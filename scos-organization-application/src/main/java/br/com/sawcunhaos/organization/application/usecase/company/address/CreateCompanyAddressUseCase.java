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

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.dto.CompanyAddressDTO;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Criar endereço de empresa
 *
 * POST /v1/companies/{companyId}/addresses
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateCompanyAddressUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyAddressRepository companyAddressRepository;

    public void execute(Long companyId, CompanyAddressDTO addressDTO) {
        log.info("Creating company address for company: {}", companyId);

        // 1. Validar que company existe
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        // 3. Criar endereço
        CompanyAddress address = CompanyAddress.builder()
                .company(company)
                .type(addressDTO.getType())
                .number(addressDTO.getNumber())
                .complement(addressDTO.getComplement())
                .build();

        // 4. Persistir
        companyAddressRepository.persist(address);

        log.info("Company address created successfully for company: {}", companyId);
    }
}

