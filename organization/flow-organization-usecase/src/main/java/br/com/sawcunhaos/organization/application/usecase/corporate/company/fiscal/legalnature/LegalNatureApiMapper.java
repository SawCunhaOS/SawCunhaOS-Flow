
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature;

import br.com.sawcunhaos.organization.api.dto.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;

/**
 * Conversão entre o dto de domínio {@link LegalNatureOutput} e o dto {@link LegalNature} gerado do contrato OpenAPI.
 */
final class LegalNatureApiMapper {

    private LegalNatureApiMapper() {
    }

    /** Monta o {@code LegalNature} do contrato a partir da saída do domínio. */
    static LegalNature toApiLegalNature(LegalNatureOutput legalNatureOutput) {
        return LegalNature.builder()
                .id(legalNatureOutput.id())
                .code(legalNatureOutput.code())
                .description(legalNatureOutput.description())
                .build();
    }
}
