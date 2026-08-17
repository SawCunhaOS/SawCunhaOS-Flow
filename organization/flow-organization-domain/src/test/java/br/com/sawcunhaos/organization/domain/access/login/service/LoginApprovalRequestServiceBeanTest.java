
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
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestProfileChangeKind;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.profile.internal.LoginProfile;
import br.com.sawcunhaos.organization.domain.access.profile.internal.LoginProfileRepository;
import br.com.sawcunhaos.organization.domain.access.profile.internal.Profile;
import br.com.sawcunhaos.organization.domain.access.status.internal.LoginStatusHistory;
import br.com.sawcunhaos.organization.domain.access.status.internal.LoginStatusHistoryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.dto.EmployeeOutput;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeeService;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxEventRepository;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxTopic;
import br.com.sawcunhaos.organization.domain.outbox.internal.OutboxTopicRepository;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_016;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_017;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_018;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_APPROVAL_REQUEST_001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre os 4 cenários do AC 3 (aprovador certo decide; não-aprovador sem APPROVE_SYSTEM_ACCESS →
 * 422; auto-aprovação sem a permissão → 422; auto-aprovação com a permissão → sucesso +
 * isExceptionSelfApproval=true), AC 5 (Funcionário desligado entre criação e decisão → 422) e
 * AC 10 (currentApprover presente para SUPERVISOR/MANAGER, null para SYSTEM_ACCESS_GROUP).
 */
@ExtendWith(MockitoExtension.class)
class LoginApprovalRequestServiceBeanTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");
    private static final String DECIDER_USERNAME = "decider";

    @Mock
    private LoginApprovalRequestRepository loginApprovalRequestRepository;
    @Mock
    private LoginRepository loginRepository;
    @Mock
    private LoginProfileRepository loginProfileRepository;
    @Mock
    private LoginStatusHistoryRepository loginStatusHistoryRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private OutboxTopicRepository outboxTopicRepository;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private LoginApprovalRequestServiceBean service() {
        return new LoginApprovalRequestServiceBean(loginApprovalRequestRepository, loginRepository,
                loginProfileRepository, loginStatusHistoryRepository, outboxEventRepository, outboxTopicRepository,
                employeeService, scosUserAuthentication, clock);
    }

    /** {@code update(S)}/{@code merge(S)} existem tanto em {@link BaseJpaRepository} quanto em outra sobrecarga - estreita o tipo. */
    private BaseJpaRepository<LoginApprovalRequest, Long> asBaseJpaRepository() {
        return loginApprovalRequestRepository;
    }

    private Employee employee(Long id, String name, Employee supervisor) {
        return Employee.builder().id(id).name(name).supervisor(supervisor).build();
    }

    private Login pendingLogin(Long id, Employee employee) {
        return Login.builder().id(id).login("jane.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).employee(employee).build();
    }

    private LoginApprovalRequest pendingRequest(Login login) {
        return pendingRequest(login, LoginApprovalRequestType.CREATE_LOGIN);
    }

    private LoginApprovalRequest pendingRequest(Login login, LoginApprovalRequestType requestType) {
        return LoginApprovalRequest.builder()
                .id(1L).login(login).currentLevel(LoginApprovalRequestLevel.SUPERVISOR)
                .status(LoginApprovalRequestStatus.PENDING)
                .requestType(requestType)
                .build();
    }

    private void stubEmployeeActive(Long employeeId) {
        lenient().when(employeeService.findById(employeeId)).thenReturn(
                EmployeeOutput.builder().id(employeeId).status(StatusEmployee.ACTIVE).build());
    }

    private void stubOutboxTopic() {
        lenient().when(outboxTopicRepository.findById("KEYCLOAK_LOGIN_SYNC")).thenReturn(
                Optional.of(OutboxTopic.builder().topic("KEYCLOAK_LOGIN_SYNC").defaultMaxRetries(3).build()));
    }

    // ---- decide ----

    @Test
    void decideShouldApproveWhenDeciderIsTheResolvedApprover() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        Login deciderLogin = pendingLogin(50L, supervisor);
        deciderLogin.setStatus(LoginStatus.ACTIVE);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);
        stubOutboxTopic();

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, "aprovado", false);

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.APPROVED);
        assertThat(request.isExceptionSelfApproval()).isFalse();
        verify(loginStatusHistoryRepository).merge(any());
        verify(outboxEventRepository).merge(any());
        verify(asBaseJpaRepository()).update(request);
    }

    @Test
    void decideShouldThrowWhenDeciderIsNotResolvedApproverAndHasNoSystemAccessPermission() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        Employee otherEmployee = employee(999L, "Random Person", null);
        Login deciderLogin = pendingLogin(51L, otherEmployee);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));

        assertThatThrownBy(() -> service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_017.getCode());

        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void decideShouldThrowWhenSelfApprovalWithoutSystemAccessPermission() {
        Employee employee = employee(2L, "Jane Doe", null);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        // O próprio login sendo decidido é quem está autenticado (auto-aprovação)
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_017.getCode());

        verify(asBaseJpaRepository(), never()).update(any());
    }

    @Test
    void decideShouldApproveSelfWithExceptionFlagWhenDeciderHasSystemAccessPermission() {
        Employee employee = employee(2L, "Jane Doe", null);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(login));
        stubEmployeeActive(2L);
        stubOutboxTopic();

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, true);

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.APPROVED);
        assertThat(request.isExceptionSelfApproval()).isTrue();
        verify(asBaseJpaRepository()).update(request);
    }

    @Test
    void decideShouldThrowWhenEmployeeIsNoLongerActive() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        Login deciderLogin = pendingLogin(50L, supervisor);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        when(employeeService.findById(2L)).thenReturn(EmployeeOutput.builder().id(2L).status(StatusEmployee.INACTIVE).build());

        assertThatThrownBy(() -> service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_018.getCode());

        verify(asBaseJpaRepository(), never()).update(any());
        verify(outboxEventRepository, never()).merge(any());
    }

    @Test
    void decideShouldRejectWithoutOpeningOutboxEvent() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        LoginApprovalRequest request = pendingRequest(login);

        Login deciderLogin = pendingLogin(50L, supervisor);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);

        service().decide(1L, LoginApprovalRequestStatus.REJECTED, 3L, "sem aderência", false);

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.REJECTED);
        verify(loginStatusHistoryRepository).merge(any());
        verify(outboxEventRepository, never()).merge(any());
        verify(asBaseJpaRepository()).update(request);
    }

    // ---- decide REACTIVATE_LOGIN (Story 3.3) ----

    @Test
    void decideShouldActivateWhenReactivationApprovedFromInactive() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.INACTIVE);
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.REACTIVATE_LOGIN);

        Login deciderLogin = pendingLogin(50L, supervisor);
        deciderLogin.setStatus(LoginStatus.ACTIVE);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);
        stubOutboxTopic();

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false);

        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.APPROVED);
        ArgumentCaptor<LoginStatusHistory> historyCaptor = ArgumentCaptor.forClass(LoginStatusHistory.class);
        verify(loginStatusHistoryRepository).merge(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(historyCaptor.getValue().getReasonActivate()).isNotNull();
        assertThat(historyCaptor.getValue().getReasonEnable()).isNull();
        verify(outboxEventRepository).merge(any());
    }

    @Test
    void decideShouldEnableWhenReactivationApprovedFromBlocked() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.BLOCKED);
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.REACTIVATE_LOGIN);

        Login deciderLogin = pendingLogin(50L, supervisor);
        deciderLogin.setStatus(LoginStatus.ACTIVE);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);
        stubOutboxTopic();

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false);

        ArgumentCaptor<LoginStatusHistory> historyCaptor = ArgumentCaptor.forClass(LoginStatusHistory.class);
        verify(loginStatusHistoryRepository).merge(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(LoginStatus.ACTIVE);
        assertThat(historyCaptor.getValue().getReasonEnable()).isNotNull();
        assertThat(historyCaptor.getValue().getReasonActivate()).isNull();
        verify(outboxEventRepository).merge(any());
    }

    @Test
    void decideShouldKeepLoginUnchangedWhenReactivationIsRejected() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.INACTIVE);
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.REACTIVATE_LOGIN);

        Login deciderLogin = pendingLogin(50L, supervisor);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);

        service().decide(1L, LoginApprovalRequestStatus.REJECTED, 3L, "sem justificativa", false);

        assertThat(login.getStatus()).isEqualTo(LoginStatus.INACTIVE);
        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.REJECTED);
        verify(loginStatusHistoryRepository, never()).merge(any());
        verify(outboxEventRepository, never()).merge(any());
        verify(asBaseJpaRepository()).update(request);
    }

    // ---- decide CHANGE_PROFILE (Story 3.4) ----

    @Test
    void decideShouldSetPrimaryProfileWhenChangeProfileApproved() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.ACTIVE);
        Profile currentProfile = Profile.builder().id(1L).code("ADMIN").build();
        Profile requestedProfile = Profile.builder().id(2L).code("VIEWER").build();
        login.setProfile(currentProfile);
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.CHANGE_PROFILE);
        request.setRequestedProfile(requestedProfile);
        request.setProfileChangeKind(LoginApprovalRequestProfileChangeKind.SET_PRIMARY);

        Login deciderLogin = pendingLogin(50L, supervisor);
        deciderLogin.setStatus(LoginStatus.ACTIVE);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false);

        assertThat(login.getProfile()).isEqualTo(requestedProfile);
        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.APPROVED);
        verify(loginRepository).merge(login);
        verify(loginProfileRepository, never()).merge(any());
        verify(loginStatusHistoryRepository, never()).merge(any()); // troca de perfil não é transição de status
        verify(outboxEventRepository, never()).merge(any()); // sem Saga Keycloak para CHANGE_PROFILE
    }

    @Test
    void decideShouldAddAdditionalProfileWhenChangeProfileApproved() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.ACTIVE);
        Profile requestedProfile = Profile.builder().id(2L).code("VIEWER").build();
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.CHANGE_PROFILE);
        request.setRequestedProfile(requestedProfile);
        request.setProfileChangeKind(LoginApprovalRequestProfileChangeKind.ADD_ADDITIONAL);

        Login deciderLogin = pendingLogin(50L, supervisor);
        deciderLogin.setStatus(LoginStatus.ACTIVE);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);

        service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, false);

        ArgumentCaptor<LoginProfile> captor = ArgumentCaptor.forClass(LoginProfile.class);
        verify(loginProfileRepository).merge(captor.capture());
        assertThat(captor.getValue().getId().getLoginId()).isEqualTo(1L);
        assertThat(captor.getValue().getId().getProfileId()).isEqualTo(2L);
        verify(loginRepository, never()).merge(any(Login.class));
        verify(outboxEventRepository, never()).merge(any());
    }

    @Test
    void decideShouldKeepProfileUnchangedWhenChangeProfileIsRejected() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Employee employee = employee(2L, "Jane Doe", supervisor);
        Login login = pendingLogin(1L, employee);
        login.setStatus(LoginStatus.ACTIVE);
        Profile currentProfile = Profile.builder().id(1L).code("ADMIN").build();
        login.setProfile(currentProfile);
        LoginApprovalRequest request = pendingRequest(login, LoginApprovalRequestType.CHANGE_PROFILE);
        request.setRequestedProfile(Profile.builder().id(2L).code("VIEWER").build());
        request.setProfileChangeKind(LoginApprovalRequestProfileChangeKind.SET_PRIMARY);

        Login deciderLogin = pendingLogin(50L, supervisor);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.of(deciderLogin));
        stubEmployeeActive(2L);

        service().decide(1L, LoginApprovalRequestStatus.REJECTED, 3L, "sem aderência", false);

        assertThat(login.getProfile()).isEqualTo(currentProfile);
        assertThat(request.getStatus()).isEqualTo(LoginApprovalRequestStatus.REJECTED);
        verify(loginRepository, never()).merge(any(Login.class));
        verify(loginProfileRepository, never()).merge(any());
        verify(loginStatusHistoryRepository, never()).merge(any());
        verify(outboxEventRepository, never()).merge(any());
    }

    @Test
    void decideShouldThrowWhenRequestNotFound() {
        when(loginApprovalRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().decide(999L, LoginApprovalRequestStatus.APPROVED, 4L, null, false))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_APPROVAL_REQUEST_001.getCode());
    }

    @Test
    void decideShouldThrowWhenDecidingLoginIsNotFound() {
        Login login = pendingLogin(1L, employee(2L, "Jane Doe", null));
        LoginApprovalRequest request = pendingRequest(login);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(scosUserAuthentication.findUserAuthentication()).thenReturn(DECIDER_USERNAME);
        when(loginRepository.findByLogin(DECIDER_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().decide(1L, LoginApprovalRequestStatus.APPROVED, 4L, null, true))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_016.getCode());
    }

    // ---- findById / findAll (AC 10) ----

    @Test
    void findByIdShouldIncludeCurrentApproverWhenLevelIsSupervisor() {
        Employee supervisor = employee(5L, "Sam Supervisor", null);
        Login login = pendingLogin(1L, employee(2L, "Jane Doe", supervisor));
        LoginApprovalRequest request = pendingRequest(login);

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        LoginApprovalRequestOutput output = service().findById(1L);

        assertThat(output.id()).isEqualTo(1L);
        assertThat(output.currentApproverEmployeeId()).isEqualTo(5L);
        assertThat(output.currentApproverEmployeeName()).isEqualTo("Sam Supervisor");
    }

    @Test
    void findByIdShouldReturnNullCurrentApproverWhenLevelIsSystemAccessGroup() {
        Login login = pendingLogin(1L, employee(2L, "Jane Doe", null));
        LoginApprovalRequest request = LoginApprovalRequest.builder()
                .id(1L).login(login).currentLevel(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP)
                .status(LoginApprovalRequestStatus.PENDING)
                .build();

        when(loginApprovalRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        LoginApprovalRequestOutput output = service().findById(1L);

        assertThat(output.currentApproverEmployeeId()).isNull();
        assertThat(output.currentApproverEmployeeName()).isNull();
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(loginApprovalRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_APPROVAL_REQUEST_001.getCode());
    }

    @Test
    void findAllShouldDelegateFiltersToRepositoryAndMapEachItem() {
        Pageable pageable = Pageable.unpaged();
        Login login = pendingLogin(1L, employee(2L, "Jane Doe", null));
        LoginApprovalRequest request = pendingRequest(login);

        when(loginApprovalRequestRepository.findAllFiltered(LoginApprovalRequestStatus.PENDING, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(request)));

        Page<LoginApprovalRequestOutput> result = service().findAll(LoginApprovalRequestStatus.PENDING, 1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(1L);
        verify(loginApprovalRequestRepository).findAllFiltered(LoginApprovalRequestStatus.PENDING, 1L, pageable);
    }
}
