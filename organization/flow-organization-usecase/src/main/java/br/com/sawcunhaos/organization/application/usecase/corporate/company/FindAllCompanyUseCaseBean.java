
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

import br.com.sawcunhaos.organization.api.dto.GetAllCompaniesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.StatusCompany;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindAllCompanyUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllCompanyUseCaseBean implements FindAllCompanyUseCase {

    private final CompanyService companyService;

    @Override
    public GetAllCompaniesResponse execute(@NonNull PaginationFilter paginationFilter, StatusCompany status, String name) {
        log.info("Find All Companies, Page: {}, Size: {}, Status: {}, Name: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                status,
                name
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<CompanyOutput> companyOutput = companyService.findAll(
                CompanyApiMapper.toDomainStatus(status),
                name,
                pageable
        );

        return GetAllCompaniesResponse.builder()
                .data(
                        companyOutput.getContent().stream()
                                .map(CompanyApiMapper::toApiCompanies)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(companyOutput)
                )
                .build();
    }
}
