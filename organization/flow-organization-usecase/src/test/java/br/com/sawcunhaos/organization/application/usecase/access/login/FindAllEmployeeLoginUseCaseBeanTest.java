
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.GetAllLoginsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindAllEmployeeLoginUseCaseBean}: delega para {@link FindAllLoginUseCase} escopado por employeeId, sem outros filtros. */
@ExtendWith(MockitoExtension.class)
class FindAllEmployeeLoginUseCaseBeanTest {

    @Mock
    private FindAllLoginUseCase findAllLoginUseCase;

    @InjectMocks
    private FindAllEmployeeLoginUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder().page(1).sizePerPage(10).direction(Direction.ASC).order("login").build();
    }

    @Test
    @DisplayName("delega para FindAllLoginUseCase com type/status nulos e employeeId do path")
    void executes_delegatesToFindAllLoginUseCaseScopedByEmployeeId() {
        GetAllLoginsResponse response = GetAllLoginsResponse.builder().data(java.util.List.of()).build();
        given(findAllLoginUseCase.execute(filter(), null, null, 2L)).willReturn(response);

        GetAllLoginsResponse result = useCase.execute(2L, filter());

        then(findAllLoginUseCase).should().execute(filter(), null, null, 2L);
        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("propaga ScosException quando FindAllLoginUseCase falha")
    void whenDelegateThrows_propagatesScosException() {
        given(findAllLoginUseCase.execute(filter(), null, null, 2L)).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(2L, filter())).isInstanceOf(ScosException.class);
    }
}
