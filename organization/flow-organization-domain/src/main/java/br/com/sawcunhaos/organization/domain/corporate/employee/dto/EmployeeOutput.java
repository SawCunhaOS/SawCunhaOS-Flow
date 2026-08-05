
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
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Retorno de leitura de {@code Employee} — flat, com ids de FK (sem sub-objetos aninhados).
 */
@Builder
public record EmployeeOutput(
        Long id,
        String name,
        String nameTreatment,
        String taxIdentifier,
        String email,
        LocalDate birthDate,
        String observation,
        LocalDate dateOfHiring,
        EmployeeContractType contractType,
        LocalDate probationEndDate,
        StatusEmployee status,
        Long supervisorId,
        Long companyId,
        Long positionId
) {
}
