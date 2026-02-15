
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

package br.com.sawcunhaos.organization.domain.repository.company;

import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.QCompany;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface CompanyRepository extends BaseJpaRepository<Company, Long>, JpaSpecificationExecutor<Company>, QuerydslPredicateExecutor<Company> {
    QCompany company = QCompany.company;

    default Page<Company> findAllNotDeleted(Pageable pageable) {
        return findAll(company.status.ne(StatusCompany.DISABLED), pageable);
    }

    default Optional<Company> findNotDeletedById(Long id) {
        return findOne(
                company.status.ne(StatusCompany.DISABLED)
                        .and(company.id.eq(id))
        );
    }

    List<Company> findAll();

    @Modifying
    @Query("""
    update Company c
    set    c.active = :active,
           c.status = :statusCompany
    where  c.id     = :companyId
    """)
    void updateCompanyStatusById(Long companyId, StatusCompany statusCompany, boolean active);


}
