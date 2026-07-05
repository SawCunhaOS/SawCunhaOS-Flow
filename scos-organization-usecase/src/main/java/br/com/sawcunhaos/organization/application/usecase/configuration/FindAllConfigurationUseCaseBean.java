
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
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindAllConfigurationUseCaseBean implements FindAllConfigurationUseCase {
    private final ConfigurationService configurationService;

    @Override
    public List<ModelConfiguration> execute() {
        log.info("Find All Configuration");
        return configurationService.getAllConfigurations().stream()
                .map( configurationOutput ->
                        ModelConfiguration.builder()
                                .id(configurationOutput.key())
                                .description(configurationOutput.description())
                                .value(null)
                                .type(ConfigurationType.fromValue(configurationOutput.type()))
                                .build()
                ).toList();
    }
}
