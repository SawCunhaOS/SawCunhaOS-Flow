
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

package br.com.sawcunhaos.organization.domain.access.login.specification;

import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de domínio da cadeia de aprovação de acesso (Story 3.2).
 */
public interface LoginApprovalRequestService {

    /** Busca a solicitação pelo id, com o aprovador do nível atual resolvido na hora (AC 10). 404 SCOS_LOGIN_APPROVAL_REQUEST_001 se não existir. */
    LoginApprovalRequestOutput findById(@NonNull Long id);

    /** Lista solicitações paginadas, filtrando por status/loginId quando informados (ambos opcionais). */
    Page<LoginApprovalRequestOutput> findAll(LoginApprovalRequestStatus status, Long loginId, @NonNull Pageable pageable);

    /**
     * Aprova ({@code decision=APPROVED}) ou rejeita ({@code decision=REJECTED}) a solicitação.
     * {@code actingUserHasSystemAccessPermission} é resolvido pelo chamador a partir do contexto de
     * segurança (mesmo mecanismo do {@code @PreAuthorize}) - o domínio não depende de Spring Security.
     *
     * @throws br.com.sawcunhaos.foundation.utils.exception.ScosException SCOS_LOGIN_APPROVAL_REQUEST_001 (404) se a solicitação não existir.
     * @throws br.com.sawcunhaos.foundation.utils.exception.ScosException SCOS_LOGIN_016 (404) se o Login de quem decide não for encontrado.
     * @throws br.com.sawcunhaos.foundation.utils.exception.ScosException SCOS_LOGIN_017 (422) se quem decide não for elegível (AC 3).
     * @throws br.com.sawcunhaos.foundation.utils.exception.ScosException SCOS_LOGIN_018 (422) se o Funcionário vinculado não estiver mais ACTIVE (AC 5).
     */
    void decide(@NonNull Long requestId, @NonNull LoginApprovalRequestStatus decision, @NonNull Long reasonId, String observation, boolean actingUserHasSystemAccessPermission);

}
