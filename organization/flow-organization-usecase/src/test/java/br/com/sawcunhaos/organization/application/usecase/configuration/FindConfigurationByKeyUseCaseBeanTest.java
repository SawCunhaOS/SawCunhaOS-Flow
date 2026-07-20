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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindConfigurationByKeyUseCaseBean}: delegação da chave e mapeamento com valor. */
@ExtendWith(MockitoExtension.class)
class FindConfigurationByKeyUseCaseBeanTest {

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private FindConfigurationByKeyUseCaseBean useCase;

    @Test
    @DisplayName("delega a chave ao service e retorna a configuração com valor")
    void executes_delegatesKeyAndReturnsConfigurationWithValue() {
        given(configurationService.getConfiguration("app.name"))
                .willReturn(new ConfigurationOutput("app.name", "App name", "STRING", "hello"));

        ModelConfiguration result = useCase.execute("app.name");

        then(configurationService).should().getConfiguration("app.name");
        assertThat(result.id()).isEqualTo("app.name");
        assertThat(result.description()).isEqualTo("App name");
        assertThat(result.value()).isEqualTo("hello");
        assertThat(result.type()).isEqualTo(ConfigurationType.STRING);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(configurationService.getConfiguration(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute("missing")).isInstanceOf(ScosException.class);
    }
}
