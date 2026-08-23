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
import br.com.sawcunhaos.organization.api.dto.CreatePositionRequest;
import br.com.sawcunhaos.organization.api.dto.Position;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionInput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link CreatePositionUseCaseBean}: mapeamento request→input, output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class CreatePositionUseCaseBeanTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private CreatePositionUseCaseBean useCase;

    private PositionOutput output() {
        return PositionOutput.builder()
                .id(1L).code("DEV").description("Developer").active(true).isTrustPosition(true)
                .department(DepartmentOutput.builder()
                        .id(3L).code("ENG").description("Engineering").active(true).build())
                .build();
    }

    @Test
    @DisplayName("mapeia request para input (id nulo) e retorna o output mapeado com department")
    void executes_mapsRequestToInputAndReturnsMappedOutput() {
        CreatePositionRequest request = new CreatePositionRequest("DEV", "Developer", 3L, true);
        given(positionService.create(any(PositionInput.class))).willReturn(output());

        Position result = useCase.execute(request);

        ArgumentCaptor<PositionInput> captor = ArgumentCaptor.forClass(PositionInput.class);
        then(positionService).should().create(captor.capture());
        PositionInput input = captor.getValue();
        assertThat(input.id()).isNull();
        assertThat(input.code()).isEqualTo("DEV");
        assertThat(input.description()).isEqualTo("Developer");
        assertThat(input.departmentId()).isEqualTo(3L);
        assertThat(input.isTrustPosition()).isTrue();

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("DEV");
        assertThat(result.description()).isEqualTo("Developer");
        assertThat(result.active()).isTrue();
        assertThat(result.isTrustPosition()).isTrue();
        assertThat(result.department().id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("isTrustPosition nulo no request vira false no input")
    void executes_withNullTrustFlag_mapsToFalse() {
        CreatePositionRequest request = new CreatePositionRequest("DEV", "Developer", 3L, null);
        given(positionService.create(any(PositionInput.class))).willReturn(output());

        useCase.execute(request);

        ArgumentCaptor<PositionInput> captor = ArgumentCaptor.forClass(PositionInput.class);
        then(positionService).should().create(captor.capture());
        assertThat(captor.getValue().isTrustPosition()).isFalse();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        CreatePositionRequest request = new CreatePositionRequest("DEV", "Developer", 3L, true);
        given(positionService.create(any(PositionInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(positionService).shouldHaveNoInteractions();
    }
}
