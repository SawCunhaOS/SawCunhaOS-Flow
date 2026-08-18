
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

import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestEscalationPolicy;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.service.BusinessDayCalculator;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.EmployeeStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@code Clock.fixed} determinístico (padrão Story 0.2/0.3) - sem {@code sleep}, sem tempo real. */
@ExtendWith(MockitoExtension.class)
class EmployeeReturnFromLeaveJobTest {

    private static final Instant NOW = Instant.parse("2026-08-17T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-08-17");
    private static final Instant LEAVE_CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Mock
    private EmployeeStatusHistoryRepository employeeStatusHistoryRepository;
    @Mock
    private LoginApprovalRequestRepository loginApprovalRequestRepository;

    private final BusinessDayCalculator businessDayCalculator = new BusinessDayCalculator();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private EmployeeReturnFromLeaveJob job() {
        return new EmployeeReturnFromLeaveJob(employeeStatusHistoryRepository, loginApprovalRequestRepository, businessDayCalculator, clock);
    }

    private Login blockedLogin(Employee employee) {
        return Login.builder().id(1L).login("jane.doe").type(LoginType.EMPLOYEE).status(LoginStatus.BLOCKED).employee(employee).build();
    }

    private EmployeeStatusHistory leaveHistory(Employee employee) {
        return EmployeeStatusHistory.builder().id(100L).employee(employee).expectedReturnDate(TODAY).createdAt(LEAVE_CREATED_AT).build();
    }

    @Test
    void runShouldOpenApprovalRequestWhenReturnDateArrivesForEligibleLogin() {
        Employee employee = Employee.builder().id(10L).name("Jane Doe").build();
        Login login = blockedLogin(employee);
        employee.setLogin(Set.of(login));
        EmployeeStatusHistory candidate = leaveHistory(employee);

        when(employeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(TODAY)).thenReturn(List.of(candidate));
        when(employeeStatusHistoryRepository.findTopByEmployeeIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(candidate));
        when(loginApprovalRequestRepository.existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(
                1L, LoginApprovalRequestType.REACTIVATE_LOGIN, LoginApprovalRequestEscalationPolicy.AUTO_CANCEL, LEAVE_CREATED_AT))
                .thenReturn(false);
        lenient().when(loginApprovalRequestRepository.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        job().run();

        ArgumentCaptor<LoginApprovalRequest> captor = ArgumentCaptor.forClass(LoginApprovalRequest.class);
        verify(loginApprovalRequestRepository).merge(captor.capture());
        LoginApprovalRequest request = captor.getValue();
        assertThat(request.getLogin()).isEqualTo(login);
        assertThat(request.getRequestedByLogin()).isNull();
        assertThat(request.getRequestType()).isEqualTo(LoginApprovalRequestType.REACTIVATE_LOGIN);
        assertThat(request.getEscalationPolicy()).isEqualTo(LoginApprovalRequestEscalationPolicy.AUTO_CANCEL);
        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.PENDING);
        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP);
        assertThat(request.getSlaDeadline()).isEqualTo(businessDayCalculator.plusBusinessDays(NOW, 1, ZoneOffset.UTC));
        assertThat(request.getUserAt()).isEqualTo("SYSTEM");
    }

    @Test
    void runShouldNotOpenWhenAnotherTransitionIsMoreRecentThanTheLeave() {
        Employee employee = Employee.builder().id(10L).name("Jane Doe").build();
        Login login = blockedLogin(employee);
        employee.setLogin(Set.of(login));
        EmployeeStatusHistory candidate = leaveHistory(employee);
        EmployeeStatusHistory moreRecent = EmployeeStatusHistory.builder().id(101L).employee(employee).createdAt(Instant.parse("2026-08-10T10:00:00Z")).build();

        when(employeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(TODAY)).thenReturn(List.of(candidate));
        when(employeeStatusHistoryRepository.findTopByEmployeeIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(moreRecent));

        job().run();

        verify(loginApprovalRequestRepository, never()).merge(any());
    }

    @Test
    void runShouldNotOpenSecondRequestForTheSameLeaveEpisode() {
        Employee employee = Employee.builder().id(10L).name("Jane Doe").build();
        Login login = blockedLogin(employee);
        employee.setLogin(Set.of(login));
        EmployeeStatusHistory candidate = leaveHistory(employee);

        when(employeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(TODAY)).thenReturn(List.of(candidate));
        when(employeeStatusHistoryRepository.findTopByEmployeeIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(candidate));
        when(loginApprovalRequestRepository.existsByLoginIdAndRequestTypeAndEscalationPolicyAndCreatedAtAfter(
                1L, LoginApprovalRequestType.REACTIVATE_LOGIN, LoginApprovalRequestEscalationPolicy.AUTO_CANCEL, LEAVE_CREATED_AT))
                .thenReturn(true);

        job().run();

        verify(loginApprovalRequestRepository, never()).merge(any());
    }

    @Test
    void runShouldNotOpenWhenLoginIsAlreadyActive() {
        Employee employee = Employee.builder().id(10L).name("Jane Doe").build();
        Login activeLogin = Login.builder().id(1L).login("jane.doe").type(LoginType.EMPLOYEE).status(LoginStatus.ACTIVE).employee(employee).build();
        employee.setLogin(Set.of(activeLogin));
        EmployeeStatusHistory candidate = leaveHistory(employee);

        when(employeeStatusHistoryRepository.findAllByExpectedReturnDateLessThanEqual(TODAY)).thenReturn(List.of(candidate));
        when(employeeStatusHistoryRepository.findTopByEmployeeIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(candidate));

        job().run();

        verify(loginApprovalRequestRepository, never()).merge(any());
    }
}
