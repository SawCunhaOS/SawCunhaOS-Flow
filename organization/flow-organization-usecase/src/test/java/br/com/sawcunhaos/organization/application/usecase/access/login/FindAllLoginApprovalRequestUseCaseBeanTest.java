
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

import br.com.sawcunhaos.organization.api.dto.Direction;
import br.com.sawcunhaos.organization.api.dto.GetAllLoginApprovalRequestsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FindAllLoginApprovalRequestUseCaseBeanTest {

    @Mock
    private LoginApprovalRequestService loginApprovalRequestService;

    @InjectMocks
    private FindAllLoginApprovalRequestUseCaseBean useCase;

    private PaginationFilter filter() {
        return PaginationFilter.builder().page(1).sizePerPage(10).direction(Direction.ASC).order("id").build();
    }

    private LoginApprovalRequestOutput output(Long id) {
        return LoginApprovalRequestOutput.builder()
                .id(id).loginId(1L)
                .currentLevel(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();
    }

    @Test
    void executeShouldConvertStatusFilterAndDelegateToService() {
        given(loginApprovalRequestService.findAll(eq(LoginApprovalRequestStatus.PENDING), eq(2L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(output(10L))));

        GetAllLoginApprovalRequestsResponse response = useCase.execute(
                filter(), br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestStatus.PENDING, 2L);

        then(loginApprovalRequestService).should().findAll(eq(LoginApprovalRequestStatus.PENDING), eq(2L), any(Pageable.class));
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().id()).isEqualTo(10L);
        assertThat(response.paginatedDTO()).isNotNull();
    }

    @Test
    void executeWithNullFiltersShouldDelegateWithNulls() {
        given(loginApprovalRequestService.findAll(eq(null), eq(null), any(Pageable.class)))
                .willReturn(Page.empty());

        GetAllLoginApprovalRequestsResponse response = useCase.execute(filter(), null, null);

        then(loginApprovalRequestService).should().findAll(eq(null), eq(null), any(Pageable.class));
        assertThat(response.data()).isEmpty();
    }
}
