
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
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestProfileChangeKind;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
import br.com.sawcunhaos.organization.domain.access.profile.internal.LoginProfile;
import br.com.sawcunhaos.organization.domain.access.profile.internal.LoginProfilePk;
import br.com.sawcunhaos.organization.domain.access.profile.internal.LoginProfileRepository;
import br.com.sawcunhaos.organization.domain.access.status.internal.LoginStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.LoginStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxEvent;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxEventRepository;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxEventStatus;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxTopic;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_016;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_017;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_018;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_APPROVAL_REQUEST_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_APPROVAL_REQUEST_003;

/**
 * Implementação de {@link LoginApprovalRequestService} — regras de negócio da cadeia de aprovação
 * de acesso (Story 3.2) que cruzam {@code Login}, {@code LoginApprovalRequest},
 * {@code LoginStatusHistory} e {@code OutboxEvent}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class LoginApprovalRequestServiceBean implements LoginApprovalRequestService {

    private static final String OUTBOX_TOPIC_KEYCLOAK_LOGIN_SYNC = "KEYCLOAK_LOGIN_SYNC";

    private final LoginApprovalRequestRepository loginApprovalRequestRepository;
    private final LoginRepository loginRepository;
    private final LoginProfileRepository loginProfileRepository;
    private final LoginStatusHistoryRepository loginStatusHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxTopicRepository outboxTopicRepository;
    private final EmployeeService employeeService;
    private final ScosUserAuthentication scosUserAuthentication;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public LoginApprovalRequestOutput findById(@NonNull Long id) {
        log.info("Find Login Approval Request by Id: {}", id);
        return toOutput(findRequestById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginApprovalRequestOutput> findAll(LoginApprovalRequestStatus status, Long loginId, @NonNull Pageable pageable) {
        log.info("Find All Login Approval Requests, Status: {}, LoginId: {}", status, loginId);
        return loginApprovalRequestRepository.findAllFiltered(status, loginId, pageable)
                .map(this::toOutput);
    }

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public void decide(@NonNull Long requestId, @NonNull LoginApprovalRequestStatus decision, @NonNull Long reasonId, String observation, boolean actingUserHasSystemAccessPermission) {
        log.info("Decide Login Approval Request: {}, Decision: {}", requestId, decision);

        LoginApprovalRequest request = findRequestById(requestId);

        String user = scosUserAuthentication.findUserAuthentication();
        Login currentLogin = loginRepository.findByLogin(user)
                .orElseThrow(() -> new ScosException(SCOS_LOGIN_016));

        boolean isSelfApproval = request.getLogin().getId().equals(currentLogin.getId());
        assertEligibleToDecide(request, currentLogin, isSelfApproval, actingUserHasSystemAccessPermission);
        assertEmployeeStillActive(request.getLogin());

        Instant now = clock.instant();
        Login login = request.getLogin();
        boolean approved = decision == LoginApprovalRequestStatus.APPROVED;

        LoginStatusHistory history = switch (request.getRequestType()) {
            case CREATE_LOGIN -> approved ? login.approve(reasonId) : login.reject(reasonId);
            case REACTIVATE_LOGIN -> approved
                    ? (login.getStatus() == LoginStatus.INACTIVE ? login.activate(reasonId) : login.enable(reasonId))
                    : null; // rejeição de reativação não muda o Login (AC 5 da Story 3.3) - só a LoginApprovalRequest registra REJECTED
            case CHANGE_PROFILE -> {
                if (approved) {
                    applyProfileChange(request, login, user);
                }
                yield null; // troca de perfil não é transição de status - sem LoginStatusHistory (AC 1 da Story 3.4)
            }
        };

        if (history != null) {
            persistHistory(history, observation, user);
        }

        if (approved) {
            request.approve(currentLogin, isSelfApproval, now);
            // Saga Keycloak só existe para o ciclo de vida do próprio Login (FR-6/FR-25) - CHANGE_PROFILE
            // nunca produz history (sempre null aqui), então esta condição já exclui esse requestType.
            if (history != null) {
                openKeycloakSyncOutboxEvent(login, user, now);
            }
        } else {
            request.reject(currentLogin, isSelfApproval, now);
        }

        loginApprovalRequestRepository.update(request);
    }

    private LoginApprovalRequest findRequestById(Long id) {
        return loginApprovalRequestRepository.findById(id)
                .orElseThrow(() -> new ScosException(SCOS_LOGIN_APPROVAL_REQUEST_001));
    }

    /**
     * @throws ScosException SCOS_LOGIN_017 se quem decide não for o aprovador resolvido no nível
     * atual nem detiver {@code APPROVE_SYSTEM_ACCESS} - a única exceção é a válvula de última
     * instância (AC 3): titular de {@code APPROVE_SYSTEM_ACCESS} pode decidir mesmo em auto-aprovação.
     */
    private void assertEligibleToDecide(LoginApprovalRequest request, Login currentLogin, boolean isSelfApproval, boolean actingUserHasSystemAccessPermission) {
        if (isSelfApproval) {
            if (!actingUserHasSystemAccessPermission) {
                throw new ScosException(SCOS_LOGIN_017);
            }
            return;
        }

        boolean isResolvedApprover = LoginApprovalChainResolver.resolveApproverEmployee(request.getCurrentLevel(), request.getLogin())
                .map(approver -> currentLogin.getEmployee() != null && approver.getId().equals(currentLogin.getEmployee().getId()))
                .orElse(false);

        if (!isResolvedApprover && !actingUserHasSystemAccessPermission) {
            throw new ScosException(SCOS_LOGIN_017);
        }
    }

    /**
     * @throws ScosException SCOS_LOGIN_018 se o Funcionário vinculado ao Login já não estiver ACTIVE.
     * Reverificado aqui (não só na criação, Story 3.1) porque pode ter sido desligado entre a
     * criação da solicitação e a decisão.
     */
    private void assertEmployeeStillActive(Login login) {
        if (login.getEmployee() == null) {
            return;
        }
        if (employeeService.findById(login.getEmployee().getId()).status() != StatusEmployee.ACTIVE) {
            throw new ScosException(SCOS_LOGIN_018);
        }
    }

    /**
     * Aplica a troca de Perfil aprovada (Story 3.4) - troca direta de FK ({@code SET_PRIMARY}) ou
     * nova linha em {@code SCOS_LOGIN_PROFILE} ({@code ADD_ADDITIONAL}). Não é transição de status
     * do Login, por isso não passa por método validado como {@code activate}/{@code approve}.
     */
    private void applyProfileChange(LoginApprovalRequest request, Login login, String user) {
        if (request.getProfileChangeKind() == LoginApprovalRequestProfileChangeKind.SET_PRIMARY) {
            login.setProfile(request.getRequestedProfile());
            loginRepository.merge(login);
        } else {
            loginProfileRepository.merge(LoginProfile.builder()
                    .id(LoginProfilePk.builder().loginId(login.getId()).profileId(request.getRequestedProfile().getId()).build())
                    .userAt(user)
                    .build());
        }
    }

    private void persistHistory(LoginStatusHistory history, String observation, String user) {
        history.setObservation(observation);
        history.setUserAt(user);
        loginStatusHistoryRepository.merge(history);
    }

    /** Guarda de escopo (Story 3.1/3.2): só grava a linha do Outbox - sem dispatcher real. */
    private void openKeycloakSyncOutboxEvent(Login login, String user, Instant now) {
        OutboxTopic topic = outboxTopicRepository.findById(OUTBOX_TOPIC_KEYCLOAK_LOGIN_SYNC)
                .orElseThrow(() -> new ScosException(SCOS_LOGIN_APPROVAL_REQUEST_003));

        OutboxEvent event = OutboxEvent.builder()
                .eventHash(sha256Hex(OUTBOX_TOPIC_KEYCLOAK_LOGIN_SYNC + "|" + login.getId() + "|" + now))
                .topic(topic)
                .aggregateId(String.valueOf(login.getId()))
                .payload("{\"loginId\":" + login.getId() + "}")
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .maxRetries(topic.getDefaultMaxRetries())
                .requesting(user)
                .updatedAt(now)
                .userAt(user)
                .build();

        outboxEventRepository.merge(event);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    /**
     * {@code currentApproverEmployee*} calculado na hora via {@link LoginApprovalChainResolver}
     * (AC 10) - nunca lido de coluna persistida, já que o organograma pode mudar entre a abertura
     * da solicitação e a consulta.
     */
    private LoginApprovalRequestOutput toOutput(LoginApprovalRequest request) {
        Optional<Employee> approver = LoginApprovalChainResolver.resolveApproverEmployee(request.getCurrentLevel(), request.getLogin());

        return LoginApprovalRequestOutput.builder()
                .id(request.getId())
                .loginId(request.getLogin().getId())
                .requestType(request.getRequestType())
                .currentLevel(request.getCurrentLevel())
                .status(request.getStatus())
                .escalationPolicy(request.getEscalationPolicy())
                .slaDeadline(request.getSlaDeadline())
                .decidedByLoginId(request.getDecidedByLogin() != null ? request.getDecidedByLogin().getId() : null)
                .decidedAt(request.getDecidedAt())
                .createdAt(request.getCreatedAt())
                .currentApproverEmployeeId(approver.map(Employee::getId).orElse(null))
                .currentApproverEmployeeName(approver.map(Employee::getName).orElse(null))
                .build();
    }
}
