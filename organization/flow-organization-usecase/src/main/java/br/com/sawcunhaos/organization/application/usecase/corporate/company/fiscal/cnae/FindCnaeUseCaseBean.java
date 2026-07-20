
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
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindCnaeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindCnaeUseCaseBean implements FindCnaeUseCase {

    private final CnaeService cnaeService;

    @Override
    public Cnae execute(@NonNull Long id) {
        log.info("Find cnae by id: {}", id);
        return CnaeApiMapper.toApiCnae(cnaeService.findById(id));
    }
}
