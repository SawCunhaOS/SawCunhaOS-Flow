
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
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Objects;

import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactTypePredicates.predicateCodeAndEntityAndNotId;
import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactTypePredicates.predicateCodeAndEntityType;
import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactTypePredicates.predicateEntityTypeAndActive;

/**
 * Repositório JPA/QueryDSL de {@link ContactType}.
 */
@Repository
public interface ContactTypeRepository extends BaseJpaRepository<ContactType, Long>, JpaSpecificationExecutor<ContactType>, QuerydslPredicateExecutor<ContactType> {

    Page<ContactType> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link ContactType} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        return exists(predicateCodeAndEntityType(code, entityType));
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code contactTypeId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityAndNotId(String code, EntityType entityType, Long contactTypeId) {
        return exists(predicateCodeAndEntityAndNotId(code, entityType, contactTypeId));
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado; sem filtro quando {@code null}.
     */
    default Page<ContactType> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        return findAll(predicateEntityTypeAndActive(entityType, active), pageable);
    }

    boolean existsByCode(String code);
}
