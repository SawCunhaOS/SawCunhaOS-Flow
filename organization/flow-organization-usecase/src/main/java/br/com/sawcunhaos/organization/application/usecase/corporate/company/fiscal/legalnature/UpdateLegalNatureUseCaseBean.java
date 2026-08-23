
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateLegalNatureRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateLegalNatureUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateLegalNatureUseCaseBean implements UpdateLegalNatureUseCase {

    private final LegalNatureService legalNatureService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateLegalNatureRequest updateLegalNatureRequest) {
        log.info("Update legal nature: {}", id);

        LegalNatureInput legalNatureInput = LegalNatureInput.builder()
                .id(id)
                .code(updateLegalNatureRequest.code())
                .description(updateLegalNatureRequest.description())
                .build();

        legalNatureService.update(legalNatureInput);
    }
}
