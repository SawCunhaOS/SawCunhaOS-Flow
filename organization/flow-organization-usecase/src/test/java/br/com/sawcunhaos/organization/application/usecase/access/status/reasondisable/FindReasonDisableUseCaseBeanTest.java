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
import br.com.sawcunhaos.organization.api.dto.ReasonDisable;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindReasonDisableUseCaseBean}: delegação do id e mapeamento output→api. */
@ExtendWith(MockitoExtension.class)
class FindReasonDisableUseCaseBeanTest {

    @Mock
    private ReasonDisableService reasonDisableService;

    @InjectMocks
    private FindReasonDisableUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service e retorna o output mapeado")
    void executes_delegatesIdAndReturnsMappedOutput() {
        ReasonDisableOutput output = ReasonDisableOutput.builder()
                .id(5L).code("HOME").description("Home address")
                .entityType(EntityType.COMPANY).active(true).build();
        given(reasonDisableService.findById(5L)).willReturn(output);

        ReasonDisable result = useCase.execute(5L);

        then(reasonDisableService).should().findById(5L);
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.code()).isEqualTo("HOME");
        assertThat(result.description()).isEqualTo("Home address");
        assertThat(result.entityType()).isEqualTo(ReasonEntityType.COMPANY);
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(reasonDisableService.findById(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(99L)).isInstanceOf(ScosException.class);
    }
}
