
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

package br.com.sawcunhaos.organization.domain.corporate.company.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyPredicatesTest {

    private static final QCompany Q = QCompany.company;

    @Test
    void predicateStatusAndNameShouldOnlyFilterByStatusWhenNameIsNull() {
        Predicate actual = CompanyPredicates.predicateStatusAndName(StatusCompany.ACTIVE, null);

        Predicate expected = new BooleanBuilder().and(Q.status.eq(StatusCompany.ACTIVE)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateStatusAndNameShouldOnlyFilterByNameWhenStatusIsNull() {
        Predicate actual = CompanyPredicates.predicateStatusAndName(null, "Saw");

        Predicate expected = new BooleanBuilder().and(Q.name.containsIgnoreCase("Saw")).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateStatusAndNameShouldCombineBothFields() {
        Predicate actual = CompanyPredicates.predicateStatusAndName(StatusCompany.ACTIVE, "Saw");

        Predicate expected = new BooleanBuilder()
                .and(Q.status.eq(StatusCompany.ACTIVE))
                .and(Q.name.containsIgnoreCase("Saw"))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateStatusAndNameShouldFallBackToIdIsNotNullWhenNoFilterIsGiven() {
        Predicate actual = CompanyPredicates.predicateStatusAndName(null, null);

        Predicate expected = new BooleanBuilder().and(Q.id.isNotNull()).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateStatusAndNameShouldFallBackToIdIsNotNullWhenNameIsBlank() {
        Predicate actual = CompanyPredicates.predicateStatusAndName(null, "   ");

        Predicate expected = new BooleanBuilder().and(Q.id.isNotNull()).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }
}
