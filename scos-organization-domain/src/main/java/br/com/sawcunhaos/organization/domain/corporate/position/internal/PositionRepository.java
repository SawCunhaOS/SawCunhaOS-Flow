
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

import com.querydsl.core.BooleanBuilder;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface PositionRepository extends BaseJpaRepository<Position, Long>, JpaSpecificationExecutor<Position>, QuerydslPredicateExecutor<Position> {
    QPosition qPosition = QPosition.position;

    Page<Position> findAll(Pageable pageable);

    Optional<Position> findById(Long id);

    default boolean existsByDepartmentId(Long departmentId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qPosition.department.id.eq(departmentId));

        return exists(booleanBuilder.getValue());
    }

    default boolean existsByCodeAndNotId(Long positionId, String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qPosition.code.eq(code))
                .and(qPosition.id.ne(positionId));

        return exists(booleanBuilder.getValue());
    }

    default boolean existsByCode(String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qPosition.code.eq(code));

        return exists(booleanBuilder.getValue());
    }
}
