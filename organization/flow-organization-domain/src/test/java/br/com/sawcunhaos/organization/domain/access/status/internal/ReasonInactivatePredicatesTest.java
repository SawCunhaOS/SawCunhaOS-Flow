
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

package br.com.sawcunhaos.organization.domain.access.status.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonInactivatePredicatesTest {

    private static final QReasonInactivate Q = QReasonInactivate.reasonInactivate;

    @Test
    void predicateCodeAndEntityTypeShouldCombineBothFields() {
        Predicate actual = ReasonInactivatePredicates.predicateCodeAndEntityType("CODE", EntityType.COMPANY);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeAndEntityTypeAndNotIdShouldExcludeGivenId() {
        Predicate actual = ReasonInactivatePredicates.predicateCodeAndEntityTypeAndNotId("CODE", EntityType.COMPANY, 1L);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .and(Q.id.ne(1L))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldOnlyFilterByActiveWhenEntityTypeIsNull() {
        Predicate actual = ReasonInactivatePredicates.predicateEntityTypeAndActive(null, true);

        Predicate expected = new BooleanBuilder().and(Q.active.eq(true)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldOnlyFilterByEntityTypeWhenActiveIsNull() {
        Predicate actual = ReasonInactivatePredicates.predicateEntityTypeAndActive(EntityType.COMPANY, null);

        Predicate expected = new BooleanBuilder().and(Q.entityType.eq(EntityType.COMPANY)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldBeEmptyWhenBothAreNull() {
        Predicate actual = ReasonInactivatePredicates.predicateEntityTypeAndActive(null, null);

        assertThat(actual).isEqualTo(new BooleanBuilder().getValue());
    }
}
