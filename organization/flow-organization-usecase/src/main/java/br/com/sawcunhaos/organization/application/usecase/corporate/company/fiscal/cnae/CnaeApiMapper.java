
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae;

import br.com.sawcunhaos.organization.api.dto.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;

/**
 * Conversão entre o dto de domínio {@link CnaeOutput} e o dto {@link Cnae} gerado do contrato OpenAPI.
 */
final class CnaeApiMapper {

    private CnaeApiMapper() {
    }

    /** Monta o {@code Cnae} do contrato a partir da saída do domínio. */
    static Cnae toApiCnae(CnaeOutput cnaeOutput) {
        return Cnae.builder()
                .id(cnaeOutput.id())
                .code(cnaeOutput.code())
                .description(cnaeOutput.description())
                .build();
    }
}
