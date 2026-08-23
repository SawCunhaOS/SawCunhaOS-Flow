
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

package br.com.sawcunhaos.organization.application.usecase.access.resource.registry;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.resource.dto.RegisterResourceInput;
import br.com.sawcunhaos.organization.domain.access.resource.specification.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class RegistryResourcesUseCaseBean implements RegistryResourcesUseCase {

    private final ResourceService resourceService;

    @Override
    public void execute(List<RegistryResourceInput> requests) {
        String systemCode = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                .getPrincipal().toString();

        log.info("Registry Resources batch: {} - System: {}", requests.size(), systemCode);

        requests.forEach(request -> resourceService.register(
                RegisterResourceInput.builder()
                        .code(request.code())
                        .descriptionPt(request.descriptionPt())
                        .descriptionEn(request.descriptionEn())
                        .group(request.group())
                        .subGroup(request.subGroup())
                        .version(request.version())
                        .updatedAt(request.updatedAt())
                        .active(request.active())
                        .systemCode(systemCode)
                        .build()
        ));

        log.info("Registry Resources batch finished: {} - System: {}", requests.size(), systemCode);
    }
}
