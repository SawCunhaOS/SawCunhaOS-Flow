
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

package br.com.sawcunhaos.organization.domain.corporate.company.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CompanyRepository extends BaseJpaRepository<Company, Long>, JpaSpecificationExecutor<Company>, QuerydslPredicateExecutor<Company> {
    QCompany company = QCompany.company;

    default Page<Company> findAllNotDisabled(Pageable pageable) {
        return findAll(company.status.ne(StatusCompany.DISABLED), pageable);
    }

    default Optional<Company> findNotDisabledById(Long id) {
        return findOne(
                company.status.ne(StatusCompany.DISABLED)
                        .and(company.id.eq(id))
        );
    }

    List<Company> findAll();

    /**
     * Verifica se existe uma empresa com o CNPJ informado.
     *
     * @param taxIdentifier CNPJ formatado ou não
     * @return true se existe, false caso contrário
     */
    default boolean existsByTaxIdentifier(String taxIdentifier) {
        return exists(
                company.taxIdentifier.cnpj.eq(taxIdentifier)
        );
    }

    /**
     * Verifica duplicidade de CNPJ excluindo a própria empresa — usado na atualização.
     *
     * @param taxIdentifier CNPJ formatado ou não
     * @param companyId     empresa a ser excluída da checagem
     * @return true se outra empresa já usa o CNPJ, false caso contrário
     */
    default boolean existsByTaxIdentifierAndNotId(String taxIdentifier, Long companyId) {
        return exists(
                company.taxIdentifier.cnpj.eq(taxIdentifier).and(company.id.ne(companyId))
        );
    }

    /**
     * Verifica se existe uma empresa-mãe com o ID informado.
     *
     * @param parentCompanyId ID da empresa-mãe
     * @return true se existe, false caso contrário
     */
    boolean existsByParentCompanyId(Long parentCompanyId);

    /**
     * Verifica se existe alguma empresa com o status informado.
     *
     * @param companyId ID da empresa para nao ser validado
     * @param status Status a verificar
     * @return true se existe, false caso contrário
     */
    default boolean existsByStatus(Long companyId, StatusCompany status) {
        return exists(company.id.ne(companyId).and(company.status.eq(status)));
    }

    /**
     * Verifica se existe alguma empresa que usa o CNAE informado como CNAE principal.
     *
     * @param cnaeId ID do CNAE
     * @return true se existe, false caso contrário
     */
    default boolean existsByCnaePrincipalId(Long cnaeId) {
        return exists(company.cnaePrincipal.id.eq(cnaeId));
    }

    /**
     * Verifica se existe alguma empresa que usa a natureza jurídica informada (qualquer status).
     *
     * @param legalNatureId ID da natureza jurídica
     * @return true se existe, false caso contrário
     */
    default boolean existsByLegalNatureId(Long legalNatureId) {
        return exists(company.legalNature.id.eq(legalNatureId));
    }

}
