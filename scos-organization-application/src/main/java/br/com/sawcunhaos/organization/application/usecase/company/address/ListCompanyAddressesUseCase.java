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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * UseCase: Listar endereços de empresa
 *
 * GET /v1/companies/{companyId}/addresses
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListCompanyAddressesUseCase {

    private final CompanyRepository companyRepository;

    public Page<CompanyAddressDTO> execute(Long companyId, Pageable pageable) {
        log.info("Listing company addresses: companyId={}", companyId);

        // 1. Validar que company existe
        if (!companyRepository.existsById(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        log.info("Company addresses listed successfully");
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
}

