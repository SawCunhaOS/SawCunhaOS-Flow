
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.internal;

import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTypePredicatesTest {

    private static final QContactType Q = QContactType.contactType;

    @Test
    void predicateCodeAndEntityTypeShouldCombineBothFields() {
        Predicate actual = ContactTypePredicates.predicateCodeAndEntityType("CODE", EntityType.COMPANY);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeAndEntityAndNotIdShouldExcludeGivenId() {
        Predicate actual = ContactTypePredicates.predicateCodeAndEntityAndNotId("CODE", EntityType.COMPANY, 1L);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.id.ne(1L))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldOnlyFilterByActiveWhenEntityTypeIsNull() {
        Predicate actual = ContactTypePredicates.predicateEntityTypeAndActive(null, true);

        Predicate expected = new BooleanBuilder().and(Q.active.eq(true)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldOnlyFilterByEntityTypeWhenActiveIsNull() {
        Predicate actual = ContactTypePredicates.predicateEntityTypeAndActive(EntityType.COMPANY, null);

        Predicate expected = new BooleanBuilder().and(Q.entityType.eq(EntityType.COMPANY)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityTypeAndActiveShouldBeEmptyWhenBothAreNull() {
        Predicate actual = ContactTypePredicates.predicateEntityTypeAndActive(null, null);

        Predicate expected = new BooleanBuilder().getValue();

        assertThat(actual).isEqualTo(expected);
    }
}
