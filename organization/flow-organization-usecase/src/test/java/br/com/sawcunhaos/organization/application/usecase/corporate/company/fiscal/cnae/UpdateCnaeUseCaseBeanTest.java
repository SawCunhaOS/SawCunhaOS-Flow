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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdateCnaeRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeInput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link UpdateCnaeUseCaseBean}: mapeamento id+request→input e propagação. */
@ExtendWith(MockitoExtension.class)
class UpdateCnaeUseCaseBeanTest {

    @Mock
    private CnaeService cnaeService;

    @InjectMocks
    private UpdateCnaeUseCaseBean useCase;

    @Test
    @DisplayName("mapeia id + request para input e chama update")
    void executes_mapsIdAndRequestToInputAndCallsUpdate() {
        UpdateCnaeRequest request = new UpdateCnaeRequest("6202-3/00", "Custom software");

        useCase.execute(10L, request);

        ArgumentCaptor<CnaeInput> captor = ArgumentCaptor.forClass(CnaeInput.class);
        then(cnaeService).should().update(captor.capture());
        CnaeInput input = captor.getValue();
        assertThat(input.id()).isEqualTo(10L);
        assertThat(input.code()).isEqualTo("6202-3/00");
        assertThat(input.description()).isEqualTo("Custom software");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        UpdateCnaeRequest request = new UpdateCnaeRequest("6202-3/00", "Custom software");
        willThrow(new ScosException()).given(cnaeService).update(any(CnaeInput.class));

        assertThatThrownBy(() -> useCase.execute(10L, request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(10L, null)).isInstanceOf(NullPointerException.class);
        then(cnaeService).shouldHaveNoInteractions();
    }
}
