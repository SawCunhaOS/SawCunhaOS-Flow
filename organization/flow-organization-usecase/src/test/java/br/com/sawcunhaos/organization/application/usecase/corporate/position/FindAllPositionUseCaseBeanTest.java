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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.GetAllPositionsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.domain.corporate.department.dto.DepartmentOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
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

/** Testes de {@link FindAllPositionUseCaseBean}: filtro por department/ativo, página vazia e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllPositionUseCaseBeanTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private FindAllPositionUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder()
                .page(1).sizePerPage(10).direction(Direction.ASC).order("code").build();
    }

    private PositionOutput output() {
        return PositionOutput.builder()
                .id(1L).code("DEV").description("Developer").active(true).isTrustPosition(false)
                .department(DepartmentOutput.builder()
                        .id(3L).code("ENG").description("Engineering").active(true).build())
                .build();
    }

    @Test
    @DisplayName("repassa departmentId e ativo e retorna data mapeada com paginação")
    void executes_passesDepartmentAndActiveFilters() {
        given(positionService.findAll(eq(3L), eq(true), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(output())));

        GetAllPositionsResponse response = useCase.execute(filter(), 3L, true);

        then(positionService).should().findAll(eq(3L), eq(true), any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().code()).isEqualTo("DEV");
        assertThat(response.paginatedDTO()).isNotNull();
        assertThat(response.paginatedDTO().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("página vazia retorna data vazia com paginação preenchida")
    void executes_withEmptyPage_returnsEmptyData() {
        given(positionService.findAll(any(), any(), any(Pageable.class))).willReturn(Page.empty());

        GetAllPositionsResponse response = useCase.execute(filter(), 3L, false);

        assertThat(response.data()).isEmpty();
        assertThat(response.paginatedDTO()).isNotNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(positionService.findAll(any(), any(), any(Pageable.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(filter(), 3L, true)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("filtro nulo lança NullPointerException e não chama o service")
    void nullFilter_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null, 3L, true)).isInstanceOf(NullPointerException.class);
        then(positionService).shouldHaveNoInteractions();
    }
}
