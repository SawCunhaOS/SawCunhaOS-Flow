
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

import java.util.Objects;

final class EmployeeQueryPredicates {
    private static final QEmployee qEmployee = QEmployee.employee;

    static Predicate predicatePositionId(Long positionId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.position.id.eq(positionId));
        return booleanBuilder.getValue();
    }

    static Predicate predicatePositionIdAndStatus(Long positionId, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.position.id.eq(positionId))
                .and(qEmployee.status.eq(status));
        return booleanBuilder.getValue();
    }

    static Predicate predicateTaxIdentifier(String taxIdentifier) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier));
        return booleanBuilder.getValue();
    }

    static Predicate predicateEmail(String email) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.email.email.eq(email));
        return booleanBuilder.getValue();
    }

    static Predicate predicateTaxIdentifierAndStatus(String taxIdentifier, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier))
                .and(qEmployee.status.eq(status));
        return booleanBuilder.getValue();
    }

    static Predicate predicateCompanyIdAndPositionIdAndStatus(Long companyId, Long positionId, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(companyId)) {
            booleanBuilder.and(qEmployee.company.id.eq(companyId));
        }
        if (Objects.nonNull(positionId)) {
            booleanBuilder.and(qEmployee.position.id.eq(positionId));
        }
        if (Objects.nonNull(status)) {
            booleanBuilder.and(qEmployee.status.eq(status));
        }

        return booleanBuilder.getValue();
    }
}
