
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

import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestProfileChangeKind;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoginProfileChangeUseCaseBeanTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private RequestLoginProfileChangeUseCaseBean useCase;

    @Test
    void executeShouldTranslateSetPrimaryAndDelegateToLoginService() {
        when(loginService.requestProfileChange(10L, 2L, LoginApprovalRequestProfileChangeKind.SET_PRIMARY)).thenReturn(5L);

        Long requestId = useCase.execute(10L, 2L, ProfileChangeKind.SET_PRIMARY);

        assertThat(requestId).isEqualTo(5L);
        verify(loginService).requestProfileChange(10L, 2L, LoginApprovalRequestProfileChangeKind.SET_PRIMARY);
    }

    @Test
    void executeShouldTranslateAddAdditionalAndDelegateToLoginService() {
        useCase.execute(10L, 2L, ProfileChangeKind.ADD_ADDITIONAL);

        verify(loginService).requestProfileChange(10L, 2L, LoginApprovalRequestProfileChangeKind.ADD_ADDITIONAL);
    }
}
