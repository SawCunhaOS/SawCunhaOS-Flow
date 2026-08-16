
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginPredicatesTest {

    private static final QLogin Q = QLogin.login1;

    @Test
    void predicateTypeAndStatusAndEmployeeIdShouldFilterOnlyByType() {
        Predicate actual = LoginPredicates.predicateTypeAndStatusAndEmployeeId(LoginType.EMPLOYEE, null, null);

        Predicate expected = new BooleanBuilder().and(Q.type.eq(LoginType.EMPLOYEE)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateTypeAndStatusAndEmployeeIdShouldFilterOnlyByStatus() {
        Predicate actual = LoginPredicates.predicateTypeAndStatusAndEmployeeId(null, LoginStatus.PENDING_APPROVAL, null);

        Predicate expected = new BooleanBuilder().and(Q.status.eq(LoginStatus.PENDING_APPROVAL)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateTypeAndStatusAndEmployeeIdShouldFilterOnlyByEmployeeId() {
        Predicate actual = LoginPredicates.predicateTypeAndStatusAndEmployeeId(null, null, 1L);

        Predicate expected = new BooleanBuilder().and(Q.employee.id.eq(1L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateTypeAndStatusAndEmployeeIdShouldCombineAllThree() {
        Predicate actual = LoginPredicates.predicateTypeAndStatusAndEmployeeId(LoginType.EMPLOYEE, LoginStatus.ACTIVE, 1L);

        Predicate expected = new BooleanBuilder()
                .and(Q.type.eq(LoginType.EMPLOYEE))
                .and(Q.status.eq(LoginStatus.ACTIVE))
                .and(Q.employee.id.eq(1L))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }
}
