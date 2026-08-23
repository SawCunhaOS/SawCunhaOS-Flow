
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

/** Testes de {@link BlockCompanyUseCaseBean}: delegação a {@code companyService.disable} (não {@code block}) e propagação. */
@ExtendWith(MockitoExtension.class)
class BlockCompanyUseCaseBeanTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private BlockCompanyUseCaseBean useCase;

    @Test
    @DisplayName("delega id/reasonId/observation ao service.disable")
    void executes_delegatesDisableToService() {
        CompanyStatusTransitionRequest request = CompanyStatusTransitionRequest.builder()
                .reasonId(3L)
                .observation("Suspensa em auditoria")
                .build();

        useCase.execute(7L, request);

        then(companyService).should().disable(7L, 3L, "Suspensa em auditoria");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        willThrow(new ScosException()).given(companyService).disable(any(), any(), any());

        assertThatThrownBy(() -> useCase.execute(7L, CompanyStatusTransitionRequest.builder().reasonId(3L).build()))
                .isInstanceOf(ScosException.class);
    }
}
