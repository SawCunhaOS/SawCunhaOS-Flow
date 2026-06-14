
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

package br.com.sawcunhaos.organization.domain.service.system.registry;

import br.com.sawcunhaos.organization.domain.model.system.ScosSystem;
import br.com.sawcunhaos.organization.domain.repository.system.ScosSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
class RegistrySystemServiceBean implements RegistrySystemService {

    private final ScosSystemRepository scosSystemRepository;

    @Override
    public ScosSystem register(@NonNull String name, @NonNull String code, @NonNull String description) {
        AtomicReference<ScosSystem> scosSystemResponse = new AtomicReference<>();
        scosSystemRepository.findByCode(code).ifPresentOrElse(
                scosSystem -> {
                    scosSystem.setCode(code);
                    scosSystem.setDescription(description);
                    scosSystem.setName(name);
                    scosSystemResponse.set(scosSystemRepository.merge(scosSystem));
                },
                () -> {
                    ScosSystem scosSystem = ScosSystem.builder().build();
                    scosSystem.setCode(code);
                    scosSystem.setDescription(description);
                    scosSystem.setName(name);
                    scosSystemResponse.set(scosSystemRepository.update(scosSystem));
                }
        );
        return scosSystemResponse.get();
    }
}
