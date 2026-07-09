
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CreateLegalNatureRequest;
import br.com.sawcunhaos.organization.api.dto.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link CreateLegalNatureUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class CreateLegalNatureUseCaseBean implements CreateLegalNatureUseCase {

    private final LegalNatureService legalNatureService;

    @Override
    public LegalNature execute(@NonNull CreateLegalNatureRequest createLegalNatureRequest) {
        log.info("Create legal nature: {}", createLegalNatureRequest.code());

        LegalNatureInput legalNatureInput = LegalNatureInput.builder()
                .code(createLegalNatureRequest.code())
                .description(createLegalNatureRequest.description())
                .build();

        LegalNatureOutput legalNatureOutput = legalNatureService.create(legalNatureInput);
        return LegalNatureApiMapper.toApiLegalNature(legalNatureOutput);
    }
}
