
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

final class PositionPredicates {
    private static final QPosition qPosition = QPosition.position;

    static Predicate predicateDepartmentId(Long departmentId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qPosition.department.id.eq(departmentId));
        return booleanBuilder.getValue();
    }

    static Predicate predicateCodeAndNotId(Long positionId, String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qPosition.code.eq(code))
                .and(qPosition.id.ne(positionId));
        return booleanBuilder.getValue();
    }

    static Predicate predicateCode(String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qPosition.code.eq(code));
        return booleanBuilder.getValue();
    }

    static Predicate predicateDepartmentIdAndActive(Long departmentId, Boolean active) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (departmentId != null) {
            booleanBuilder.and(qPosition.department.id.eq(departmentId));
        }
        if (active != null) {
            booleanBuilder.and(qPosition.active.eq(active));
        }
        return booleanBuilder.getValue();
    }
}
