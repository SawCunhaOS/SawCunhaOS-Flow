
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

package br.com.sawcunhaos.organization.domain.corporate.position.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionPredicatesTest {

    private static final QPosition Q = QPosition.position;

    @Test
    void predicateDepartmentIdShouldFilterByDepartment() {
        Predicate actual = PositionPredicates.predicateDepartmentId(1L);

        Predicate expected = new BooleanBuilder().and(Q.department.id.eq(1L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeAndNotIdShouldExcludeGivenId() {
        Predicate actual = PositionPredicates.predicateCodeAndNotId(1L, "CODE");

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.id.ne(1L))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeShouldFilterByCode() {
        Predicate actual = PositionPredicates.predicateCode("CODE");

        Predicate expected = new BooleanBuilder().and(Q.code.eq("CODE")).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateDepartmentIdAndActiveShouldOnlyFilterByDepartmentIdWhenActiveIsNull() {
        Predicate actual = PositionPredicates.predicateDepartmentIdAndActive(1L, null);

        Predicate expected = new BooleanBuilder().and(Q.department.id.eq(1L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateDepartmentIdAndActiveShouldOnlyFilterByActiveWhenDepartmentIdIsNull() {
        Predicate actual = PositionPredicates.predicateDepartmentIdAndActive(null, true);

        Predicate expected = new BooleanBuilder().and(Q.active.eq(true)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateDepartmentIdAndActiveShouldCombineBothFields() {
        Predicate actual = PositionPredicates.predicateDepartmentIdAndActive(1L, true);

        Predicate expected = new BooleanBuilder()
                .and(Q.department.id.eq(1L))
                .and(Q.active.eq(true))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }
}
