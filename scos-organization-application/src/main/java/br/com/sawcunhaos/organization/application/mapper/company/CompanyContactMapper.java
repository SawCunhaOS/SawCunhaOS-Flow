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

import br.com.sawcunhaos.organization.application.dto.CompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyContactDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyContactDTO;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.CompanyContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", expression = "java(mapCompanyFromId(createCompanyContactDTO.getCompanyId()))")
    @Mapping(target = "email", expression = "java(new br.com.sawcunhaos.foundation.utils.valueobjects.Email(createCompanyContactDTO.getEmail()))")
    CompanyContact toCompanyContact(CreateCompanyContactDTO createCompanyContactDTO);

    @Mapping(target = "id", source = "contactId")
    @Mapping(target = "company", expression = "java(mapCompanyFromId(updateCompanyContactDTO.getCompanyId()))")
    @Mapping(target = "email", expression = "java(new br.com.sawcunhaos.foundation.utils.valueobjects.Email(updateCompanyContactDTO.getEmail()))")
    CompanyContact toCompanyContact(UpdateCompanyContactDTO updateCompanyContactDTO);

    @Mapping(target = "email", source = "email.email")
    CompanyContactDTO toCompanyContactDTO(CompanyContact companyContact);

    default Company mapCompanyFromId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return Company.builder().id(companyId).build();
    }

}
