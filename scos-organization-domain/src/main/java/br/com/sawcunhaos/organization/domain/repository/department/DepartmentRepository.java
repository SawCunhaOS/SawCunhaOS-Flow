
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

package br.com.sawcunhaos.organization.domain.repository.department;

import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.model.department.QDepartment;
import com.querydsl.core.BooleanBuilder;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface DepartmentRepository extends BaseJpaRepository<Department, Long>, JpaSpecificationExecutor<Department>, QuerydslPredicateExecutor<Department> {
    QDepartment qDepartment = QDepartment.department;

    Page<Department> findAll(Pageable pageable);

    Optional<Department> findById(Long id);

    default boolean existsByCodeAndNotId(Long departmentId, String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qDepartment.code.eq(code))
                      .and(qDepartment.id.ne(departmentId));

        return exists(booleanBuilder.getValue());
    }
    default boolean existsByCode(String code) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qDepartment.code.eq(code));

        return exists(booleanBuilder.getValue());
    }

}
