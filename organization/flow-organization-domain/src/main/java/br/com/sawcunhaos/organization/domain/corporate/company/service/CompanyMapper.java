
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

package br.com.sawcunhaos.organization.domain.corporate.company.service;

import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.ParentCompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct: {@link Company} (entidade JPA) → {@link CompanyOutput} (dto de domínio).
 * O CNPJ é desembrulhado do value object {@code Cnpj}; os FKs opcionais viram ids (null-safe).
 */
@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "taxIdentifier", source = "taxIdentifier.cnpj")
    @Mapping(target = "legalNatureId", source = "legalNature.id")
    @Mapping(target = "cnaePrincipalId", source = "cnaePrincipal.id")
    @Mapping(target = "parentCompany", source = "parentCompany")
    CompanyOutput toCompanyOutput(Company company);

    @Mapping(target = "taxIdentifier", source = "taxIdentifier.cnpj")
    ParentCompanyOutput toParentCompanyOutput(Company company);

}
