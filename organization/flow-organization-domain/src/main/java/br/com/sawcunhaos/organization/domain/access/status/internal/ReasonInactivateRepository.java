
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
 * Repositório JPA/QueryDSL de {@link ReasonInactivate}.
 */
@Repository
public interface ReasonInactivateRepository extends BaseJpaRepository<ReasonInactivate, Long>, JpaSpecificationExecutor<ReasonInactivate>, QuerydslPredicateExecutor<ReasonInactivate> {
    QReasonInactivate qReasonInactivate = QReasonInactivate.reasonInactivate;

    Page<ReasonInactivate> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link ReasonInactivate} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qReasonInactivate.code.eq(code))
                      .and(qReasonInactivate.entityType.eq(entityType));

        return exists(booleanBuilder.getValue());
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code reasonInactivateId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonInactivateId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qReasonInactivate.code.eq(code))
                      .and(qReasonInactivate.entityType.eq(entityType))
                      .and(qReasonInactivate.id.ne(reasonInactivateId));

        return exists(booleanBuilder.getValue());
    }

    /**
     * Lista paginada, filtrando por {@code entityType}/{@code active} quando informados.
     */
    default Page<ReasonInactivate> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (Objects.nonNull(active)) {
            booleanBuilder.and(qReasonInactivate.active.eq(active));
        }
        if (Objects.nonNull(entityType)) {
            booleanBuilder.and(qReasonInactivate.entityType.eq(entityType));
        }

        return findAll(booleanBuilder.getValue(), pageable);
    }
}
