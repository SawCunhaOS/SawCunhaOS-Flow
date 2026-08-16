
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

import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressTypePredicates.predicateCodeAndEntityType;
import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressTypePredicates.predicateCodeAndEntityTypeAndNotId;
import static br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressTypePredicates.predicateEntityAndActive;

/**
 * Repositório JPA/QueryDSL de {@link AddressType}.
 */
@Repository
public interface AddressTypeRepository extends BaseJpaRepository<AddressType, Long>, JpaSpecificationExecutor<AddressType>, QuerydslPredicateExecutor<AddressType> {
    Page<AddressType> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link AddressType} com o {@code code} informado dentro do mesmo {@code entityType}.
     */
    default boolean existsByCodeAndEntityType(String code, EntityType entityType) {
        return exists(predicateCodeAndEntityType(code, entityType));
    }

    /**
     * Verifica duplicidade de {@code code}/{@code entityType} excluindo o próprio {@code addressTypeId} — usado na atualização.
     */
    default boolean existsByCodeAndEntityTypeAndNotId(String code, EntityType entityType, Long addressTypeId) {
        return exists(predicateCodeAndEntityTypeAndNotId(code, entityType, addressTypeId));
    }

    /**
     * Lista paginada, filtrando por {@code entityType} quando informado; sem filtro quando {@code null}.
     */
    default Page<AddressType> findAllFiltered(EntityType entityType, Boolean active, Pageable pageable) {
        if (Objects.isNull(entityType) && Objects.isNull(active)) {
            return findAll(pageable);
        }

        return findAll(predicateEntityAndActive(entityType, active), pageable);
    }
}
