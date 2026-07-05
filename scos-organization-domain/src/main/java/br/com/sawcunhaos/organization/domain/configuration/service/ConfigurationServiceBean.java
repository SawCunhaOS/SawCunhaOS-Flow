
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

package br.com.sawcunhaos.organization.domain.configuration.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import br.com.sawcunhaos.organization.domain.configuration.dto.ConfigurationOutput;
import br.com.sawcunhaos.organization.domain.configuration.dto.KeyConfigurationOutput;
import br.com.sawcunhaos.organization.domain.configuration.internal.ConfigurationKey;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.internal.OrganizationConfigurationRepository;
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONFIGURATION_002;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationServiceBean implements ConfigurationService {

    private final OrganizationConfigurationRepository organizationConfigurationRepository;
    private final LocaleService localeService;

    @Override
    public List<KeyConfigurationOutput> getAllKeys() {
        log.info("Getting all keys");
        return (List<KeyConfigurationOutput>) Arrays.stream(ConfigurationKey.values())
                .map(configurationKey -> KeyConfigurationOutput.builder()
                        .key(configurationKey.name())
                        .description(localeService.getMessage(configurationKey.name()))
                        .type(configurationKey.getType().name())
                        .build())
                .toList();
    }

    @Override
    public List<ConfigurationOutput> getAllConfigurations() {
        return organizationConfigurationRepository.findAll().stream()
                .map(organizationConfiguration ->
                        ConfigurationOutput.builder()
                                .key(organizationConfiguration.getId().name())
                                .description(localeService.getMessage(organizationConfiguration.getId().name()))
                                .type(organizationConfiguration.getType().name())
                                .value(null)
                                .build()
                )
                .toList();
    }

    @Override
    public ConfigurationOutput getConfiguration(String key) {
        log.info("Getting configuration by key: {}", key);

        OrganizationConfiguration organizationConfiguration = getOrganizationConfiguration(ConfigurationKey.valueOfKey(key));

        return ConfigurationOutput.builder()
                .key(organizationConfiguration.getId().name())
                .description(localeService.getMessage(organizationConfiguration.getId().name()))
                .type(organizationConfiguration.getType().name())
                .value(null)
                .build();
    }

    @Override
    public void updateConfiguration(String key, String value) {
        log.info("Updating configuration by key: {}", key);
        OrganizationConfiguration organizationConfiguration = getOrganizationConfiguration(ConfigurationKey.valueOfKey(key));

        organizationConfiguration.setValue(value);

        organizationConfigurationRepository.merge(organizationConfiguration);
    }

    public OrganizationConfiguration getOrganizationConfiguration(ConfigurationKey configurationKey) {
        log.info("Getting configuration by key: {}", configurationKey.name());

        return organizationConfigurationRepository.findById(
                configurationKey
        ).orElseThrow(() -> new ScosException(SCOS_CONFIGURATION_002));
    }
}
