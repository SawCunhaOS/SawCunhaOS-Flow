
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
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * Repositório JPA/QueryDSL de {@link ReasonDisable}.
 */
@Repository
public interface ReasonDisableRepository extends BaseJpaRepository<ReasonDisable, Long>, JpaSpecificationExecutor<ReasonDisable>, QuerydslPredicateExecutor<ReasonDisable> {
    QReasonDisable qReasonDisable = QReasonDisable.reasonDisable;

    Page<ReasonDisable> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link ReasonDisable} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qReasonDisable.code.eq(code))
                      .and(qReasonDisable.entityType.eq(entityType));

        return exists(booleanBuilder.getValue());
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code reasonDisableId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonDisableId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qReasonDisable.code.eq(code))
                      .and(qReasonDisable.entityType.eq(entityType))
                      .and(qReasonDisable.id.ne(reasonDisableId));

        return exists(booleanBuilder.getValue());
    }

    /**
     * Lista paginada, filtrando por {@code entityType}/{@code active} quando informados.
     */
    default Page<ReasonDisable> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (Objects.nonNull(active)) {
            booleanBuilder.and(qReasonDisable.active.eq(active));
        }
        if (Objects.nonNull(entityType)) {
            booleanBuilder.and(qReasonDisable.entityType.eq(entityType));
        }

        return findAll(booleanBuilder.getValue(), pageable);
    }
}
