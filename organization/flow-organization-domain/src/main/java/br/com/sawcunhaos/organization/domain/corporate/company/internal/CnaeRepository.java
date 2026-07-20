
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
 * Repositório JPA/QueryDSL de {@link Cnae}. Unicidade de {@code code} é global na tabela
 * {@code SCOS_CNAE} (respaldada pela constraint {@code UK_CODE_SCOS_CNAE}).
 */
@Repository
public interface CnaeRepository extends BaseJpaRepository<Cnae, Long>, JpaSpecificationExecutor<Cnae>, QuerydslPredicateExecutor<Cnae> {
    QCnae qCnae = QCnae.cnae;

    Page<Cnae> findAll(Pageable pageable);

    /**
     * Verifica se já existe um {@link Cnae} com o {@code code} informado.
     */
    default boolean existsByCode(String code) {
        return exists(qCnae.code.eq(code));
    }

    /**
     * Verifica duplicidade de {@code code} excluindo o próprio {@code cnaeId} — usado na atualização.
     */
    default boolean existsByCodeAndNotId(String code, Long cnaeId) {
        return exists(qCnae.code.eq(code).and(qCnae.id.ne(cnaeId)));
    }
}
