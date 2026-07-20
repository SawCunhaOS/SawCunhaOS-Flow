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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.GetAllDepartmentsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.department.specification.DepartmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindAllDepartmentUseCaseBean}: paginação, filtro ativo, página vazia e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllDepartmentUseCaseBeanTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private FindAllDepartmentUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder()
                .page(1).sizePerPage(10).direction(Direction.ASC).order("code").build();
    }

    @Test
    @DisplayName("repassa o filtro ativo e retorna data mapeada com paginação")
    void executes_passesActiveFilterAndReturnsMappedPage() {
        DepartmentOutput output = DepartmentOutput.builder()
                .id(1L).code("HR").description("Human Resources").active(true).build();
        given(departmentService.findAll(eq(true), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(output)));

        GetAllDepartmentsResponse response = useCase.execute(filter(), true);

        then(departmentService).should().findAll(eq(true), any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().code()).isEqualTo("HR");
        assertThat(response.data().getFirst().active()).isTrue();
        assertThat(response.paginatedDTO()).isNotNull();
        assertThat(response.paginatedDTO().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("página vazia retorna data vazia com paginação preenchida")
    void executes_withEmptyPage_returnsEmptyData() {
        given(departmentService.findAll(any(), any(Pageable.class))).willReturn(Page.empty());

        GetAllDepartmentsResponse response = useCase.execute(filter(), false);

        assertThat(response.data()).isEmpty();
        assertThat(response.paginatedDTO()).isNotNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(departmentService.findAll(any(), any(Pageable.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(filter(), true)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("filtro nulo lança NullPointerException e não chama o service")
    void nullFilter_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null, true)).isInstanceOf(NullPointerException.class);
        then(departmentService).shouldHaveNoInteractions();
    }
}
