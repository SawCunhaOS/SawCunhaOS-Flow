
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Varre solicitações {@code PENDING} com {@code slaDeadline} vencido (1 dia útil sem decisão no
 * nível atual) e escala para o próximo nível da cadeia. Se já estiver em {@code SYSTEM_ACCESS_GROUP},
 * não faz nada - política {@code INDEFINITE} não cancela sozinha (AC 4).
 *
 * <p>Primeiro uso de {@code @Scheduled} do projeto - {@code @EnableScheduling} adicionado em
 * {@code ScosOrganizationApplication}. Frequência de 30 min - mesma ordem de grandeza do refresh
 * das views de autoridade.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginApprovalEscalationJob {

    private static final long INTERVAL_MS = 30 * 60 * 1000;

    private final LoginApprovalRequestRepository loginApprovalRequestRepository;
    private final BusinessDayCalculator businessDayCalculator;
    private final Clock clock;

    @Scheduled(fixedRate = INTERVAL_MS, initialDelay = INTERVAL_MS)
    @Transactional(rollbackFor = ScosException.class)
    public void run() {
        Instant now = clock.instant();
        List<LoginApprovalRequest> overdue = loginApprovalRequestRepository
                .findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, now);

        log.info("Login Approval Escalation Job: {} overdue request(s)", overdue.size());

        overdue.forEach(request -> LoginApprovalChainResolver.nextLevelAfter(request.getCurrentLevel(), request.getLogin())
                .ifPresent(next -> {
                    request.escalate(next, now, businessDayCalculator.plusBusinessDays(now, 1, ZoneOffset.UTC));
                    loginApprovalRequestRepository.update(request);
                }));
    }
}
