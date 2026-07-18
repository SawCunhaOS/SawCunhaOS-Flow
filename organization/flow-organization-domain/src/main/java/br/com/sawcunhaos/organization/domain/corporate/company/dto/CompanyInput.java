
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

import lombok.Builder;

import java.time.LocalDate;

/**
 * Dados de entrada para criar/atualizar uma {@code Company} — {@code id} nulo em criação.
 * Na atualização, {@code parentCompanyId} e {@code reasonActivateId} são ignorados
 * ({@code parentCompanyId} é imutável; {@code reasonActivateId} só vale na criação).
 */
@Builder
public record CompanyInput(
        Long id,
        String name,
        String nameTreatment,
        String taxIdentifier,
        LocalDate foundationDate,
        String sectorOfActivity,
        String observation,
        Long parentCompanyId,
        Long reasonActivateId,
        Long legalNatureId,
        Long cnaePrincipalId,
        String stateRegistration,
        String municipalRegistration
) {
}
