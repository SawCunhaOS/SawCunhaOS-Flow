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
import br.com.sawcunhaos.organization.api.dto.ConfigurationType;
import br.com.sawcunhaos.organization.api.dto.ModelConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.dto.ConfigurationOutput;
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/** Testes de {@link FindAllConfigurationUseCaseBean}: mapeamento output→api (valor sempre nulo) e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllConfigurationUseCaseBeanTest {

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private FindAllConfigurationUseCaseBean useCase;

    @Test
    @DisplayName("mapeia cada configuração para o contrato omitindo o valor")
    void executes_mapsConfigurationsWithoutValue() {
        given(configurationService.getAllConfigurations())
                .willReturn(List.of(new ConfigurationOutput("app.name", "App name", "STRING", "hello")));

        List<ModelConfiguration> result = useCase.execute();

        assertThat(result).hasSize(1);
        ModelConfiguration model = result.getFirst();
        assertThat(model.id()).isEqualTo("app.name");
        assertThat(model.description()).isEqualTo("App name");
        assertThat(model.value()).isNull();
        assertThat(model.type()).isEqualTo(ConfigurationType.STRING);
    }

    @Test
    @DisplayName("lista vazia retorna vazio")
    void executes_withNoConfigurations_returnsEmpty() {
        given(configurationService.getAllConfigurations()).willReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(configurationService.getAllConfigurations()).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute()).isInstanceOf(ScosException.class);
    }
}
