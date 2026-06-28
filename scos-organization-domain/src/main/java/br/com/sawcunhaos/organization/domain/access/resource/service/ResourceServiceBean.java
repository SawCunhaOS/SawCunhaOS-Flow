
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

import br.com.sawcunhaos.organization.domain.access.resource.dto.RegisterResourceInput;
import br.com.sawcunhaos.organization.domain.access.resource.specification.ResourceService;
import br.com.sawcunhaos.organization.domain.access.resource.internal.Resource;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.resource.internal.ResourceRepository;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
class ResourceServiceBean implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ScosSystemRepository scosSystemRepository;

    @Override
    public void register(@NonNull RegisterResourceInput registerResourceInput) {
        log.info("Registry Resource : {} - System: {}", registerResourceInput.code(), registerResourceInput.systemCode());
        ScosSystem scosSystem = scosSystemRepository.findByCode(registerResourceInput.systemCode()).get();

        Optional<Resource> id = resourceRepository.findIdByCodeAndSystemCode(
                registerResourceInput.code(),
                registerResourceInput.systemCode()
        );

        if (id.isEmpty()) {
            Resource resource = Resource.builder()
                    .code(registerResourceInput.code())
                    .descriptionEn(registerResourceInput.descriptionEn())
                    .descriptionPt(registerResourceInput.descriptionPt())
                    .active(registerResourceInput.active())
                    .system(scosSystem)
                    .build();
            resource.updateAuditInfo(registerResourceInput.systemCode());
            resourceRepository.merge(resource);
        } else {
            Resource resource = id.get();
            resource.setDescriptionEn(registerResourceInput.descriptionEn());
            resource.setDescriptionPt(registerResourceInput.descriptionPt());
            resource.setActive(registerResourceInput.active());
            resource.updateAuditInfo(registerResourceInput.systemCode());
            resourceRepository.update(resource);
        }
        log.info("Registry Resource finished: {} - System: {}", registerResourceInput.code(), registerResourceInput.systemCode());
    }
}
