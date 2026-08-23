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

package br.com.sawcunhaos.organization.application.usecase.corporate.department;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Department;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
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

/** Testes de {@link FindDepartmentUseCaseBean}: delegação do id e montagem da resposta. */
@ExtendWith(MockitoExtension.class)
class FindDepartmentUseCaseBeanTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private FindDepartmentUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service e retorna o output mapeado")
    void executes_delegatesIdAndReturnsMappedOutput() {
        DepartmentOutput output = DepartmentOutput.builder()
                .id(5L).code("HR").description("Human Resources").active(true).build();
        given(departmentService.findById(5L)).willReturn(output);

        Department result = useCase.execute(5L);

        then(departmentService).should().findById(5L);
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.code()).isEqualTo("HR");
        assertThat(result.description()).isEqualTo("Human Resources");
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(departmentService.findById(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(99L)).isInstanceOf(ScosException.class);
    }
}
