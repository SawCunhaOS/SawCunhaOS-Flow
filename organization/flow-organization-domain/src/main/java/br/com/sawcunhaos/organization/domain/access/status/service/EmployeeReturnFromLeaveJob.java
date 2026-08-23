
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

package br.com.sawcunhaos.organization.domain.access.status.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestEscalationPolicy;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.service.BusinessDayCalculator;
import br.com.sawcunhaos.organization.domain.access.login.service.LoginApprovalChainResolver;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Abre, sozinho, a {@code LoginApprovalRequest} de reativação (Story 3.5) na data prevista de
 * retorno de uma licença/férias ({@code EmployeeStatusHistory.expectedReturnDate}, Story 2.4) -
 * ainda exige aprovação humana, na mesma cadeia de 3 níveis (Story 3.2/3.3); o Login não vira
 * {@code ACTIVE} sozinho.
 *
 * <p>1x/dia, cedo - {@code expectedReturnDate} é {@code LocalDate} (sem hora), sem ganho em rodar
 * mais vezes. Consulta simples (N linhas com {@code expectedReturnDate <= hoje}, cada uma
 * confirmada 1 a 1 como a mais recente do Funcionário) em vez de uma query dedicada - suficiente
 * para o volume atual.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeReturnFromLeaveJob {

    private final EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    private final LoginApprovalRequestRepository loginApprovalRequestRepository;
    private final BusinessDayCalculator businessDayCalculator;
    private final Clock clock;

    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    @Transactional(rollbackFor = ScosException.class)
    public void run() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);

        List<EmployeeStatusHistory> candidates = employeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(today);
        log.info("Employee Return From Leave Job: {} candidate(s)", candidates.size());

        candidates.forEach(candidate -> processIfLatest(candidate, now));
    }

    /** AC 3 - só a transição mais recente do Funcionário conta; uma mais nova pode ter substituído a licença. */
    private void processIfLatest(EmployeeStatusHistory candidate, Instant now) {
        Employee employee = candidate.getEmployee();
        boolean isLatest = employeeStatusHistoryRepository.findTopByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .map(latest -> latest.getId().equals(candidate.getId()))
                .orElse(false);
        if (!isLatest) {
            return;
        }

        employee.getLogin().stream()
                .filter(login -> login.getStatus() == LoginStatus.INACTIVE || login.getStatus() == LoginStatus.BLOCKED)
                .forEach(login -> openReturnRequestIfAbsent(login, candidate, now));
    }

    /** AC 4 - uma solicitação por episódio de licença: comparada por data, sem FK dedicada de rastreio. */
    private void openReturnRequestIfAbsent(Login login, EmployeeStatusHistory candidate, Instant now) {
        boolean alreadyRequested = loginApprovalRequestRepository.existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(
                login.getId(), LoginApprovalRequestType.REACTIVATE_LOGIN, LoginApprovalRequestEscalationPolicy.AUTO_CANCEL, candidate.getCreatedAt());
        if (alreadyRequested) {
            return;
        }

        LoginApprovalRequestLevel firstLevel = LoginApprovalChainResolver.firstLevelFor(login)
                .orElse(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP);

        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .login(login)
                .requestedByLogin(null)
                .currentLevel(firstLevel)
                .escalationPolicy(LoginApprovalRequestEscalationPolicy.AUTO_CANCEL)
                .status(LoginApprovalRequestStatus.PENDING)
                .requestType(LoginApprovalRequestType.REACTIVATE_LOGIN)
                .isExceptionSelfApproval(false)
                .slaDeadline(businessDayCalculator.plusBusinessDays(now, 1, ZoneOffset.UTC))
                .build();
        request.updateAuditInfo("SYSTEM");

        loginApprovalRequestRepository.merge(request);
    }
}
