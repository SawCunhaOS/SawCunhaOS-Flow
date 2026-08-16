
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

package br.com.sawcunhaos.organization.domain.corporate.employee.internal;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeQueryPredicatesTest {

    private static final QEmployee Q = QEmployee.employee;

    @Test
    void predicatePositionIdShouldFilterByPosition() {
        Predicate actual = EmployeeQueryPredicates.predicatePositionId(1L);

        Predicate expected = new BooleanBuilder().and(Q.position.id.eq(1L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicatePositionIdAndStatusShouldCombineBothFields() {
        Predicate actual = EmployeeQueryPredicates.predicatePositionIdAndStatus(1L, StatusEmployee.ACTIVE);

        Predicate expected = new BooleanBuilder()
                .and(Q.position.id.eq(1L))
                .and(Q.status.eq(StatusEmployee.ACTIVE))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateTaxIdentifierShouldFilterByCpf() {
        Predicate actual = EmployeeQueryPredicates.predicateTaxIdentifier("12345678901");

        Predicate expected = new BooleanBuilder().and(Q.taxIdentifier.cpf.eq("12345678901")).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateEmailShouldFilterByEmail() {
        Predicate actual = EmployeeQueryPredicates.predicateEmail("test@sawcunhaos.com.br");

        Predicate expected = new BooleanBuilder().and(Q.email.email.eq("test@sawcunhaos.com.br")).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateTaxIdentifierAndStatusShouldCombineBothFields() {
        Predicate actual = EmployeeQueryPredicates.predicateTaxIdentifierAndStatus("12345678901", StatusEmployee.ACTIVE);

        Predicate expected = new BooleanBuilder()
                .and(Q.taxIdentifier.cpf.eq("12345678901"))
                .and(Q.status.eq(StatusEmployee.ACTIVE))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCompanyIdAndPositionIdAndStatusShouldFilterOnlyByCompanyId() {
        Predicate actual = EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus(1L, null, null);

        Predicate expected = new BooleanBuilder().and(Q.company.id.eq(1L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCompanyIdAndPositionIdAndStatusShouldFilterOnlyByPositionId() {
        Predicate actual = EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus(null, 2L, null);

        Predicate expected = new BooleanBuilder().and(Q.position.id.eq(2L)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCompanyIdAndPositionIdAndStatusShouldFilterOnlyByStatus() {
        Predicate actual = EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus(null, null, StatusEmployee.ACTIVE);

        Predicate expected = new BooleanBuilder().and(Q.status.eq(StatusEmployee.ACTIVE)).getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    void predicateCompanyIdAndPositionIdAndStatusShouldCombineAllThree() {
        Predicate actual = EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus(1L, 2L, StatusEmployee.ACTIVE);

        Predicate expected = new BooleanBuilder()
                .and(Q.company.id.eq(1L))
                .and(Q.position.id.eq(2L))
                .and(Q.status.eq(StatusEmployee.ACTIVE))
                .getValue();

        assertThat(actual.toString()).isEqualTo(expected.toString());
    }
}
