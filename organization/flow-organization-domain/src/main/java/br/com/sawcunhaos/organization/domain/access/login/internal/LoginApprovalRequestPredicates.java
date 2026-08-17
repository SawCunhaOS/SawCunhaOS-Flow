
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

package br.com.sawcunhaos.organization.domain.access.login.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import java.util.Objects;

final class LoginApprovalRequestPredicates {
    private static final QLoginApprovalRequest qLoginApprovalRequest = QLoginApprovalRequest.loginApprovalRequest;

    static Predicate predicateStatusAndLoginId(LoginApprovalRequestStatus status, Long loginId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(status)) {
            booleanBuilder.and(qLoginApprovalRequest.status.eq(status));
        }
        if (Objects.nonNull(loginId)) {
            booleanBuilder.and(qLoginApprovalRequest.login.id.eq(loginId));
        }

        return booleanBuilder.getValue();
    }
}
