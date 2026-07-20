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
import br.com.sawcunhaos.organization.domain.configuration.specification.ConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link UpdateConfigurationUseCaseBean}: delegação de chave+valor e propagação. */
@ExtendWith(MockitoExtension.class)
class UpdateConfigurationUseCaseBeanTest {

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private UpdateConfigurationUseCaseBean useCase;

    @Test
    @DisplayName("delega chave e valor ao service.updateConfiguration")
    void executes_delegatesKeyAndValue() {
        useCase.execute("app.name", "new-value");

        then(configurationService).should().updateConfiguration("app.name", "new-value");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(configurationService).updateConfiguration(any(), any());

        assertThatThrownBy(() -> useCase.execute("app.name", "new-value")).isInstanceOf(ScosException.class);
    }
}
