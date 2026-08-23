
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.CompanyStatusTransitionRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CompanyService;
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

/** Testes de {@link ActivateCompanyUseCaseBean}: delegação a {@code companyService.activate} e propagação. */
@ExtendWith(MockitoExtension.class)
class ActivateCompanyUseCaseBeanTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private ActivateCompanyUseCaseBean useCase;

    @Test
    @DisplayName("delega id/reasonId/observation ao service.activate")
    void executes_delegatesActivateToService() {
        CompanyStatusTransitionRequest request = CompanyStatusTransitionRequest.builder()
                .reasonId(1L)
                .observation("Reaberta após regularização")
                .build();

        useCase.execute(7L, request);

        then(companyService).should().activate(7L, 1L, "Reaberta após regularização");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(companyService).activate(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(7L, CompanyStatusTransitionRequest.builder().reasonId(1L).build()))
                .isInstanceOf(ScosException.class);
    }
}
