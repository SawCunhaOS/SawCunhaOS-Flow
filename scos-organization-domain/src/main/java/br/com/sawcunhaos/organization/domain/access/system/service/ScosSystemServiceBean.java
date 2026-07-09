
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

package br.com.sawcunhaos.organization.domain.access.system.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.system.dto.RegisterScosSystemInput;
import br.com.sawcunhaos.organization.domain.access.system.dto.ScosSystemOutput;
import br.com.sawcunhaos.organization.domain.access.system.specification.ScosSystemService;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class ScosSystemServiceBean implements ScosSystemService {

    private final ScosSystemRepository scosSystemRepository;

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public ScosSystemOutput register(@NonNull RegisterScosSystemInput registerScosSystemInput) {
        log.info("Registry System : {} - Code: {}", registerScosSystemInput.name(), registerScosSystemInput.code());
        ScosSystem scosSystem = scosSystemRepository.findByCode(registerScosSystemInput.code()).orElse(null);

        if (Objects.isNull(scosSystem)) {
            log.info("System not registered: {}", registerScosSystemInput.code());
            scosSystem = createSystem(
                    registerScosSystemInput.name(),
                    registerScosSystemInput.code(),
                    registerScosSystemInput.description(),
                    registerScosSystemInput.version()
            );
        }

        if (!scosSystem.getVersion().equals(registerScosSystemInput.version())) {
            log.info("Update System registered: {} - Version: {}", scosSystem.getCode(), scosSystem.getVersion());
            scosSystem = updateSystem(
                    scosSystem,
                    registerScosSystemInput.name(),
                    registerScosSystemInput.code(),
                    registerScosSystemInput.description(),
                    registerScosSystemInput.version()
            );
        }

        return ScosSystemOutput.builder()
                .id(scosSystem.getId())
                .name(scosSystem.getName())
                .description(scosSystem.getDescription())
                .code(scosSystem.getCode())
                .status(scosSystem.getStatus())
                .secretKey(scosSystem.getSecretKey())
                .version(scosSystem.getVersion())
                .updateRegistration(scosSystem.isUpdateRegistration())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ScosSystemOutput findByCodeAndSecretKey(@NonNull String Code, @NonNull String secretKey) {
        ScosSystem scosSystem = scosSystemRepository.findByCodeAndSecretKey(Code, secretKey).orElseThrow();
        return ScosSystemOutput.builder()
                .id(scosSystem.getId())
                .name(scosSystem.getName())
                .description(scosSystem.getDescription())
                .code(scosSystem.getCode())
                .status(scosSystem.getStatus())
                .secretKey(scosSystem.getSecretKey())
                .version(scosSystem.getVersion())
                .build();
    }

    private ScosSystem createSystem(
            @NonNull String name,
            @NonNull String code,
            @NonNull String description,
            @NonNull String version
    ) {
        ScosSystem scosSystem = ScosSystem.builder().build();
        scosSystem.setCode(code);
        scosSystem.setDescription(description);
        scosSystem.setName(name);
        scosSystem.updateAuditInfo("REGISTRY");
        scosSystem.setSecretKey(UUID.randomUUID().toString());
        scosSystem.setVersion(version);
        scosSystem.setStatus("ACTIVE");
        scosSystem = scosSystemRepository.merge(scosSystem);
        scosSystem.setUpdateRegistration(true);
        return scosSystem;
    }

    private ScosSystem updateSystem(
            @NonNull ScosSystem scosSystem,
            @NonNull String name,
            @NonNull String code,
            @NonNull String description,
            @NonNull String version
    ) {
        scosSystem.setCode(code);
        scosSystem.setDescription(description);
        scosSystem.setName(name);
        scosSystem.setVersion(version);
        scosSystem.updateAuditInfo("REGISTRY");
        scosSystem = scosSystemRepository.update(scosSystem);
        scosSystem.setUpdateRegistration(true);
        return scosSystem;
    }
}
