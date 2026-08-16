
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

final class LoginPredicates {
    private static final QLogin qLogin = QLogin.login1;

    static Predicate predicateTypeAndStatusAndEmployeeId(LoginType type, LoginStatus status, Long employeeId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(type)) {
            booleanBuilder.and(qLogin.type.eq(type));
        }
        if (Objects.nonNull(status)) {
            booleanBuilder.and(qLogin.status.eq(status));
        }
        if (Objects.nonNull(employeeId)) {
            booleanBuilder.and(qLogin.employee.id.eq(employeeId));
        }

        return booleanBuilder.getValue();
    }
}
