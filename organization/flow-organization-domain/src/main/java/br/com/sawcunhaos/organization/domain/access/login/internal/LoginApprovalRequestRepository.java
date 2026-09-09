
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestPredicates.predicateStatusAndLoginId;

@Repository
public interface LoginApprovalRequestRepository extends BaseJpaRepository<LoginApprovalRequest, Long>, JpaSpecificationExecutor<LoginApprovalRequest>, QuerydslPredicateExecutor<LoginApprovalRequest> {

    Page<LoginApprovalRequest> findAll(Pageable pageable);

    Optional<LoginApprovalRequest> findByLoginIdAndStatus(Long loginId, LoginApprovalRequestStatus status);

    List<LoginApprovalRequest> findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus status, Instant deadline);

    /**
     * Guarda "uma solicitação por episódio de licença" (Story 3.5 AC 4) - sem FK dedicada, compara
     * datas. {@code createdAt} é {@code LocalDateTime} (dívida da {@code BaseEntity} da foundation)
     * - {@code after} (Instant) é convertido em UTC antes de comparar.
     */
    default boolean existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(Long loginId, LoginApprovalRequestType requestType, LoginApprovalRequestEscalationPolicy escalationPolicy, Instant after) {
        QLoginApprovalRequest q = QLoginApprovalRequest.loginApprovalRequest;
        return exists(q.login.id.eq(loginId)
                .and(q.requestType.eq(requestType))
                .and(q.escalationPolicy.eq(escalationPolicy))
                .and(q.createdAt.after(after)));
    }

    default Page<LoginApprovalRequest> findAllFiltered(LoginApprovalRequestStatus status, Long loginId, Pageable pageable) {
        if (status == null && loginId == null) {
            return findAll(pageable);
        }

        return findAll(predicateStatusAndLoginId(status, loginId), pageable);
    }

}
