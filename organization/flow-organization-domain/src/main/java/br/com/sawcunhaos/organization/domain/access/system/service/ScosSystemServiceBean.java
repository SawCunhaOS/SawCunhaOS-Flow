
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
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import br.com.sawcunhaos.organization.domain.access.system.specification.ScosSystemService;
import br.com.sawcunhaos.organization.shared.utils.SystemSecretCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Objects;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_002;

@Service
@RequiredArgsConstructor
@Slf4j
class ScosSystemServiceBean implements ScosSystemService {

    private final ScosSystemRepository scosSystemRepository;
    private final SystemSecretCryptoService systemSecretCryptoService;
    private final Clock clock;
    private static final SecureRandom RNG = new SecureRandom();

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
    public ScosSystemOutput findByCode(@NonNull String code) {
        ScosSystem scosSystem = getByCode(code);
        return ScosSystemOutput.builder()
                .id(scosSystem.getId())
                .name(scosSystem.getName())
                .description(scosSystem.getDescription())
                .code(scosSystem.getCode())
                .status(scosSystem.getStatus())
                .version(scosSystem.getVersion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateSecretKey(@NonNull String code, @NonNull String secretKey) {
        ScosSystem scosSystem = getByCode(code);

        if (!scosSystem.matchesSecret(secretKey, clock.instant())) {
            throw new ScosException(SCOS_SYSTEM_002);
        }
    }

    private ScosSystem getByCode(@NonNull String code) {
        return scosSystemRepository.findByCode(code).orElseThrow(
                () -> new ScosException(SCOS_SYSTEM_001)
        );
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
        scosSystem.setSecretKey(
                systemSecretCryptoService.encrypt(
                        generateRawSecret()
                )
        );
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

    private String generateRawSecret() {
        byte[] b = new byte[32];                                      // 256 bits
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
