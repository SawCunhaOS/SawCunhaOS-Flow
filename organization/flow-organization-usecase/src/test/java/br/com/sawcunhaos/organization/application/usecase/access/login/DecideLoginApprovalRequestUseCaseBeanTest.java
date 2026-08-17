
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
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

/**
 * Testes de {@link DecideLoginApprovalRequestUseCaseBean}: só a tradução {@link DecisionType} →
 * {@link LoginApprovalRequestStatus} e a resolução de {@code APPROVE_SYSTEM_ACCESS} a partir do
 * contexto de segurança - toda a regra de negócio é testada em {@code LoginApprovalRequestServiceBeanTest} (domain).
 */
@ExtendWith(MockitoExtension.class)
class DecideLoginApprovalRequestUseCaseBeanTest {

    @Mock
    private LoginApprovalRequestService loginApprovalRequestService;

    @InjectMocks
    private DecideLoginApprovalRequestUseCaseBean useCase;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String... permissions) {
        List<SimpleGrantedAuthority> authorities = List.of(permissions).stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("decider", null, authorities));
    }

    @Test
    void executeShouldTranslateApproveDecisionAndResolveNoSystemAccessPermission() {
        authenticateAs("DECIDE_LOGIN_APPROVAL_REQUEST");

        useCase.execute(1L, DecisionType.APPROVE, 4L, "obs");

        then(loginApprovalRequestService).should().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, "obs", false);
    }

    @Test
    void executeShouldTranslateRejectDecisionAndResolveSystemAccessPermissionTrue() {
        authenticateAs("DECIDE_LOGIN_APPROVAL_REQUEST", "APPROVE_SYSTEM_ACCESS");

        useCase.execute(2L, DecisionType.REJECT, 3L, null);

        then(loginApprovalRequestService).should().decide(2L, LoginApprovalRequestStatus.REJECTED, 3L, null, true);
    }

    @Test
    void executeShouldPropagateScosExceptionFromService() {
        authenticateAs("DECIDE_LOGIN_APPROVAL_REQUEST");
        doThrow(new ScosException()).when(loginApprovalRequestService)
                .decide(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());

        assertThatThrownBy(() -> useCase.execute(1L, DecisionType.APPROVE, 4L, null))
                .isInstanceOf(ScosException.class);
    }
}
