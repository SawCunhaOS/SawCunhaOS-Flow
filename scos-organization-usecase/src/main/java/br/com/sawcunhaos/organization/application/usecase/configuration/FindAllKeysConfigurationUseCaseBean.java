
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
import br.com.sawcunhaos.organization.api.dto.KeyConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllKeysConfigurationUseCaseBean implements FindAllKeysConfigurationUseCase {

    private final ConfigurationService configurationService;

    @Override
    public List<KeyConfiguration> execute() {
        log.info("Find All Keys Configuration");

        return configurationService.getAllKeys().stream()
                .map(keyConfigurationOutput ->
                        KeyConfiguration.builder()
                                .id(keyConfigurationOutput.key())
                                .description(keyConfigurationOutput.description())
                                .type(
                                        ConfigurationType.fromValue(keyConfigurationOutput.type())
                                )
                                .build()
                ).toList();
    }
}
