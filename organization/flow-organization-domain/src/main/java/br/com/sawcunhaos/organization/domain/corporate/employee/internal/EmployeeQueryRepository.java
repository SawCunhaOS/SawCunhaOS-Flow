
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

package br.com.sawcunhaos.organization.domain.corporate.employee.internal;

import com.querydsl.core.BooleanBuilder;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
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

    default boolean existsByPositionIdAndStatus(Long positionId, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        booleanBuilder.and(qEmployee.position.id.eq(positionId))
                .and(qEmployee.status.eq(status));

        return exists(booleanBuilder.getValue());
    }

    default boolean existsByTaxIdentifier(String taxIdentifier) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier));
        return exists(booleanBuilder.getValue());
    }

    default boolean existsByEmail(String email) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.email.email.eq(email));
        return exists(booleanBuilder.getValue());
    }

    default Optional<Employee> findByTaxIdentifierAndStatus(String taxIdentifier, StatusEmployee status) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(qEmployee.taxIdentifier.cpf.eq(taxIdentifier))
                .and(qEmployee.status.eq(status));
        return findOne(booleanBuilder.getValue());
    }

    default Page<Employee> findAllFiltered(Long companyId, Long positionId, StatusEmployee status, Pageable pageable) {
        if (Objects.isNull(companyId) && Objects.isNull(positionId) && Objects.isNull(status)) {
            return findAll(pageable);
        }

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (Objects.nonNull(companyId)) {
            booleanBuilder.and(qEmployee.company.id.eq(companyId));
        }
        if (Objects.nonNull(positionId)) {
            booleanBuilder.and(qEmployee.position.id.eq(positionId));
        }
        if (Objects.nonNull(status)) {
            booleanBuilder.and(qEmployee.status.eq(status));
        }

        return findAll(booleanBuilder.getValue(), pageable);
    }

}
