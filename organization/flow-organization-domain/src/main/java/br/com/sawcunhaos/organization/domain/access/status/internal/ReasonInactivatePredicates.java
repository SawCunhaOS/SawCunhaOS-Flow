
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

import java.util.Objects;

final class ReasonInactivatePredicates {
    private static final QReasonInactivate qReasonInactivate = QReasonInactivate.reasonInactivate;

    static Predicate predicateCodeAndEntityType(String code, EntityType entityType) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qReasonInactivate.code.eq(code))
                .and(qReasonInactivate.entityType.eq(entityType));
        return booleanBuilder.getValue();
    }

    static Predicate predicateCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonInactivateId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qReasonInactivate.code.eq(code))
                .and(qReasonInactivate.entityType.eq(entityType))
                .and(qReasonInactivate.id.ne(reasonInactivateId));
        return booleanBuilder.getValue();
    }

    static Predicate predicateEntityTypeAndActive(EntityType entityType, Boolean active) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (Objects.nonNull(active)) {
            booleanBuilder.and(qReasonInactivate.active.eq(active));
        }
        if (Objects.nonNull(entityType)) {
            booleanBuilder.and(qReasonInactivate.entityType.eq(entityType));
        }
        return booleanBuilder.getValue();
    }
}
