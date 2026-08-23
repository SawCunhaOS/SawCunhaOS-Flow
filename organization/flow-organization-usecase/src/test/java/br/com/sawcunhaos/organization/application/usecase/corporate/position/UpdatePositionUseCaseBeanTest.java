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

package br.com.sawcunhaos.organization.application.usecase.corporate.position;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.UpdatePositionRequest;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
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

/** Testes de {@link UpdatePositionUseCaseBean}: mapeamento id+request→input e propagação. */
@ExtendWith(MockitoExtension.class)
class UpdatePositionUseCaseBeanTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private UpdatePositionUseCaseBean useCase;

    @Test
    @DisplayName("mapeia id + request para input e chama update")
    void executes_mapsIdAndRequestToInputAndCallsUpdate() {
        UpdatePositionRequest request = new UpdatePositionRequest("LEAD", "Tech Lead", 4L, true);

        useCase.execute(10L, request);

        ArgumentCaptor<PositionInput> captor = ArgumentCaptor.forClass(PositionInput.class);
        then(positionService).should().update(captor.capture());
        PositionInput input = captor.getValue();
        assertThat(input.id()).isEqualTo(10L);
        assertThat(input.code()).isEqualTo("LEAD");
        assertThat(input.description()).isEqualTo("Tech Lead");
        assertThat(input.departmentId()).isEqualTo(4L);
        assertThat(input.isTrustPosition()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        UpdatePositionRequest request = new UpdatePositionRequest("LEAD", "Tech Lead", 4L, true);
        willThrow(new ScosException()).given(positionService).update(any(PositionInput.class));

        assertThatThrownBy(() -> useCase.execute(10L, request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(10L, null)).isInstanceOf(NullPointerException.class);
        then(positionService).shouldHaveNoInteractions();
    }
}
