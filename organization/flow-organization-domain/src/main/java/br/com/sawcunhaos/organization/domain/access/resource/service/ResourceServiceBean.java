
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

package br.com.sawcunhaos.organization.domain.access.resource.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.resource.dto.RegisterResourceInput;
import br.com.sawcunhaos.organization.domain.access.resource.specification.ResourceService;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.resource.internal.ResourceRepository;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class ResourceServiceBean implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ScosSystemRepository scosSystemRepository;

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void register(@NonNull RegisterResourceInput registerResourceInput) {
        log.info("Registry Resource : {} - System: {}", registerResourceInput.code(), registerResourceInput.systemCode());

        ScosSystem scosSystem = scosSystemRepository.findByCode(registerResourceInput.systemCode())
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_SYSTEM_001, registerResourceInput.systemCode()));

        resourceRepository.upsert(
                scosSystem.getId(),
                registerResourceInput.code(),
                registerResourceInput.descriptionPt(),
                registerResourceInput.descriptionEn(),
                registerResourceInput.group(),
                registerResourceInput.subGroup(),
                registerResourceInput.version(),
                registerResourceInput.updatedAt(),
                registerResourceInput.active(),
                registerResourceInput.systemCode()
        );

        log.info("Registry Resource finished: {} - System: {}", registerResourceInput.code(), registerResourceInput.systemCode());
    }
}
