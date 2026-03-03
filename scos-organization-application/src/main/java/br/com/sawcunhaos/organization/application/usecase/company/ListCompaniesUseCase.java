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

package br.com.sawcunhaos.organization.application.usecase.company;

import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListCompaniesUseCase implements ScosBaseUseCase<Pageable, Page<CompanyDTO>> {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public Page<CompanyDTO> execute(Pageable pageable) {
        log.info("Listing companies with pagination: {}", pageable);

        Page<Company> companiesPage = companyRepository.findAllNotDeleted(pageable);
        return companiesPage.map(companyMapper::toCompanyDTO);
    }
}
