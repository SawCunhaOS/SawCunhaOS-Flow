
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VwAuthorityResponseRepository extends BaseJpaRepository<VwAuthorityResponse, Long> {
    Optional<VwAuthorityResponse> findByLogin(String login);

    /**
     * {@code permissions} é {@code text[]} - containment de array não é trivial em JPQL/QueryDSL
     * puro, por isso via {@code @Query} nativa (mesmo padrão de {@code CompanyRepository.wouldCreateCycleFlag}).
     * Reaproveita a mesma view que o {@code @PreAuthorize} já usa - não uma segunda forma de calcular
     * quem tem a permissão.
     */
    @Query(value = "SELECT * FROM scos.vw_authority_response WHERE :permission = ANY(permissions) AND status = 'ACTIVE'", nativeQuery = true)
    List<VwAuthorityResponse> findAllByPermission(@Param("permission") String permission);
}
