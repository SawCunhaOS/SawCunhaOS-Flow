
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
import br.com.sawcunhaos.organization.api.dto.GetLoginApprovalRequestResponse;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/** Testes de {@link FindLoginApprovalRequestUseCaseBean}: mapeamento do {@link LoginApprovalRequestOutput} para a resposta da API. */
@ExtendWith(MockitoExtension.class)
class FindLoginApprovalRequestUseCaseBeanTest {

    @Mock
    private LoginApprovalRequestService loginApprovalRequestService;

    @InjectMocks
    private FindLoginApprovalRequestUseCaseBean useCase;

    private LoginApprovalRequestOutput output(Long currentApproverId, String currentApproverName) {
        return LoginApprovalRequestOutput.builder()
                .id(1L).loginId(10L)
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .currentApproverEmployeeId(currentApproverId)
                .currentApproverEmployeeName(currentApproverName)
                .build();
    }

    @Test
    void executeShouldMapOutputIncludingCurrentApprover() {
        given(loginApprovalRequestService.findById(1L)).willReturn(output(5L, "Sam Supervisor"));

        GetLoginApprovalRequestResponse response = useCase.execute(1L);

        assertThat(response.data().id()).isEqualTo(1L);
        assertThat(response.data().loginId()).isEqualTo(10L);
        assertThat(response.data().currentApprover().employeeId()).isEqualTo(5L);
        assertThat(response.data().currentApprover().employeeName()).isEqualTo("Sam Supervisor");
    }

    @Test
    void executeShouldMapNullCurrentApproverWhenAbsent() {
        given(loginApprovalRequestService.findById(1L)).willReturn(output(null, null));

        GetLoginApprovalRequestResponse response = useCase.execute(1L);

        assertThat(response.data().currentApprover()).isNull();
    }

    @Test
    void executeShouldPropagateScosExceptionFromService() {
        willThrow(new ScosException()).given(loginApprovalRequestService).findById(999L);

        assertThatThrownBy(() -> useCase.execute(999L)).isInstanceOf(ScosException.class);
    }
}
