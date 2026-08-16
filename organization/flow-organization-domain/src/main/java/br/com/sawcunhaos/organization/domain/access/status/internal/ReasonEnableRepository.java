
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

import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnablePredicates.predicateCodeAndEntityType;
import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnablePredicates.predicateCodeAndEntityTypeAndNotId;
import static br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnablePredicates.predicateEntityTypeAndActive;

/**
 * Repositório JPA/QueryDSL de {@link ReasonEnable}.
 */
@Repository
public interface ReasonEnableRepository extends BaseJpaRepository<ReasonEnable, Long>, JpaSpecificationExecutor<ReasonEnable>, QuerydslPredicateExecutor<ReasonEnable> {

    Page<ReasonEnable> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link ReasonEnable} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        return exists(predicateCodeAndEntityType(code, entityType));
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code reasonEnableId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long reasonEnableId) {
        return exists(predicateCodeAndEntityTypeAndNotId(code, entityType, reasonEnableId));
    }

    /**
     * Lista paginada, filtrando por {@code entityType}/{@code active} quando informados.
     */
    default Page<ReasonEnable> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        return findAll(predicateEntityTypeAndActive(entityType, active), pageable);
    }
}
