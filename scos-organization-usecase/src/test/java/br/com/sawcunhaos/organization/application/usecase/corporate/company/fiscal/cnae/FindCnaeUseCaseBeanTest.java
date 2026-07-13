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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
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

/** Testes de {@link FindCnaeUseCaseBean}: delegação do id e mapeamento output→api. */
@ExtendWith(MockitoExtension.class)
class FindCnaeUseCaseBeanTest {

    @Mock
    private CnaeService cnaeService;

    @InjectMocks
    private FindCnaeUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service e retorna o output mapeado")
    void executes_delegatesIdAndReturnsMappedOutput() {
        CnaeOutput output = CnaeOutput.builder()
                .id(5L).code("6201-5/01").description("Software development").build();
        given(cnaeService.findById(5L)).willReturn(output);

        Cnae result = useCase.execute(5L);

        then(cnaeService).should().findById(5L);
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.code()).isEqualTo("6201-5/01");
        assertThat(result.description()).isEqualTo("Software development");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(cnaeService.findById(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(99L)).isInstanceOf(ScosException.class);
    }
}
