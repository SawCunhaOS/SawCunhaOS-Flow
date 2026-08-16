
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

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionPredicates.predicateCode;
import static br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionPredicates.predicateCodeAndNotId;
import static br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionPredicates.predicateDepartmentId;
import static br.com.sawcunhaos.organization.domain.corporate.position.internal.PositionPredicates.predicateDepartmentIdAndActive;


@Repository
public interface PositionRepository extends BaseJpaRepository<Position, Long>, JpaSpecificationExecutor<Position>, QuerydslPredicateExecutor<Position> {

    Page<Position> findAll(Pageable pageable);

    Optional<Position> findById(Long id);

    default boolean existsByDepartmentId(Long departmentId) {
        return exists(predicateDepartmentId(departmentId));
    }

    default boolean existsByCodeAndNotId(Long positionId, String code) {
        return exists(predicateCodeAndNotId(positionId, code));
    }

    default boolean existsByCode(String code) {
        return exists(predicateCode(code));
    }

    default Page<Position> findAllFiltered(Long departmentId, Boolean active, Pageable pageable) {
        if (departmentId == null && active == null) {
            return findAll(pageable);
        }

        return findAll(predicateDepartmentIdAndActive(departmentId, active), pageable);
    }
}
