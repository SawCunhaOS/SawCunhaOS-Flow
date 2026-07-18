
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

/**
 * Repositório JPA/QueryDSL de {@link LegalNature}. Unicidade de {@code code} é global na tabela
 * {@code SCOS_LEGAL_NATURE} (respaldada pela constraint {@code UK_CODE_SCOS_LEGAL_NATURE}).
 */
@Repository
public interface LegalNatureRepository extends BaseJpaRepository<LegalNature, Long>, JpaSpecificationExecutor<LegalNature>, QuerydslPredicateExecutor<LegalNature> {
    QLegalNature qLegalNature = QLegalNature.legalNature;

    Page<LegalNature> findAll(Pageable pageable);

    /**
     * Verifica se já existe uma {@link LegalNature} com o {@code code} informado.
     */
    default boolean existsByCode(String code) {
        return exists(qLegalNature.code.eq(code));
    }

    /**
     * Verifica duplicidade de {@code code} excluindo o próprio {@code legalNatureId} — usado na atualização.
     */
    default boolean existsByCodeAndNotId(String code, Long legalNatureId) {
        return exists(qLegalNature.code.eq(code).and(qLegalNature.id.ne(legalNatureId)));
    }
}
