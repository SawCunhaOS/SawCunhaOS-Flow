
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

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicateCompanyIdAndPositionIdAndStatus;
import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicateEmail;
import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicatePositionId;
import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicatePositionIdAndStatus;
import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicateTaxIdentifier;
import static br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryPredicates.predicateTaxIdentifierAndStatus;


@Repository
public interface EmployeeQueryRepository extends BaseJpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee>, QuerydslPredicateExecutor<Employee> {

    Page<Employee> findAll(Pageable pageable);

    Optional<Employee> findById(Long id);

    default boolean existsByPositionId(Long positionId) {
        return exists(predicatePositionId(positionId));
    }

    default boolean existsByPositionIdAndStatus(Long positionId, StatusEmployee status) {
        return exists(predicatePositionIdAndStatus(positionId, status));
    }

    default boolean existsByTaxIdentifier(String taxIdentifier) {
        return exists(predicateTaxIdentifier(taxIdentifier));
    }

    default boolean existsByEmail(String email) {
        return exists(predicateEmail(email));
    }

    default Optional<Employee> findByTaxIdentifierAndStatus(String taxIdentifier, StatusEmployee status) {
        return findOne(predicateTaxIdentifierAndStatus(taxIdentifier, status));
    }

    default Page<Employee> findAllFiltered(Long companyId, Long positionId, StatusEmployee status, Pageable pageable) {
        if (Objects.isNull(companyId) && Objects.isNull(positionId) && Objects.isNull(status)) {
            return findAll(pageable);
        }

        return findAll(predicateCompanyIdAndPositionIdAndStatus(companyId, positionId, status), pageable);
    }

}
