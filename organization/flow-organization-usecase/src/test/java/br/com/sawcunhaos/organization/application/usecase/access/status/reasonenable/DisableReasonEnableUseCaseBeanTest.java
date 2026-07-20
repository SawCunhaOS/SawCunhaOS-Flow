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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonEnableService;
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

/** Testes de {@link DisableReasonEnableUseCaseBean}: delegação do disable e propagação. */
@ExtendWith(MockitoExtension.class)
class DisableReasonEnableUseCaseBeanTest {

    @Mock
    private ReasonEnableService reasonEnableService;

    @InjectMocks
    private DisableReasonEnableUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service.disable")
    void executes_delegatesDisableToService() {
        useCase.execute(8L);

        then(reasonEnableService).should().disable(8L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(reasonEnableService).disable(any());

        assertThatThrownBy(() -> useCase.execute(8L)).isInstanceOf(ScosException.class);
    }
}
