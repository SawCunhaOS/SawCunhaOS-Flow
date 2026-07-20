
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

package br.com.sawcunhaos.organization.domain.corporate.company.dto;

import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Projeção de leitura de uma {@code Company} — usada nas respostas dos Use Cases.
 */
@Builder
public record CompanyOutput(
        Long id,
        String name,
        String nameTreatment,
        String taxIdentifier,
        LocalDate foundationDate,
        String sectorOfActivity,
        String observation,
        StatusCompany status,
        ParentCompanyOutput parentCompany,
        Long legalNatureId,
        Long cnaePrincipalId,
        String stateRegistration,
        String municipalRegistration
) {
}
