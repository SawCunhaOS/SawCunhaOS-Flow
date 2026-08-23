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

/** Testes de {@link EnableReasonEnableUseCaseBean}: delegação do enable e propagação. */
@ExtendWith(MockitoExtension.class)
class EnableReasonEnableUseCaseBeanTest {

    @Mock
    private ReasonEnableService reasonEnableService;

    @InjectMocks
    private EnableReasonEnableUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service.enable")
    void executes_delegatesEnableToService() {
        useCase.execute(7L);

        then(reasonEnableService).should().enable(7L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(reasonEnableService).enable(any());

        assertThatThrownBy(() -> useCase.execute(7L)).isInstanceOf(ScosException.class);
    }
}
