
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

package br.com.sawcunhaos.organization.domain.access.status.dto;

import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import lombok.Builder;

/**
 * Projeção de leitura de um {@code ReasonDisable} — usada nas respostas dos Use Cases.
 */
@Builder
public record ReasonDisableOutput(
        Long id,
        String code,
        String description,
        EntityType entityType,
        boolean active
) {
}
