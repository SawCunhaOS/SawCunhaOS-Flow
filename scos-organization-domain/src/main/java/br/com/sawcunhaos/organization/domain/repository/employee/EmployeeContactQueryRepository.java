
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

import br.com.sawcunhaos.organization.domain.model.employee.EmployeeContact;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface EmployeeContactQueryRepository extends BaseJpaRepository<EmployeeContact, Long>, JpaSpecificationExecutor<EmployeeContact>, QuerydslPredicateExecutor<EmployeeContact> {

}
