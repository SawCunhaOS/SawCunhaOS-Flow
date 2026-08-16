
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

class AddressTypePredicatesTest {

    private static final QAddressType Q = QAddressType.addressType;

    @Test
    void predicateCodeAndEntityTypeShouldCombineBothFields() {
        Predicate actual = AddressTypePredicates.predicateCodeAndEntityType("CODE", EntityType.COMPANY);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCodeAndEntityTypeAndNotIdShouldExcludeGivenId() {
        Predicate actual = AddressTypePredicates.predicateCodeAndEntityTypeAndNotId("CODE", EntityType.COMPANY, 1L);

        Predicate expected = new BooleanBuilder()
                .and(Q.code.eq("CODE"))
                .and(Q.id.ne(1L))
                .and(Q.entityType.eq(EntityType.COMPANY))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityAndActiveShouldOnlyFilterByActiveWhenEntityTypeIsNull() {
        Predicate actual = AddressTypePredicates.predicateEntityAndActive(null, true);

        Predicate expected = new BooleanBuilder().and(Q.active.eq(true)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityAndActiveShouldOnlyFilterByEntityTypeWhenActiveIsNull() {
        Predicate actual = AddressTypePredicates.predicateEntityAndActive(EntityType.COMPANY, null);

        Predicate expected = new BooleanBuilder().and(Q.entityType.eq(EntityType.COMPANY)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEntityAndActiveShouldBeEmptyWhenBothAreNull() {
        Predicate actual = AddressTypePredicates.predicateEntityAndActive(null, null);

        Predicate expected = new BooleanBuilder().getValue();

        assertThat(actual).isEqualTo(expected);
    }
}
