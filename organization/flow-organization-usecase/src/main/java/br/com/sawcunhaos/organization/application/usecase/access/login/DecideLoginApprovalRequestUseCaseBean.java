
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a decisão (AC 1-7 da Story 3.2): traduz {@link DecisionType} (contrato do Use Case)
 * para {@link LoginApprovalRequestStatus} (vocabulário do domínio) e resolve
 * {@code APPROVE_SYSTEM_ACCESS} a partir do contexto de segurança - mesmo mecanismo do
 * {@code @PreAuthorize}. Toda a regra de negócio (elegibilidade, transições, Outbox) vive em
 * {@link LoginApprovalRequestService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class)
class DecideLoginApprovalRequestUseCaseBean implements DecideLoginApprovalRequestUseCase {

    /** Espelha {@code ScosOrganizationPermission.APPROVE_SYSTEM_ACCESS} - módulo infrastructure não é dependência do usecase. */
    private static final String PERMISSION_APPROVE_SYSTEM_ACCESS = "APPROVE_SYSTEM_ACCESS";

    private final LoginApprovalRequestService loginApprovalRequestService;

    @Override
    public void execute(@NonNull Long requestId, @NonNull DecisionType decision, @NonNull Long reasonId, String observation) {
        log.info("Decide Login Approval Request: {}, Decision: {}", requestId, decision);

        LoginApprovalRequestStatus domainDecision = decision == DecisionType.APPROVE
                ? LoginApprovalRequestStatus.APPROVED
                : LoginApprovalRequestStatus.REJECTED;

        loginApprovalRequestService.decide(requestId, domainDecision, reasonId, observation, currentUserHasSystemAccessApproval());
    }

    private boolean currentUserHasSystemAccessApproval() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(PERMISSION_APPROVE_SYSTEM_ACCESS::equals);
    }
}
