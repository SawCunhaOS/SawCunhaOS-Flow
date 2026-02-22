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

/**
 * UseCase: Listar todas as empresas com paginação
 *
 * Fluxo:
 * 1. Buscar empresas não deletadas com paginação
 * 2. Converter para DTOs
 * 3. Retornar página com empresas
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ListCompaniesUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public Page<CompanyDTO> execute(Pageable pageable) {
        log.info("Listing companies with pagination: page={}, size={}",
            pageable.getPageNumber(),
            pageable.getPageSize());

        Page<Company> companies = companyRepository.findAllNotDeleted(pageable);

        return companies.map(companyMapper::toCompanyDTO);
    }
}

