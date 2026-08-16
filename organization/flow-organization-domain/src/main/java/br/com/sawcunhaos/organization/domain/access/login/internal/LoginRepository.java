
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

package br.com.sawcunhaos.organization.domain.access.login.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginPredicates.predicateTypeAndStatusAndEmployeeId;

@Repository
public interface LoginRepository extends BaseJpaRepository<Login, Long>, JpaSpecificationExecutor<Login>, QuerydslPredicateExecutor<Login> {

    Page<Login> findAll(Pageable pageable);

    Optional<Login> findByLogin(String login);

    default boolean existsByLogin(String login) {
        return exists(QLogin.login1.login.eq(login));
    }

    default Page<Login> findAllFiltered(LoginType type, LoginStatus status, Long employeeId, Pageable pageable) {
        if (type == null && status == null && employeeId == null) {
            return findAll(pageable);
        }

        return findAll(predicateTypeAndStatusAndEmployeeId(type, status, employeeId), pageable);
    }

}
