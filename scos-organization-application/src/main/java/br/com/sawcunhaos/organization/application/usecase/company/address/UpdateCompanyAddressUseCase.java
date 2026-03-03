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
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyAddressDTO;
import br.com.sawcunhaos.organization.application.mapper.company.CompanyAddressMapper;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
import br.com.sawcunhaos.organization.domain.service.company.CompanyDomainService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_COMPANY_006;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class UpdateCompanyAddressUseCase implements ScosBaseUseCase<UpdateCompanyAddressDTO, Void> {

    private final CompanyAddressRepository companyAddressRepository;
    private final CompanyDomainService companyDomainService;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Void execute(UpdateCompanyAddressDTO updateCompanyAddressDTO) {
        log.info("Updating address {} for company: {}", updateCompanyAddressDTO.getCompanyAddressId(), updateCompanyAddressDTO.getCompanyId());

        companyDomainService.validateCompanyAddressExistsValidation(
                updateCompanyAddressDTO.getCompanyId(), updateCompanyAddressDTO.getCompanyAddressId()
        );

        CompanyAddress address = companyAddressRepository
                .findCompanyContactByCompany(updateCompanyAddressDTO.getCompanyId(), updateCompanyAddressDTO.getCompanyAddressId())
                .orElseThrow(() -> new ScosException(SCOS_COMPANY_006));

        address.setType(updateCompanyAddressDTO.getType());
        address.setNumber(Long.parseLong(updateCompanyAddressDTO.getNumber()));
        address.setComplement(updateCompanyAddressDTO.getComplement());

        if (updateCompanyAddressDTO.getLatitude() != null && updateCompanyAddressDTO.getLongitude() != null) {
            GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
            address.setGeolocation(factory.createPoint(
                    new Coordinate(updateCompanyAddressDTO.getLongitude(), updateCompanyAddressDTO.getLatitude())
            ));
        } else {
            address.setGeolocation(null);
        }

        address.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return null;
    }
}
