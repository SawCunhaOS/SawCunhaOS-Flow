
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

package br.com.sawcunhaos.organization.api.delegate;

import br.com.sawcunhaos.foundation.utils.sort.PropertiesOrder;
import br.com.sawcunhaos.organization.api.controller.ConfigurationApiDelegate;
import br.com.sawcunhaos.organization.api.dto.ConfigurationType;
import br.com.sawcunhaos.organization.api.dto.GetAllConfigurationsResponse;
import br.com.sawcunhaos.organization.api.dto.GetConfigurationResponse;
import br.com.sawcunhaos.organization.api.dto.ModelConfiguration;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.UpdateConfigurationRequest;
import br.com.sawcunhaos.organization.api.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.application.dto.configuration.ConfigurationDTO;
import br.com.sawcunhaos.organization.application.port.in.configuration.GetConfigurationUseCase;
import br.com.sawcunhaos.organization.application.port.in.configuration.ListConfigurationsUseCase;
import br.com.sawcunhaos.organization.application.port.in.configuration.UpdateConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfigurationApiDelegateImpl implements ConfigurationApiDelegate {

    private static final PropertiesOrder CONFIGURATION_ORDER = new PropertiesOrder() {
        @Override public String properties() { return "id,value,type"; }
        @Override public String value(String name) {
            return switch (name == null ? "" : name) {
                case "value" -> "value";
                case "type" -> "type";
                default -> "id";
            };
        }
    };

    private final ListConfigurationsUseCase listConfigurationsUseCase;
    private final GetConfigurationUseCase getConfigurationUseCase;
    private final UpdateConfigurationUseCase updateConfigurationUseCase;

    @Override
    public GetAllConfigurationsResponse getAllConfigurations(PaginationFilter paginationFilter,
                                                            Optional<UUID> xRequestID,
                                                            Optional<String> acceptLanguage) {
        Pageable pageable = PaginatioUtils.createPageable(paginationFilter, CONFIGURATION_ORDER);
        Page<ConfigurationDTO> page = listConfigurationsUseCase.execute(pageable);

        List<ModelConfiguration> data = page.getContent()
                .stream()
                .map(this::toModelConfiguration)
                .toList();

        return GetAllConfigurationsResponse.builder()
                .data(data)
                .paginatedDTO(PaginatioUtils.createScosPaginated(page))
                .build();
    }

    @Override
    public GetConfigurationResponse getConfigurationById(String id,
                                                         Optional<UUID> xRequestID,
                                                         Optional<String> acceptLanguage) {
        ConfigurationDTO dto = getConfigurationUseCase.execute(id);
        return GetConfigurationResponse.builder()
                .data(toModelConfiguration(dto))
                .build();
    }

    @Override
    public Void updateConfiguration(String id,
                                    UpdateConfigurationRequest updateConfigurationRequest,
                                    Optional<UUID> xRequestID,
                                    Optional<String> acceptLanguage) {
        updateConfigurationUseCase.execute(id, updateConfigurationRequest.value());
        return null;
    }

    private ModelConfiguration toModelConfiguration(ConfigurationDTO dto) {
        ConfigurationType type = dto.type() != null
                ? ConfigurationType.fromValue(dto.type())
                : null;
        return ModelConfiguration.builder()
                .id(dto.id())
                .value(dto.value())
                .type(type)
                .build();
    }
}
