
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

import br.com.sawcunhaos.organization.domain.model.company.CompanyAddress;
import br.com.sawcunhaos.organization.domain.model.company.CompanyAddressPk;
import br.com.sawcunhaos.organization.domain.model.company.QCompanyAddress;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CompanyAddressRepository extends BaseJpaRepository<CompanyAddress, CompanyAddressPk>, JpaSpecificationExecutor<CompanyAddress>, QuerydslPredicateExecutor<CompanyAddress> {
    QCompanyAddress qCompanyAddress = QCompanyAddress.companyAddress;

    default Page<CompanyAddress> findAll(final Long companyId, final Pageable pageable) {
        return findAll(
                qCompanyAddress.id.companyId.eq(companyId)
                        .and(qCompanyAddress.company.status.ne(StatusCompany.DISABLED)),
                pageable
        );
    }

    default Optional<CompanyAddress> findByCompanyAndAddress(final Long companyId, final Long companyIdAddress) {
        return findOne(
                qCompanyAddress.id.companyId.eq(companyId)
                        .and(qCompanyAddress.company.status.ne(StatusCompany.DISABLED))
                        .and(qCompanyAddress.id.companyIdAddress.eq(companyIdAddress))
        );
    }

    default void deleteByCompanyIdAndAddressId(final Long companyId, final Long companyIdAddress) {
        deleteById(new CompanyAddressPk(companyIdAddress, companyId));
    }
}
