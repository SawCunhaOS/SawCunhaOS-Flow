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

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyMapper;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_COMPANY_001;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdateCompanyUseCase implements ScosBaseUseCase<UpdateCompanyDTO, Void> {

    private final CompanyRepository companyRepository;
    private final CompanyDomainService companyDomainService;
    private final CompanyMapper companyMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Void execute(UpdateCompanyDTO updateCompanyDTO) {
        log.info("Updating company with id: {}", updateCompanyDTO.getCompanyId());

        companyDomainService.validateCompanyExistsValidation(updateCompanyDTO.getCompanyId());

        // Validate parent company if provided
        if (updateCompanyDTO.getParentCompanyId() != null) {
            companyDomainService.validateParentCompanyExistsAndActiveValidation(updateCompanyDTO.getParentCompanyId());
            companyDomainService.validateCycleInHierarchyValidation(
                    updateCompanyDTO.getCompanyId(), updateCompanyDTO.getParentCompanyId()
            );
        }

        Company existingCompany = companyRepository.findNotDeletedById(updateCompanyDTO.getCompanyId())
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_001));

        existingCompany.setName(updateCompanyDTO.getName());
        existingCompany.setNameTreatment(updateCompanyDTO.getNameTreatment());
        existingCompany.setFoundationDate(updateCompanyDTO.getFoundationDate());
        existingCompany.setSectorOfActivity(updateCompanyDTO.getSectorOfActivity());
        existingCompany.setObservation(updateCompanyDTO.getObservation());

        if (updateCompanyDTO.getParentCompanyId() != null) {
            existingCompany.setParentCompany(Company.builder().id(updateCompanyDTO.getParentCompanyId()).build());
        } else {
            existingCompany.setParentCompany(null);
        }

        existingCompany.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return null;
    }
}
