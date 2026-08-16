
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

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Objects;

import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivatePredicates.predicateCodeAndEntityType;
import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivatePredicates.predicateCodeAndEntityTypeAndNotId;
import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivatePredicates.predicateEntityTypeAndActive;

/**
 * Repositório JPA/QueryDSL de {@link ReasonInactivate}.
 */
@Repository
public interface ReasonInactivateRepository extends BaseJpaRepository<ReasonInactivate, Long>, JpaSpecificationExecutor<ReasonInactivate>, QuerydslPredicateExecutor<ReasonInactivate> {

    Page<ReasonInactivate> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link ReasonInactivate} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        return exists(predicateCodeAndEntityType(code, entityType));
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code reasonInactivateId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonInactivateId) {
        return exists(predicateCodeAndEntityTypeAndNotId(code, entityType, reasonInactivateId));
    }

    /**
     * Lista paginada, filtrando por {@code entityType}/{@code active} quando informados.
     */
    default Page<ReasonInactivate> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        return findAll(predicateEntityTypeAndActive(entityType, active), pageable);
    }
}
