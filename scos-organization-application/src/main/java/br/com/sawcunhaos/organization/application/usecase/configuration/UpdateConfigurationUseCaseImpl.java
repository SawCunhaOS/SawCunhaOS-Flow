
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.port.in.configuration.UpdateConfigurationUseCase;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import br.com.sawcunhaos.organization.domain.repository.configuration.PartnersConfigurationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UpdateConfigurationUseCaseImpl implements UpdateConfigurationUseCase {

    private final PartnersConfigurationRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(String id, String value) {
        PartnersConfiguration config = repository.findById(id)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_CONFIGURATION_001));

        validateValueForType(config.getType(), value);

        config.setValue(value);
        repository.merge(config);
    }

    private void validateValueForType(String type, String value) {
        if (type == null) return;
        switch (type.toUpperCase()) {
            case "INTEGER" -> {
                try { Long.parseLong(value); }
                catch (NumberFormatException e) {
                    throw new ScosException(ExceptionCodeError.SCOS_CONFIGURATION_002);
                }
            }
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new ScosException(ExceptionCodeError.SCOS_CONFIGURATION_002);
                }
            }
            case "JSON" -> {
                try { objectMapper.readTree(value); }
                catch (Exception e) {
                    throw new ScosException(ExceptionCodeError.SCOS_CONFIGURATION_002);
                }
            }
            case "STRING" -> { /* qualquer valor é válido */ }
        }
    }
}
