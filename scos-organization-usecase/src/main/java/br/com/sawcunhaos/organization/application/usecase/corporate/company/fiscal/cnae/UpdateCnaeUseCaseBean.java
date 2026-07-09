
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateCnaeRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeInput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link UpdateCnaeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class UpdateCnaeUseCaseBean implements UpdateCnaeUseCase {

    private final CnaeService cnaeService;

    @Override
    public void execute(@NonNull Long id, @NonNull UpdateCnaeRequest updateCnaeRequest) {
        log.info("Update cnae: {}", id);

        CnaeInput cnaeInput = CnaeInput.builder()
                .id(id)
                .code(updateCnaeRequest.code())
                .description(updateCnaeRequest.description())
                .build();

        cnaeService.update(cnaeInput);
    }
}
