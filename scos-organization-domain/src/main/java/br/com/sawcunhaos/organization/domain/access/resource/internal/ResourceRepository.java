
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

package br.com.sawcunhaos.organization.domain.access.resource.internal;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface ResourceRepository extends BaseJpaRepository<Resource, UUID>, JpaSpecificationExecutor<Resource>, QuerydslPredicateExecutor<Resource> {

    /**
     * Upsert nativo condicional do resource. Insere quando o {@code CODE} não existe
     * (constraint {@code UK_CODE_SCOS_RESOURCE}); no conflito, o ramo {@code DO UPDATE}
     * só executa quando {@code ACTIVE} ou {@code DEFINITION_UPDATED_AT} mudam
     * ({@code IS DISTINCT FROM} cobre null-safety). Colunas de auditoria setadas no próprio SQL.
     *
     * @return número de linhas afetadas (0 quando o update condicional não dispara).
     */
    @Modifying
    @Query(value = """
            INSERT INTO scos.SCOS_RESOURCE
                (RESOURCE_ID, SYSTEM_ID, CODE, DESCRIPTION_PT, DESCRIPTION_EN,
                 RESOURCE_GROUP, SUB_GROUP, VERSION, DEFINITION_UPDATED_AT,
                 ACTIVE, CREATED_AT, UPDATED_AT, USER_AT)
            VALUES (gen_random_uuid(), :systemId, :code, :descriptionPt, :descriptionEn,
                    :resourceGroup, :subGroup, :version, :definitionUpdatedAt,
                    :active, NOW(), NOW(), :userAt)
            ON CONFLICT (CODE) DO UPDATE SET
                DESCRIPTION_PT        = EXCLUDED.DESCRIPTION_PT,
                DESCRIPTION_EN        = EXCLUDED.DESCRIPTION_EN,
                RESOURCE_GROUP        = EXCLUDED.RESOURCE_GROUP,
                SUB_GROUP             = EXCLUDED.SUB_GROUP,
                VERSION               = EXCLUDED.VERSION,
                DEFINITION_UPDATED_AT = EXCLUDED.DEFINITION_UPDATED_AT,
                ACTIVE                = EXCLUDED.ACTIVE,
                UPDATED_AT            = NOW(),
                USER_AT               = EXCLUDED.USER_AT
            WHERE  SCOS_RESOURCE.ACTIVE                IS DISTINCT FROM EXCLUDED.ACTIVE
               OR  SCOS_RESOURCE.DEFINITION_UPDATED_AT IS DISTINCT FROM EXCLUDED.DEFINITION_UPDATED_AT
            """, nativeQuery = true)
    int upsert(
            @Param("systemId") UUID systemId,
            @Param("code") String code,
            @Param("descriptionPt") String descriptionPt,
            @Param("descriptionEn") String descriptionEn,
            @Param("resourceGroup") String resourceGroup,
            @Param("subGroup") String subGroup,
            @Param("version") String version,
            @Param("definitionUpdatedAt") LocalDate definitionUpdatedAt,
            @Param("active") boolean active,
            @Param("userAt") String userAt
    );

}
