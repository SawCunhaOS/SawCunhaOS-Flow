
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

package br.com.sawcunhaos.organization.domain.corporate.position.dto;

import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import lombok.Builder;

@Builder
public record PositionOutput(
        Long id,
        String code,
        String description,
        boolean active,
        boolean isTrustPosition,
        DepartmentOutput department
) {
}
