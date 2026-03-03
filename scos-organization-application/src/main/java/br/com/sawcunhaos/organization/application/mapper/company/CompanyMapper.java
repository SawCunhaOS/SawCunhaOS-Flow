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

package br.com.sawcunhaos.organization.application.mapper.company;

import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taxIdentifier", expression = "java(new br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj(createCompanyDTO.getTaxIdentifier()))")
    @Mapping(target = "parentCompany", expression = "java(mapParentCompanyFromId(createCompanyDTO.getParentCompanyId()))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "companyContacts", ignore = true)
    @Mapping(target = "companyAddresses", ignore = true)
    Company toCompany(CreateCompanyDTO createCompanyDTO);

    @Mapping(target = "id", source = "companyId")
    @Mapping(target = "taxIdentifier", ignore = true)
    @Mapping(target = "parentCompany", expression = "java(mapParentCompanyFromId(updateCompanyDTO.getParentCompanyId()))")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "companyContacts", ignore = true)
    @Mapping(target = "companyAddresses", ignore = true)
    Company toCompany(UpdateCompanyDTO updateCompanyDTO);

    @Mapping(target = "taxIdentifier", source = "taxIdentifier.cnpj")
    @Mapping(target = "status", expression = "java(company.getStatus() != null ? company.getStatus().name() : null)")
    @Mapping(target = "parentCompanyId", source = "parentCompany.id")
    @Mapping(target = "parentCompanyName", source = "parentCompany.name")
    @Mapping(target = "parentCompanyNameTreatment", source = "parentCompany.nameTreatment")
    @Mapping(target = "parentCompanyTaxIdentifier", expression = "java(company.getParentCompany() != null ? company.getParentCompany().getTaxIdentifier().getCnpj() : null)")
    CompanyDTO toCompanyDTO(Company company);

    default Company mapParentCompanyFromId(Long parentCompanyId) {
        if (parentCompanyId == null) {
            return null;
        }
        return Company.builder().id(parentCompanyId).build();
    }

}
