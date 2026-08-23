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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.ConfigurationType;
import br.com.sawcunhaos.organization.api.dto.KeyConfiguration;
import br.com.sawcunhaos.organization.domain.configuration.dto.KeyConfigurationOutput;
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

/** Testes de {@link FindAllKeysConfigurationUseCaseBean}: mapeamento das chaves output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllKeysConfigurationUseCaseBeanTest {

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private FindAllKeysConfigurationUseCaseBean useCase;

    @Test
    @DisplayName("mapeia cada chave para o contrato")
    void executes_mapsKeys() {
        given(configurationService.getAllKeys())
                .willReturn(List.of(new KeyConfigurationOutput("app.name", "App name", "STRING")));

        List<KeyConfiguration> result = useCase.execute();

        assertThat(result).hasSize(1);
        KeyConfiguration key = result.getFirst();
        assertThat(key.id()).isEqualTo("app.name");
        assertThat(key.description()).isEqualTo("App name");
        assertThat(key.type()).isEqualTo(ConfigurationType.STRING);
    }

    @Test
    @DisplayName("lista vazia retorna vazio")
    void executes_withNoKeys_returnsEmpty() {
        given(configurationService.getAllKeys()).willReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(configurationService.getAllKeys()).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute()).isInstanceOf(ScosException.class);
    }
}
