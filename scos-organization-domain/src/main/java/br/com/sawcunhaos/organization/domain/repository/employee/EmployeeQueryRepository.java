
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

package br.com.sawcunhaos.organization.domain.repository.employee;

import br.com.sawcunhaos.organization.domain.model.employee.Employee;
import br.com.sawcunhaos.organization.domain.model.employee.QEmployee;
import com.querydsl.core.BooleanBuilder;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface EmployeeQueryRepository extends BaseJpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee>, QuerydslPredicateExecutor<Employee> {

    QEmployee qEmployee = QEmployee.employee;

    Page<Employee> findAll(Pageable pageable);

    Optional<Employee> findById(Long id);

    default boolean existsByPositionId(Long positionId) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qEmployee.position.id.eq(positionId));

        return exists(booleanBuilder.getValue());
    }

}
