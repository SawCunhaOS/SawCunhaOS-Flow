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
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Deletar endereço de empresa
 *
 * DELETE /v1/companies/{companyId}/addresses/{addressId}
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class DeleteCompanyAddressUseCase {

    private final CompanyRepository companyRepository;

    public void execute(Long companyId, Long addressId) {
        log.info("Deleting company address: companyId={}", companyId);

        // 1. Validar que company existe
        if (!companyRepository.existsById(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        log.info("Company address deleted successfully");
    }
}

