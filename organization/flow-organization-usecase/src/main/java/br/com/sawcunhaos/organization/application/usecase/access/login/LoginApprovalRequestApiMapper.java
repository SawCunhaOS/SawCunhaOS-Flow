
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

import br.com.sawcunhaos.organization.api.dto.CurrentApprover;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequest;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestEscalationPolicy;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestType;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;

import java.time.ZoneOffset;

final class LoginApprovalRequestApiMapper {

    private LoginApprovalRequestApiMapper() {
    }

    static LoginApprovalRequest toApiLoginApprovalRequest(LoginApprovalRequestOutput output) {
        return LoginApprovalRequest.builder()
                .id(output.id())
                .loginId(output.loginId())
                .requestType(toApiType(output.requestType()))
                .currentLevel(toApiLevel(output.currentLevel()))
                .status(toApiStatus(output.status()))
                .escalationPolicy(toApiEscalationPolicy(output.escalationPolicy()))
                .slaDeadline(output.slaDeadline() != null ? output.slaDeadline().atOffset(ZoneOffset.UTC) : null)
                .decidedByLoginId(output.decidedByLoginId())
                .decidedAt(output.decidedAt() != null ? output.decidedAt().atOffset(ZoneOffset.UTC) : null)
                .createdAt(output.createdAt() != null ? output.createdAt().atOffset(ZoneOffset.UTC) : null)
                .currentApprover(output.currentApproverEmployeeId() != null
                        ? CurrentApprover.builder().employeeId(output.currentApproverEmployeeId()).employeeName(output.currentApproverEmployeeName()).build()
                        : null)
                .build();
    }

    private static LoginApprovalRequestType toApiType(br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType type) {
        return type == null ? null : LoginApprovalRequestType.valueOf(type.name());
    }

    private static LoginApprovalRequestLevel toApiLevel(br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel level) {
        return level == null ? null : LoginApprovalRequestLevel.valueOf(level.name());
    }

    private static LoginApprovalRequestStatus toApiStatus(br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus status) {
        return status == null ? null : LoginApprovalRequestStatus.valueOf(status.name());
    }

    private static LoginApprovalRequestEscalationPolicy toApiEscalationPolicy(br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestEscalationPolicy policy) {
        return policy == null ? null : LoginApprovalRequestEscalationPolicy.valueOf(policy.name());
    }

    static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus toDomainStatus(LoginApprovalRequestStatus status) {
        return status == null ? null : br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus.valueOf(status.name());
    }
}
