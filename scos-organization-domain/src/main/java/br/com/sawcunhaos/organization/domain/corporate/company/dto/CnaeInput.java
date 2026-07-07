
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
 * Dados de entrada para criar/atualizar um {@code Cnae} — {@code id} nulo em criação.
 */
@Builder
public record CnaeInput(
        Long id,
        String code,
        String description
) {
}
