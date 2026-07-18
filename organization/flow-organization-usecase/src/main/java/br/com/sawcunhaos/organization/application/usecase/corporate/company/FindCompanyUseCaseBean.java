
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company;

import br.com.sawcunhaos.organization.api.dto.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindCompanyUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindCompanyUseCaseBean implements FindCompanyUseCase {

    private final CompanyService companyService;

    @Override
    public Company execute(@NonNull Long id) {
        log.info("Find company by id: {}", id);
        return CompanyApiMapper.toApiCompany(companyService.findById(id));
    }
}
