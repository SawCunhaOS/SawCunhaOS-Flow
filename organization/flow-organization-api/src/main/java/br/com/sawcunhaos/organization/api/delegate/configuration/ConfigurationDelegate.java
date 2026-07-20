
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

package br.com.sawcunhaos.organization.api.delegate.configuration;


import br.com.sawcunhaos.organization.api.controller.ConfigurationApiDelegate;
import br.com.sawcunhaos.organization.api.dto.GetAllConfigurationsResponse;
import br.com.sawcunhaos.organization.api.dto.GetAllKeysConfigurationsResponse;
import br.com.sawcunhaos.organization.api.dto.GetConfigurationResponse;
import br.com.sawcunhaos.organization.api.dto.UpdateConfigurationRequest;
import br.com.sawcunhaos.organization.application.usecase.configuration.FindAllConfigurationUseCase;
import br.com.sawcunhaos.organization.application.usecase.configuration.FindAllKeysConfigurationUseCase;
import br.com.sawcunhaos.organization.application.usecase.configuration.FindConfigurationByKeyUseCase;
import br.com.sawcunhaos.organization.application.usecase.configuration.UpdateConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationDelegate implements ConfigurationApiDelegate {

    private final FindAllKeysConfigurationUseCase findAllKeysConfigurationUseCase;
    private final FindAllConfigurationUseCase findAllConfigurationUseCase;
    private final UpdateConfigurationUseCase updateConfigurationUseCase;
    private final FindConfigurationByKeyUseCase findConfigurationByKeyUseCase;

    @Override
    public GetAllConfigurationsResponse getAllConfigurations(Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetAllConfigurationsResponse.builder()
                .data(findAllConfigurationUseCase.execute())
                .build();
    }

    @Override
    public GetAllKeysConfigurationsResponse getAllKeysConfigurations(Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetAllKeysConfigurationsResponse.builder()
                .data(findAllKeysConfigurationUseCase.execute())
                .build();
    }

    @Override
    public GetConfigurationResponse getConfigurationById(String id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return GetConfigurationResponse.builder()
                .data(findConfigurationByKeyUseCase.execute(id))
                .build();
    }

    @Override
    public Void updateConfiguration(String id, UpdateConfigurationRequest updateConfigurationRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        updateConfigurationUseCase.execute(id, updateConfigurationRequest.value());

        return null;
    }
}
