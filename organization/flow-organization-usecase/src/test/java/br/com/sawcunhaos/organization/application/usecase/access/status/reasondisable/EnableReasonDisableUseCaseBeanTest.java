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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
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

/** Testes de {@link EnableReasonDisableUseCaseBean}: delegação do enable e propagação. */
@ExtendWith(MockitoExtension.class)
class EnableReasonDisableUseCaseBeanTest {

    @Mock
    private ReasonDisableService reasonDisableService;

    @InjectMocks
    private EnableReasonDisableUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service.enable")
    void executes_delegatesEnableToService() {
        useCase.execute(7L);

        then(reasonDisableService).should().enable(7L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(reasonDisableService).enable(any());

        assertThatThrownBy(() -> useCase.execute(7L)).isInstanceOf(ScosException.class);
    }
}
