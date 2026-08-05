
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

package br.com.sawcunhaos.organization.domain.corporate.employee.dto;

import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeContractType;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Dados de entrada para admitir um {@code Employee}.
 */
@Builder
public record EmployeeInput(
        String name,
        String nameTreatment,
        String taxIdentifier,
        String email,
        LocalDate birthDate,
        String observation,
        LocalDate dateOfHiring,
        EmployeeContractType contractType,
        LocalDate probationEndDate,
        Long supervisorId,
        Long companyId,
        Long positionId,
        Long reasonActivateId
) {
}
