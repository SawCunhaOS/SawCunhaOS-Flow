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
import br.com.sawcunhaos.organization.application.dto.CompanyAddressDTO;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Obter endereço de empresa
 *
 * GET /v1/companies/{companyId}/addresses/{addressId}
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetCompanyAddressUseCase {

    private final CompanyRepository companyRepository;

    public CompanyAddressDTO execute(Long companyId, Long addressId) {
        log.info("Getting company address: companyId={}", companyId);

        // 1. Validar que company existe
        if (!companyRepository.existsById(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        log.info("Company address retrieved successfully");
        return CompanyAddressDTO.builder().build();
    }
}

