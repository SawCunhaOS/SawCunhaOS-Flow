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
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_COMPANY_001;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetCompanyByIdUseCase implements ScosBaseUseCase<Long, CompanyDTO> {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyDTO execute(@NonNull Long companyId) {
        log.info("Getting company by id: {}", companyId);

        companyDomainService.validateCompanyExistsValidation(companyId);

        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_001));

        return companyMapper.toCompanyDTO(company);
    }
}
