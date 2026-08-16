
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

final class DepartmentPredicates {
    private static final QDepartment qDepartment = QDepartment.department;

    static Predicate predicateCodeAndNotId(Long departmentId, String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qDepartment.code.eq(code))
                .and(qDepartment.id.ne(departmentId));
        return booleanBuilder.getValue();
    }

    static Predicate predicateCode(String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qDepartment.code.eq(code));
        return booleanBuilder.getValue();
    }

    static Predicate predicateActive(Boolean active) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qDepartment.active.eq(active));
        return booleanBuilder.getValue();
    }
}
