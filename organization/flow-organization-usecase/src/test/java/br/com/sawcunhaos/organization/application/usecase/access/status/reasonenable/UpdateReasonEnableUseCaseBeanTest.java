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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonenable;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.api.dto.UpdateReasonEnableRequest;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
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

/** Testes de {@link UpdateReasonEnableUseCaseBean}: mapeamento id+request→input e propagação. */
@ExtendWith(MockitoExtension.class)
class UpdateReasonEnableUseCaseBeanTest {

    @Mock
    private ReasonEnableService reasonEnableService;

    @InjectMocks
    private UpdateReasonEnableUseCaseBean useCase;

    @Test
    @DisplayName("mapeia id + request para input e chama update")
    void executes_mapsIdAndRequestToInputAndCallsUpdate() {
        UpdateReasonEnableRequest request =
                new UpdateReasonEnableRequest("WORK", "Work address", ReasonEntityType.EMPLOYEE);

        useCase.execute(10L, request);

        ArgumentCaptor<ReasonEnableInput> captor = ArgumentCaptor.forClass(ReasonEnableInput.class);
        then(reasonEnableService).should().update(captor.capture());
        ReasonEnableInput input = captor.getValue();
        assertThat(input.id()).isEqualTo(10L);
        assertThat(input.code()).isEqualTo("WORK");
        assertThat(input.description()).isEqualTo("Work address");
        assertThat(input.entityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        UpdateReasonEnableRequest request =
                new UpdateReasonEnableRequest("WORK", "Work address", ReasonEntityType.EMPLOYEE);
        willThrow(new ScosException()).given(reasonEnableService).update(any(ReasonEnableInput.class));

        assertThatThrownBy(() -> useCase.execute(10L, request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(10L, null)).isInstanceOf(NullPointerException.class);
        then(reasonEnableService).shouldHaveNoInteractions();
    }
}
