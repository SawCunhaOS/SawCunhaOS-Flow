
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

package br.com.sawcunhaos.organization.application.usecase.access.system.registry;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.system.dto.RegisterScosSystemInput;
import br.com.sawcunhaos.organization.domain.access.system.dto.ScosSystemOutput;
import br.com.sawcunhaos.organization.domain.access.system.specification.ScosSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class RegistrySystemUseCaseBean implements RegistrySystemUseCase {

    private final ScosSystemService scosystemService;

    @Override
    public RegistrySystemOutput execute(RegistrySystemInput request) {
        log.info("Registry System : {}", request.code());

        ScosSystemOutput scosSystem = scosystemService.register(
                RegisterScosSystemInput.builder()
                        .name(request.name())
                        .description(request.description())
                        .code(request.code())
                        .version(request.version())
                        .build()
        );

        log.info("Registry System finished: {}", scosSystem.code());
        return RegistrySystemOutput.builder()
                .systemId(scosSystem.id().toString())
                .secretKey(scosSystem.secretKey())
                .update(scosSystem.updateRegistration())
                .build();
    }
}
