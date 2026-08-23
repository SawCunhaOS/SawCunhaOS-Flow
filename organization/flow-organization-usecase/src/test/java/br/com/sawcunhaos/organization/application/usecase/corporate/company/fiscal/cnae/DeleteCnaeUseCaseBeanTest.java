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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
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

/** Testes de {@link DeleteCnaeUseCaseBean}: delegação do delete e propagação. */
@ExtendWith(MockitoExtension.class)
class DeleteCnaeUseCaseBeanTest {

    @Mock
    private CnaeService cnaeService;

    @InjectMocks
    private DeleteCnaeUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service.delete")
    void executes_delegatesDeleteToService() {
        useCase.execute(8L);

        then(cnaeService).should().delete(8L);
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(cnaeService).delete(any());

        assertThatThrownBy(() -> useCase.execute(8L)).isInstanceOf(ScosException.class);
    }
}
