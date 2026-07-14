
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

/**
 * Projeção resumida da empresa mãe, aninhada em {@link CompanyOutput}.
 */
@Builder
public record ParentCompanyOutput(
        Long id,
        String name,
        String nameTreatment,
        String taxIdentifier
) {
}
