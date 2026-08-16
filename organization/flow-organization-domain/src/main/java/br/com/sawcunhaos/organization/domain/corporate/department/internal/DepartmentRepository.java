
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

package br.com.sawcunhaos.organization.domain.corporate.department.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.corporate.department.internal.DepartmentPredicates.predicateActive;
import static br.com.sawcunhaos.organization.domain.corporate.department.internal.DepartmentPredicates.predicateCode;
import static br.com.sawcunhaos.organization.domain.corporate.department.internal.DepartmentPredicates.predicateCodeAndNotId;


@Repository
public interface DepartmentRepository extends BaseJpaRepository<Department, Long>, JpaSpecificationExecutor<Department>, QuerydslPredicateExecutor<Department> {
    QDepartment qDepartment = QDepartment.department;

    Page<Department> findAll(Pageable pageable);

    Optional<Department> findById(Long id);

    default boolean existsByCodeAndNotId(Long departmentId, String code) {
        return exists(predicateCodeAndNotId(departmentId, code));
    }

    default boolean existsByCode(String code) {
        return exists(predicateCode(code));
    }

    default boolean existsByIdAndPositionsActive(Long departmentId) {
        return exists(
                qDepartment.id.eq(departmentId).and(
                        qDepartment.positions.any().active.isTrue()
                )
        );
    }

    default Page<Department> findAllFiltered(Boolean active, Pageable pageable) {
        if (active == null) {
            return findAll(pageable);
        }

        return findAll(predicateActive(active), pageable);
    }

}
