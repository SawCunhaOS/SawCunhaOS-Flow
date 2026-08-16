
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
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
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

/** Testes de {@link FindAllLoginUseCaseBean}: mapeamento de filtros, paginação e resposta resumida. */
@ExtendWith(MockitoExtension.class)
class FindAllLoginUseCaseBeanTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private FindAllLoginUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder().page(1).sizePerPage(10).direction(Direction.ASC).order("login").build();
    }

    private LoginOutput loginOutput(long id) {
        return LoginOutput.builder().id(id).login("login" + id).type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profileId(1L).employeeId(2L).build();
    }

    @Test
    @DisplayName("repassa type/status/employeeId convertidos ao service e mapeia a resposta resumida")
    void executes_mapsFiltersAndReturnsSummarizedPage() {
        given(loginService.findAll(eq(LoginType.EMPLOYEE), eq(LoginStatus.PENDING_APPROVAL), eq(2L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(loginOutput(10L))));

        GetAllLoginsResponse response = useCase.execute(
                filter(),
                br.com.sawcunhaos.organization.api.dto.LoginType.EMPLOYEE,
                br.com.sawcunhaos.organization.api.dto.LoginStatus.PENDING_APPROVAL,
                2L
        );

        then(loginService).should().findAll(eq(LoginType.EMPLOYEE), eq(LoginStatus.PENDING_APPROVAL), eq(2L), any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().id()).isEqualTo(10L);
        assertThat(response.paginatedDTO()).isNotNull();
        assertThat(response.paginatedDTO().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("filtros todos nulos chama o service com (null, null, null, pageable)")
    void executes_withAllFiltersNull_callsServiceWithNulls() {
        given(loginService.findAll(eq(null), eq(null), eq(null), any(Pageable.class))).willReturn(Page.empty());

        GetAllLoginsResponse response = useCase.execute(filter(), null, null, null);

        then(loginService).should().findAll(eq(null), eq(null), eq(null), any(Pageable.class));
        assertThat(response.data()).isEmpty();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(loginService.findAll(any(), any(), any(), any(Pageable.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(filter(), null, null, null)).isInstanceOf(ScosException.class);
    }
}
