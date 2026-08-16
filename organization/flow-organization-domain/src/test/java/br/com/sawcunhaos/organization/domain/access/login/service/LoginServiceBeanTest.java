
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_015;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_016;
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

    @Mock
    private LoginRepository loginRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private EmployeeQueryRepository employeeQueryRepository;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private LoginServiceBean loginServiceBean;

    private Profile profile;
    private Employee employee;

    @BeforeEach
    void setUp() {
        profile = Profile.builder().id(1L).code("ADMIN").description("Administrator").active(true).build();
        employee = Employee.builder().id(2L).name("John Doe").build();
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
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
}
