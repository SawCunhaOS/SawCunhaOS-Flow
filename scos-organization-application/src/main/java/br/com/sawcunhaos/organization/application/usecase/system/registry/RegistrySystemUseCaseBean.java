
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

package br.com.sawcunhaos.organization.application.usecase.system.registry;

import br.com.sawcunhaos.organization.domain.model.system.ScosSystem;
import br.com.sawcunhaos.organization.domain.service.system.registry.RegistrySystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class RegistrySystemUseCaseBean implements RegistrySystemUseCase {

    private final RegistrySystemService registrySystemService;

    @Override
    public RegistrySystemOutput execute(RegistrySystemInput request) {
        log.info("Registry System : {}", request.code());

        ScosSystem scosSystem = registrySystemService.register(request.name(), request.code(), request.description());

        log.info("Registry System finished: {}", scosSystem.getCode());
        return RegistrySystemOutput.builder()
                .systemId(scosSystem.getId().toString())
                .secretKey(scosSystem.getSecretKey())
                .build();
    }
}
