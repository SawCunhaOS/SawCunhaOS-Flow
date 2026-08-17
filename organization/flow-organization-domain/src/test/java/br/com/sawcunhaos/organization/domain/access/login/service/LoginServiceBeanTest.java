
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
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginInput;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.Login;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequest;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestLevel;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginApprovalRequestType;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginRepository;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.profile.internal.Profile;
import br.com.sawcunhaos.organization.domain.access.profile.internal.ProfileRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import org.junit.jupiter.api.BeforeEach;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_013;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_015;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_016;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_019;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_PROFILE_001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceBeanTest {

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    @Mock
    private LoginRepository loginRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private EmployeeQueryRepository employeeQueryRepository;
    @Mock
    private LoginApprovalRequestRepository loginApprovalRequestRepository;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    private final BusinessDayCalculator businessDayCalculator = new BusinessDayCalculator();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private LoginServiceBean loginServiceBean;

    private Profile profile;
    private Employee employee;
    private Login requester;

    @BeforeEach
    void setUp() {
        loginServiceBean = new LoginServiceBean(loginRepository, profileRepository, employeeQueryRepository,
                loginApprovalRequestRepository, businessDayCalculator, clock, scosUserAuthentication);

        profile = Profile.builder().id(1L).code("ADMIN").description("Administrator").active(true).build();
        employee = Employee.builder().id(2L).name("John Doe").build();
        requester = Login.builder().id(99L).login("test-user").type(LoginType.EMPLOYEE).status(LoginStatus.ACTIVE).build();
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
        lenient().when(loginRepository.findByLogin("test-user")).thenReturn(Optional.of(requester));
    }

    // ---- create ----

    @Test
    void createShouldPersistInPendingApprovalWhenLoginIsUniqueAndProfileAndEmployeeExist() {
        LoginInput input = LoginInput.builder().login("john.doe").profileId(1L).type(LoginType.EMPLOYEE).employeeId(2L).build();
        Login persisted = Login.builder().id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profile(profile).employee(employee).build();

        when(loginRepository.existsByLogin("john.doe")).thenReturn(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(employeeQueryRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(loginRepository.merge(any(Login.class))).thenReturn(persisted);

        LoginOutput result = loginServiceBean.create(input);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.login()).isEqualTo("john.doe");
        assertThat(result.status()).isEqualTo(LoginStatus.PENDING_APPROVAL);
        assertThat(result.profileId()).isEqualTo(1L);
        assertThat(result.employeeId()).isEqualTo(2L);
        assertThat(result.profileCode()).isEqualTo("ADMIN");
        assertThat(result.employeeName()).isEqualTo("John Doe");

        ArgumentCaptor<LoginApprovalRequest> captor = ArgumentCaptor.forClass(LoginApprovalRequest.class);
        verify(loginApprovalRequestRepository).merge(captor.capture());
        LoginApprovalRequest request = captor.getValue();
        assertThat(request.getLogin()).isEqualTo(persisted);
        assertThat(request.getRequestedByLogin()).isEqualTo(requester);
        // employee sem supervisor e sem position -> pula SUPERVISOR e MANAGER, cai em SYSTEM_ACCESS_GROUP
        assertThat(request.getCurrentLevel()).isEqualTo(LoginApprovalRequestLevel.SYSTEM_ACCESS_GROUP);
        assertThat(request.getSlaDeadline()).isEqualTo(businessDayCalculator.plusBusinessDays(NOW, 1, ZoneOffset.UTC));
    }

    @Test
    void createShouldThrowWhenRequestingLoginIsNotFound() {
        LoginInput input = LoginInput.builder().login("john.doe").profileId(1L).type(LoginType.EMPLOYEE).employeeId(2L).build();
        Login persisted = Login.builder().id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profile(profile).employee(employee).build();

        when(loginRepository.existsByLogin("john.doe")).thenReturn(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(employeeQueryRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(loginRepository.merge(any(Login.class))).thenReturn(persisted);
        when(loginRepository.findByLogin("test-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_016.getCode());

        verify(loginApprovalRequestRepository, never()).merge(any());
    }

    @Test
    void createShouldSkipEmployeeLookupWhenEmployeeIdIsNull() {
        LoginInput input = LoginInput.builder().login("svc.integration").profileId(1L).type(LoginType.SERVICE).employeeId(null).build();
        Login persisted = Login.builder().id(11L).login("svc.integration").type(LoginType.SERVICE).status(LoginStatus.PENDING_APPROVAL).profile(profile).employee(null).build();

        when(loginRepository.existsByLogin("svc.integration")).thenReturn(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(loginRepository.merge(any(Login.class))).thenReturn(persisted);

        LoginOutput result = loginServiceBean.create(input);

        assertThat(result.employeeId()).isNull();
        assertThat(result.employeeName()).isNull();
        verify(employeeQueryRepository, never()).findById(any());
    }

    @Test
    void createShouldThrowWhenLoginAlreadyExists() {
        LoginInput input = LoginInput.builder().login("john.doe").profileId(1L).type(LoginType.EMPLOYEE).employeeId(2L).build();

        when(loginRepository.existsByLogin("john.doe")).thenReturn(true);

        assertThatThrownBy(() -> loginServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_015.getCode());

        verify(profileRepository, never()).findById(any());
        verify(loginRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenProfileDoesNotExist() {
        LoginInput input = LoginInput.builder().login("john.doe").profileId(99L).type(LoginType.EMPLOYEE).employeeId(2L).build();

        when(loginRepository.existsByLogin("john.doe")).thenReturn(false);
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_PROFILE_001.getCode());

        verify(loginRepository, never()).merge(any());
    }

    @Test
    void createShouldThrowWhenEmployeeDoesNotExist() {
        LoginInput input = LoginInput.builder().login("john.doe").profileId(1L).type(LoginType.EMPLOYEE).employeeId(99L).build();

        when(loginRepository.existsByLogin("john.doe")).thenReturn(false);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(employeeQueryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_EMPLOYEE_014.getCode());

        verify(loginRepository, never()).merge(any());
    }

    // ---- findById ----

    @Test
    void findByIdShouldReturnDenormalizedOutput() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profile(profile).employee(employee).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));

        LoginOutput result = loginServiceBean.findById(10L);

        assertThat(result.profileCode()).isEqualTo("ADMIN");
        assertThat(result.profileDescription()).isEqualTo("Administrator");
        assertThat(result.employeeName()).isEqualTo("John Doe");
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(loginRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_016.getCode());
    }

    // ---- findAll ----

    @Test
    void findAllShouldNotDenormalizeProfileOrEmployee() {
        Pageable pageable = Pageable.unpaged();
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL).profile(profile).employee(employee).build();

        when(loginRepository.findAllFiltered(null, null, null, pageable)).thenReturn(new PageImpl<>(List.of(login)));

        Page<LoginOutput> result = loginServiceBean.findAll(null, null, null, pageable);

        LoginOutput output = result.getContent().get(0);
        assertThat(output.id()).isEqualTo(10L);
        assertThat(output.profileId()).isEqualTo(1L);
        assertThat(output.employeeId()).isEqualTo(2L);
        assertThat(output.profileCode()).isNull();
        assertThat(output.profileDescription()).isNull();
        assertThat(output.employeeName()).isNull();
    }

    @Test
    void findAllShouldDelegateFiltersToRepository() {
        Pageable pageable = Pageable.unpaged();
        when(loginRepository.findAllFiltered(LoginType.EMPLOYEE, LoginStatus.ACTIVE, 2L, pageable)).thenReturn(new PageImpl<>(List.of()));

        loginServiceBean.findAll(LoginType.EMPLOYEE, LoginStatus.ACTIVE, 2L, pageable);

        verify(loginRepository).findAllFiltered(LoginType.EMPLOYEE, LoginStatus.ACTIVE, 2L, pageable);
    }

    // ---- requestReactivation (Story 3.3) ----

    @Test
    void requestReactivationShouldOpenApprovalRequestWhenLoginIsInactive() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.SERVICE).status(LoginStatus.INACTIVE).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));
        when(loginApprovalRequestRepository.findByLoginIdAndStatus(10L, LoginApprovalRequestStatus.PENDING)).thenReturn(Optional.empty());

        loginServiceBean.requestReactivation(10L);

        assertThat(login.getStatus()).isEqualTo(LoginStatus.INACTIVE);

        ArgumentCaptor<LoginApprovalRequest> captor = ArgumentCaptor.forClass(LoginApprovalRequest.class);
        verify(loginApprovalRequestRepository).merge(captor.capture());
        assertThat(captor.getValue().getLogin()).isEqualTo(login);
        assertThat(captor.getValue().getRequestType()).isEqualTo(LoginApprovalRequestType.REACTIVATE_LOGIN);
        assertThat(captor.getValue().getRequestedByLogin()).isEqualTo(requester);
    }

    @Test
    void requestReactivationShouldOpenApprovalRequestWhenLoginIsBlocked() {
        Login login = Login.builder().id(11L).login("jane.doe").type(LoginType.SERVICE).status(LoginStatus.BLOCKED).build();
        when(loginRepository.findById(11L)).thenReturn(Optional.of(login));
        when(loginApprovalRequestRepository.findByLoginIdAndStatus(11L, LoginApprovalRequestStatus.PENDING)).thenReturn(Optional.empty());

        loginServiceBean.requestReactivation(11L);

        assertThat(login.getStatus()).isEqualTo(LoginStatus.BLOCKED);
        verify(loginApprovalRequestRepository).merge(any(LoginApprovalRequest.class));
    }

    @Test
    void requestReactivationShouldThrowWhenLoginNotFound() {
        when(loginRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginServiceBean.requestReactivation(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_016.getCode());

        verify(loginApprovalRequestRepository, never()).merge(any());
    }

    @Test
    void requestReactivationShouldThrowWhenLoginIsAlreadyActive() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.SERVICE).status(LoginStatus.ACTIVE).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginServiceBean.requestReactivation(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_013.getCode());

        verify(loginApprovalRequestRepository, never()).merge(any());
    }

    @Test
    void requestReactivationShouldThrowWhenLoginIsPendingApproval() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.SERVICE).status(LoginStatus.PENDING_APPROVAL).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginServiceBean.requestReactivation(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_013.getCode());
    }

    @Test
    void requestReactivationShouldThrowWhenLoginIsRejected() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.SERVICE).status(LoginStatus.REJECTED).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginServiceBean.requestReactivation(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_013.getCode());
    }

    @Test
    void requestReactivationShouldThrowWhenAPendingApprovalRequestAlreadyExists() {
        Login login = Login.builder().id(10L).login("john.doe").type(LoginType.SERVICE).status(LoginStatus.INACTIVE).build();
        when(loginRepository.findById(10L)).thenReturn(Optional.of(login));
        when(loginApprovalRequestRepository.findByLoginIdAndStatus(10L, LoginApprovalRequestStatus.PENDING))
                .thenReturn(Optional.of(LoginApprovalRequest.builder().id(1L).build()));

        assertThatThrownBy(() -> loginServiceBean.requestReactivation(10L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LOGIN_019.getCode());

        verify(loginApprovalRequestRepository, never()).merge(any());
    }
}
