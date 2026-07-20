
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

package br.com.sawcunhaos.organization.application.usecase.access.resource.registry;

import lombok.Builder;

import java.time.Instant;

@Builder
public record RegistryResourceInput(
        String code,
        String descriptionPt,
        String descriptionEn,
        String group,
        String subGroup,
        String version,
        Instant updatedAt,
        boolean active
) {
}
