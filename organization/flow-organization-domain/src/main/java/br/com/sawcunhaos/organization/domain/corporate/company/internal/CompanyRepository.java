
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
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

    /**
     * Sobe a cadeia de {@code PARENT_COMPANY_ID} a partir de {@code candidateParentCompanyId} via
     * CTE recursiva (AD-7) e verifica se {@code companyId} aparece nela — nesse caso, atribuir
     * {@code candidateParentCompanyId} como pai de {@code companyId} fecharia um ciclo.
     *
     * @return {@code 1} se fecharia ciclo, {@code 0} caso contrário.
     */
    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT COMPANY_ID, PARENT_COMPANY_ID
                FROM scos.SCOS_COMPANY
                WHERE COMPANY_ID = :candidateParentCompanyId
                UNION ALL
                SELECT c.COMPANY_ID, c.PARENT_COMPANY_ID
                FROM scos.SCOS_COMPANY c
                INNER JOIN ancestors a ON c.COMPANY_ID = a.PARENT_COMPANY_ID
            )
            SELECT CASE WHEN EXISTS (SELECT 1 FROM ancestors WHERE COMPANY_ID = :companyId) THEN 1 ELSE 0 END
            """, nativeQuery = true)
    int wouldCreateCycleFlag(@Param("companyId") Long companyId, @Param("candidateParentCompanyId") Long candidateParentCompanyId);

    /**
     * Verifica se atribuir {@code candidateParentCompanyId} como pai de {@code companyId} fecharia
     * um ciclo (direto ou indireto) na hierarquia de empresas.
     *
     * @param companyId               empresa que receberia o novo pai
     * @param candidateParentCompanyId candidato a pai
     * @return {@code true} se fecharia ciclo, {@code false} caso contrário
     */
    default boolean wouldCreateCycle(Long companyId, Long candidateParentCompanyId) {
        return wouldCreateCycleFlag(companyId, candidateParentCompanyId) == 1;
    }

    /**
     * Verifica se existe outra Empresa matriz (parentCompany nulo) ativa, excluindo a própria.
     *
     * @param companyId empresa a ser excluída da checagem
     * @return true se existe outra matriz ACTIVE, false caso contrário
     */
    default boolean existsOtherActiveMatrix(Long companyId) {
        return exists(
                company.parentCompany.isNull()
                        .and(company.status.eq(StatusCompany.ACTIVE))
                        .and(company.id.ne(companyId))
        );
    }

    /**
     * Desce a árvore de {@code PARENT_COMPANY_ID} a partir de {@code companyId} via CTE recursiva
     * (AD-7) e verifica se algum descendente (qualquer nível) está {@code ACTIVE}.
     *
     * @return {@code 1} se existe descendente ativo, {@code 0} caso contrário.
     */
    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT COMPANY_ID, STATUS
                FROM scos.SCOS_COMPANY
                WHERE PARENT_COMPANY_ID = :companyId
                UNION ALL
                SELECT c.COMPANY_ID, c.STATUS
                FROM scos.SCOS_COMPANY c
                INNER JOIN descendants d ON c.PARENT_COMPANY_ID = d.COMPANY_ID
            )
            SELECT CASE WHEN EXISTS (SELECT 1 FROM descendants WHERE STATUS = 'ACTIVE') THEN 1 ELSE 0 END
            """, nativeQuery = true)
    int hasActiveDescendantFlag(@Param("companyId") Long companyId);

    /**
     * Verifica se a Empresa {@code companyId} tem alguma filial {@code ACTIVE} em qualquer nível
     * da sua subárvore (filha direta ou descendente indireto).
     */
    default boolean hasActiveDescendant(Long companyId) {
        return hasActiveDescendantFlag(companyId) == 1;
    }

    /**
     * Lista paginada, filtrando por {@code status}/{@code name} (contém, sem distinguir maiúsculas) quando informados.
     */
    default Page<Company> findAllFiltered(StatusCompany status, String name, Pageable pageable) {
        return findAll(CompanyPredicates.predicateStatusAndName(status, name), pageable);
    }

}
