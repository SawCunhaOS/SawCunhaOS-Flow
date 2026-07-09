
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

package br.com.sawcunhaos.organization.application.usecase.configuration;

import br.com.sawcunhaos.organization.api.dto.ConfigurationType;
import br.com.sawcunhaos.organization.api.dto.ModelConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.dto.ConfigurationOutput;
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FindConfigurationByKeyUseCaseBean implements FindConfigurationByKeyUseCase {
    private final ConfigurationService configurationService;

    @Override
    public ModelConfiguration execute(@NonNull String key) {
        log.info("Find Configuration By Key : {}", key);

        ConfigurationOutput configurationOutput = configurationService.getConfiguration(key);

        return ModelConfiguration.builder()
                .id(configurationOutput.key())
                .description(configurationOutput.description())
                .value(configurationOutput.value())
                .type(
                        ConfigurationType.valueOf(configurationOutput.type())
                ).build();
    }
}
