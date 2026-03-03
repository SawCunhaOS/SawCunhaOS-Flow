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
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.DeleteCompanyAddressDTO;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class DeleteCompanyAddressUseCase implements ScosBaseUseCase<DeleteCompanyAddressDTO, Void> {

    private final CompanyAddressRepository companyAddressRepository;
    private final CompanyDomainService companyDomainService;

    @Override
    public Void execute(DeleteCompanyAddressDTO deleteCompanyAddressDTO) {
        log.info("Deleting address {} for company: {}", deleteCompanyAddressDTO.getAddressId(), deleteCompanyAddressDTO.getCompanyId());

        companyDomainService.validateCompanyAddressExistsValidation(
                deleteCompanyAddressDTO.getCompanyId(), deleteCompanyAddressDTO.getAddressId()
        );

        companyAddressRepository.deleteByCompanyIdAndId(
                deleteCompanyAddressDTO.getCompanyId(), deleteCompanyAddressDTO.getAddressId()
        );

        log.info("Address deleted: {}", deleteCompanyAddressDTO.getAddressId());
        return null;
    }
}
