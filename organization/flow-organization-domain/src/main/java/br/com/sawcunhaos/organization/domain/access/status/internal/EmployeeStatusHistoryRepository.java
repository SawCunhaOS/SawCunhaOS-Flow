
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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
public interface EmployeeStatusHistoryRepository extends BaseJpaRepository<EmployeeStatusHistory, Long>, JpaSpecificationExecutor<EmployeeStatusHistory>, QuerydslPredicateExecutor<EmployeeStatusHistory> {

    Optional<EmployeeStatusHistory> findTopByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    default List<EmployeeStatusHistory> findAllByExpectedReturnDateLessThanEqual(LocalDate date) {
        Iterable<EmployeeStatusHistory> found = findAll(QEmployeeStatusHistory.employeeStatusHistory.expectedReturnDate.loe(date));
        return StreamSupport.stream(found.spliterator(), false).toList();
    }
}
