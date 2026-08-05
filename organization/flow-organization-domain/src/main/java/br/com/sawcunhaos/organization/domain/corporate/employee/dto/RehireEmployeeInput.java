
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
 * Dados de entrada para recontratar um {@code Employee} INACTIVE, reatribuindo empresa/cargo/supervisor/contrato.
 */
@Builder
public record RehireEmployeeInput(
        String taxIdentifier,
        Long companyId,
        Long positionId,
        Long supervisorId,
        EmployeeContractType contractType,
        LocalDate probationEndDate,
        LocalDate dateOfRehire,
        Long reasonActivateId,
        Long reasonPositionChangeId,
        String observation
) {
}
