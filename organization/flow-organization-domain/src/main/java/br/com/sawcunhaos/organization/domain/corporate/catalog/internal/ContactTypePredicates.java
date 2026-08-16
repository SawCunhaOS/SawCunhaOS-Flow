
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

import java.util.Objects;

final class ContactTypePredicates {
    private final static QContactType qContactType = QContactType.contactType;

    static Predicate predicateCodeAndEntityType(String code, EntityType entityType) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qContactType.code.eq(code))
                      .and(qContactType.entityType.eq(entityType));

        return booleanBuilder.getValue();
    }

    static Predicate predicateCodeAndEntityAndNotId(String code, EntityType entityType, Long contactTypeId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qContactType.code.eq(code))
                      .and(qContactType.id.ne(contactTypeId))
                      .and(qContactType.entityType.eq(entityType));

        return booleanBuilder.getValue();
    }

    static Predicate  predicateEntityTypeAndActive(EntityType entityType, Boolean active) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(active)) {
            booleanBuilder.and(qContactType.active.eq(active));
        }
        if (Objects.nonNull(entityType)) {
            booleanBuilder.and(qContactType.entityType.eq(entityType));
        }

        return booleanBuilder.getValue();
    }
}
