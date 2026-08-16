
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

package br.com.sawcunhaos.organization.domain.corporate.department.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepartmentPredicatesTest {

    private static final QDepartment Q = QDepartment.department;

    @Test
    void predicateCodeAndNotIdShouldExcludeGivenId() {
        Predicate actual = DepartmentPredicates.predicateCodeAndNotId(1L, "CODE");

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.id.ne(1L))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeShouldFilterByCode() {
        Predicate actual = DepartmentPredicates.predicateCode("CODE");

        Predicate expected = new BooleanBuilder().and(Q.code.eq("CODE")).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateActiveShouldFilterByActive() {
        Predicate actual = DepartmentPredicates.predicateActive(true);

        Predicate expected = new BooleanBuilder().and(Q.active.eq(true)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }
}
