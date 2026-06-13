
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
import br.com.sawcunhaos.organization.application.dto.configuration.ConfigurationDTO;
import br.com.sawcunhaos.organization.application.port.in.configuration.GetConfigurationUseCase;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import br.com.sawcunhaos.organization.domain.repository.configuration.PartnersConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GetConfigurationUseCaseImpl implements GetConfigurationUseCase {

    private static final Set<String> SENSITIVE_SUFFIXES = Set.of("_PASSWORD", "_SECRET", "_TOKEN", "_KEY");

    private final PartnersConfigurationRepository repository;

    @Override
    public ConfigurationDTO execute(String id) {
        PartnersConfiguration config = repository.findById(id)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_CONFIGURATION_001));

        String value = isSensitive(config.getId()) ? "****" : config.getValue();
        return new ConfigurationDTO(config.getId(), value, config.getType());
    }

    private boolean isSensitive(String id) {
        if (id == null) return false;
        String upper = id.toUpperCase();
        return SENSITIVE_SUFFIXES.stream().anyMatch(upper::endsWith);
    }
}
