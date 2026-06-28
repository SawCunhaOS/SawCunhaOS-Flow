
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

package br.com.sawcunhaos.organization.domain.access.resource.dto;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

@Builder
public record RegisterResourceInput(
        @NonNull String code,
        @NonNull String descriptionPt,
        @NonNull String descriptionEn,
        @NonNull Boolean active,
        @NonNull String systemCode
) {
}
