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

package br.com.sawcunhaos.organization.application.usecase.access.system.registry;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.system.dto.RegisterScosSystemInput;
import br.com.sawcunhaos.organization.domain.access.system.dto.ScosSystemOutput;
import br.com.sawcunhaos.organization.domain.access.system.specification.ScosSystemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link RegistrySystemUseCaseBean}: mapeamento request→input do domínio, output→contrato e propagação. */
@ExtendWith(MockitoExtension.class)
class RegistrySystemUseCaseBeanTest {

    @Mock
    private ScosSystemService scosSystemService;

    @InjectMocks
    private RegistrySystemUseCaseBean useCase;

    @Test
    @DisplayName("mapeia o request para o input de registro e projeta o output do contrato")
    void executes_mapsRequestAndReturnsRegistryOutput() {
        RegistrySystemInput request =
                new RegistrySystemInput("Scos Organization", "SCOS_ORGANIZATION", "Sistema", "1.0.0");
        UUID id = UUID.randomUUID();
        ScosSystemOutput system = new ScosSystemOutput(
                id, "Scos Organization", "SCOS_ORGANIZATION", "Sistema",
                "secret-key", "ACTIVE", "1.0.0", true);
        given(scosSystemService.register(any(RegisterScosSystemInput.class))).willReturn(system);

        RegistrySystemOutput result = useCase.execute(request);

        ArgumentCaptor<RegisterScosSystemInput> captor = ArgumentCaptor.forClass(RegisterScosSystemInput.class);
        then(scosSystemService).should().register(captor.capture());
        RegisterScosSystemInput input = captor.getValue();
        assertThat(input.name()).isEqualTo("Scos Organization");
        assertThat(input.code()).isEqualTo("SCOS_ORGANIZATION");
        assertThat(input.description()).isEqualTo("Sistema");
        assertThat(input.version()).isEqualTo("1.0.0");

        assertThat(result.systemId()).isEqualTo(id.toString());
        assertThat(result.secretKey()).isEqualTo("secret-key");
        assertThat(result.update()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        RegistrySystemInput request =
                new RegistrySystemInput("Scos Organization", "SCOS_ORGANIZATION", "Sistema", "1.0.0");
        given(scosSystemService.register(any(RegisterScosSystemInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(scosSystemService).shouldHaveNoInteractions();
    }
}
