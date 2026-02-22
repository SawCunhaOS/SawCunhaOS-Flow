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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase: Obter detalhes de uma empresa específica
 *
 * Fluxo:
 * 1. Buscar empresa por ID
 * 2. Retornar DTO com dados completos
 *
 * Erros esperados:
 * - SCOS_COMPANY_001: Company não encontrada
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetCompanyByIdUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyDTO execute(Long companyId) {
        log.info("Getting company by id: {}", companyId);

        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));

        log.info("Company found: id={}, name={}", company.getId(), company.getName());
        return companyMapper.toCompanyDTO(company);
    }
}

