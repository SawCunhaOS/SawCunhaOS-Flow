
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

import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestEscalationPolicy;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code Clock.fixed} determinístico (padrão Story 0.2/0.3) - sem {@code sleep}, sem tempo real.
 */
@ExtendWith(MockitoExtension.class)
class LoginApprovalEscalationJobTest {

    // 2026-08-17 é segunda-feira
    private static final Instant NOW = Instant.parse("2026-08-17T09:00:00Z");

    @Mock
    private LoginApprovalRequestRepository loginApprovalRequestRepository;

    private final BusinessDayCalculator businessDayCalculator = new BusinessDayCalculator();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private LoginApprovalEscalationJob job() {
        return new LoginApprovalEscalationJob(loginApprovalRequestRepository, businessDayCalculator, clock);
    }

    /**
     * {@code update(S)} existe tanto em {@link BaseJpaRepository} quanto, desde Spring Data JPA 4,
     * em {@code JpaSpecificationExecutor.update(UpdateSpecification)} - javac não desambigua o
     * overload num mock do Mockito sem estreitar o tipo estático antes.
     */
    private BaseJpaRepository<LoginApprovalRequest, Long> asBaseJpaRepository() {
        return loginApprovalRequestRepository;
    }

    private Login employeeLoginWithoutManagerChain() {
        Employee employee = Employee.builder().id(1L).name("Jane Doe").build();
        return Login.builder().id(1L).login("jane.doe").type(LoginType.EMPLOYEE).employee(employee).build();
    }

    /** Departamento com gerente cadastrado - nível MANAGER resolve para uma pessoa. */
    private Login employeeLoginWithManagerChain() {
        Employee manager = Employee.builder().id(9L).name("Maria Manager").build();
        br.com.sawcunhaos.organization.domain.corporate.department.internal.Department department =
                br.com.sawcunhaos.organization.domain.corporate.department.internal.Department.builder()
                        .id(1L).code("TI").description("TI").manager(manager).build();
        br.com.sawcunhaos.organization.domain.corporate.position.internal.Position position =
                br.com.sawcunhaos.organization.domain.corporate.position.internal.Position.builder()
                        .id(1L).code("DEV").description("Dev").department(department).build();
        Employee employee = Employee.builder().id(1L).name("Jane Doe").position(position).build();
        return Login.builder().id(1L).login("jane.doe").type(LoginType.EMPLOYEE).employee(employee).build();
    }

    @Test
    void runShouldEscalateOverdueRequestToNextLevel() {
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(request));

        job().run();

        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.MANAGER);
        assertThat(request.getManagerEscalatedAt()).isEqualTo(NOW);
        assertThat(request.getSlaDeadline()).isEqualTo(businessDayCalculator.plusBusinessDays(NOW, 1, ZoneOffset.UTC));
        verify(asBaseJpaRepository()).update(request);
    }

    @Test
    void runShouldNotEscalateWhenAlreadyAtSystemAccessGroup() {
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithoutManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(request));

        job().run();

        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP);
        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void runShouldDoNothingWhenNoRequestIsOverdue() {
        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of());

        job().run();

        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void runShouldEscalateEachOverdueRequestIndependently() {
        LoginApprovalRequest first = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();
        LoginApprovalRequest second = LoginApprovalRequest.builder()
                .id(2L).login(employeeLoginWithoutManagerChain())
                .currentLevel(LoginApprovalRequestLevel.MANAGER)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(first, second));

        job().run();

        ArgumentCaptor<LoginApprovalRequest> captor = ArgumentCaptor.forClass(LoginApprovalRequest.class);
        verify(asBaseJpaRepository(), org.mockito.Mockito.times(2)).update(captor.capture());
        assertThat(captor.getAllValues()).extracting(LoginApprovalRequest::getCurrentLevel)
                .containsExactly(LoginApprovalRequestLevel.MANAGER, LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP);
    }

    @Test
    void runShouldCancelAutoCancelRequestPastTheFiveBusinessDayCeilingInsteadOfEscalating() {
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .escalationPolicy(LoginApprovalRequestEscalationPolicy.AUTO_CANCEL)
                .build();
        request.setCreatedAt(LocalDateTime.parse("2026-07-01T09:00:00"));

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(request));

        job().run();

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.CANCELLED);
        assertThat(request.getDecidedAt()).isEqualTo(NOW);
        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.SUPERVISOR);
        verify(asBaseJpaRepository()).update(request);
    }

    @Test
    void runShouldEscalateAutoCancelRequestNormallyWhenStillWithinTheFiveBusinessDayCeiling() {
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .escalationPolicy(LoginApprovalRequestEscalationPolicy.AUTO_CANCEL)
                .build();
        request.setCreatedAt(LocalDateTime.parse("2026-08-17T09:00:00"));

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(request));

        job().run();

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.PENDING);
        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.MANAGER);
        verify(asBaseJpaRepository()).update(request);
    }

    @Test
    void runShouldNeverCancelIndefiniteRequestRegardlessOfAge() {
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(employeeLoginWithManagerChain())
                .currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .escalationPolicy(LoginApprovalRequestEscalationPolicy.INDEFINITE)
                .build();
        request.setCreatedAt(LocalDateTime.parse("2026-01-01T09:00:00"));

        when(loginApprovalRequestRepository.findAllByStatusAndSlaDeadlineBefore(LoginApprovalRequestStatus.PENDING, NOW))
                .thenReturn(List.of(request));

        job().run();

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.PENDING);
        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.MANAGER);
        verify(asBaseJpaRepository()).update(request);
    }
}
