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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.LegalNature;
import br.com.sawcunhaos.organization.api.dto.CreateLegalNatureRequest;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
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

/** Testes de {@link CreateLegalNatureUseCaseBean}: mapeamento request→input, output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class CreateLegalNatureUseCaseBeanTest {

    @Mock
    private LegalNatureService legalNatureService;

    @InjectMocks
    private CreateLegalNatureUseCaseBean useCase;

    @Test
    @DisplayName("mapeia request para input (id nulo) e retorna o output mapeado")
    void executes_mapsRequestToInputAndReturnsMappedOutput() {
        CreateLegalNatureRequest request = new CreateLegalNatureRequest("6201-5/01", "Software development");
        LegalNatureOutput output = LegalNatureOutput.builder()
                .id(1L).code("6201-5/01").description("Software development").build();
        given(legalNatureService.create(any(LegalNatureInput.class))).willReturn(output);

        LegalNature result = useCase.execute(request);

        ArgumentCaptor<LegalNatureInput> captor = ArgumentCaptor.forClass(LegalNatureInput.class);
        then(legalNatureService).should().create(captor.capture());
        LegalNatureInput input = captor.getValue();
        assertThat(input.id()).isNull();
        assertThat(input.code()).isEqualTo("6201-5/01");
        assertThat(input.description()).isEqualTo("Software development");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("6201-5/01");
        assertThat(result.description()).isEqualTo("Software development");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        CreateLegalNatureRequest request = new CreateLegalNatureRequest("6201-5/01", "Software development");
        given(legalNatureService.create(any(LegalNatureInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(legalNatureService).shouldHaveNoInteractions();
    }
}
