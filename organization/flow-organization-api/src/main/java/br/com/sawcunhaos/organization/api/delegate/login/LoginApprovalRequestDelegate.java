
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

package br.com.sawcunhaos.organization.api.delegate.login;

import br.com.sawcunhaos.organization.api.controller.LoginApprovalRequestApiDelegate;
import br.com.sawcunhaos.organization.api.dto.ApproveLoginApprovalRequestRequest;
import br.com.sawcunhaos.organization.api.dto.GetAllLoginApprovalRequestsResponse;
import br.com.sawcunhaos.organization.api.dto.GetLoginApprovalRequestResponse;
import br.com.sawcunhaos.organization.api.dto.GetSystemAccessApproversResponse;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.RejectLoginApprovalRequestRequest;
import br.com.sawcunhaos.organization.application.usecase.access.login.DecideLoginApprovalRequestUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.DecisionType;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindAllLoginApprovalRequestUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindLoginApprovalRequestUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.login.FindSystemAccessApproversUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginApprovalRequestDelegate implements LoginApprovalRequestApiDelegate {

    private final FindAllLoginApprovalRequestUseCase findAllLoginApprovalRequestUseCase;
    private final FindLoginApprovalRequestUseCase findLoginApprovalRequestUseCase;
    private final FindSystemAccessApproversUseCase findSystemAccessApproversUseCase;
    private final DecideLoginApprovalRequestUseCase decideLoginApprovalRequestUseCase;

    @Override
    public GetAllLoginApprovalRequestsResponse getAllLoginApprovalRequests(PaginationFilter paginationFilter, Optional<UUID> xRequestID, Optional<String> acceptLanguage, Optional<LoginApprovalRequestStatus> status, Optional<Long> loginId) {
        return findAllLoginApprovalRequestUseCase.execute(paginationFilter, status.orElse(null), loginId.orElse(null));
    }

    @Override
    public GetLoginApprovalRequestResponse getLoginApprovalRequestById(Long id, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return findLoginApprovalRequestUseCase.execute(id);
    }

    @Override
    public GetSystemAccessApproversResponse getSystemAccessApprovers(Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        return findSystemAccessApproversUseCase.execute();
    }

    @Override
    public Void approveLoginApprovalRequest(Long id, ApproveLoginApprovalRequestRequest approveLoginApprovalRequestRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        decideLoginApprovalRequestUseCase.execute(id, DecisionType.APPROVE, approveLoginApprovalRequestRequest.reasonId(), null);
        return null;
    }

    @Override
    public Void rejectLoginApprovalRequest(Long id, RejectLoginApprovalRequestRequest rejectLoginApprovalRequestRequest, Optional<UUID> xRequestID, Optional<String> acceptLanguage) {
        decideLoginApprovalRequestUseCase.execute(id, DecisionType.REJECT, rejectLoginApprovalRequestRequest.reasonId(), rejectLoginApprovalRequestRequest.observation());
        return null;
    }
}
