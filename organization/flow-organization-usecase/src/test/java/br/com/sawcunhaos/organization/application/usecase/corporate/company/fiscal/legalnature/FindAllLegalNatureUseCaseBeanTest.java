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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.GetAllLegalNaturesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindAllLegalNatureUseCaseBean}: paginação, página vazia e propagação. */
@ExtendWith(MockitoExtension.class)
class FindAllLegalNatureUseCaseBeanTest {

    @Mock
    private LegalNatureService legalNatureService;

    @InjectMocks
    private FindAllLegalNatureUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder()
                .page(1).sizePerPage(10).direction(Direction.ASC).order("code").build();
    }

    @Test
    @DisplayName("retorna data mapeada com paginação")
    void executes_returnsMappedPage() {
        LegalNatureOutput output = LegalNatureOutput.builder()
                .id(1L).code("6201-5/01").description("Software development").build();
        given(legalNatureService.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(output)));

        GetAllLegalNaturesResponse response = useCase.execute(filter());

        then(legalNatureService).should().findAll(any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().code()).isEqualTo("6201-5/01");
        assertThat(response.paginatedDTO()).isNotNull();
        assertThat(response.paginatedDTO().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("página vazia retorna data vazia com paginação preenchida")
    void executes_withEmptyPage_returnsEmptyData() {
        given(legalNatureService.findAll(any(Pageable.class))).willReturn(Page.empty());

        GetAllLegalNaturesResponse response = useCase.execute(filter());

        assertThat(response.data()).isEmpty();
        assertThat(response.paginatedDTO()).isNotNull();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(legalNatureService.findAll(any(Pageable.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(filter())).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("filtro nulo lança NullPointerException e não chama o service")
    void nullFilter_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(legalNatureService).shouldHaveNoInteractions();
    }
}
