
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
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import br.com.sawcunhaos.organization.domain.access.profile.internal.Profile;
import br.com.sawcunhaos.organization.domain.access.profile.internal.ProfileRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.Employee;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_EMPLOYEE_014;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_015;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_016;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_PROFILE_001;

@Service
@RequiredArgsConstructor
@Slf4j
class LoginServiceBean implements LoginService {

    private final LoginRepository loginRepository;
    private final ProfileRepository profileRepository;
    private final EmployeeQueryRepository employeeQueryRepository;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    @Transactional(rollbackFor = ScosException.class)
    public LoginOutput create(@NonNull LoginInput loginInput) {
        log.info("Create Login: {}, Type: {}", loginInput.login(), loginInput.type());

        if (loginRepository.existsByLogin(loginInput.login())) {
            throw new ScosException(SCOS_LOGIN_015);
        }

        Profile profile = profileRepository.findById(loginInput.profileId())
                .orElseThrow(() -> new ScosException(SCOS_PROFILE_001));

        Employee employee = loginInput.employeeId() != null
                ? employeeQueryRepository.findById(loginInput.employeeId()).orElseThrow(() -> new ScosException(SCOS_EMPLOYEE_014))
                : null;

        Login login = Login.builder()
                .login(loginInput.login())
                .type(loginInput.type())
                .status(LoginStatus.PENDING_APPROVAL)
                .profile(profile)
                .employee(employee)
                .build();
        login.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        login = loginRepository.merge(login);

        return toLoginOutput(login, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginOutput findById(@NonNull Long id) {
        log.info("Find Login by Id: {}", id);
        Login login = findLoginById(id);
        return toLoginOutput(login, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginOutput> findAll(LoginType type, LoginStatus status, Long employeeId, @NonNull Pageable pageable) {
        log.info("Find All Logins, Type: {}, Status: {}, EmployeeId: {}", type, status, employeeId);
        return loginRepository.findAllFiltered(type, status, employeeId, pageable)
                .map(login -> toLoginOutput(login, false));
    }

    private Login findLoginById(@NonNull Long id) {
        return loginRepository.findById(id).orElseThrow(
                () -> new ScosException(SCOS_LOGIN_016)
        );
    }

    /**
     * {@code denormalize=true} (só {@code findById}, 1 linha) lê {@code profile.getCode()}/{@code .getDescription()}
     * e {@code employee.getName()} — inicializa 2 proxies LAZY, custo irrelevante numa linha só.
     * {@code denormalize=false} (usado por {@code findAll}, paginado) só lê {@code .getId()} de ambas as
     * relações — grátis, não inicializa proxy — evita N+1 numa página inteira pra 3 campos que
     * {@code toApiLoginSummary} nem usa.
     */
    private LoginOutput toLoginOutput(Login login, boolean denormalize) {
        LoginOutput.LoginOutputBuilder builder = LoginOutput.builder()
                .id(login.getId())
                .login(login.getLogin())
                .externalId(login.getExternalId())
                .type(login.getType())
                .status(login.getStatus())
                .profileId(login.getProfile().getId())
                .employeeId(login.getEmployee() != null ? login.getEmployee().getId() : null);

        if (denormalize) {
            builder.profileCode(login.getProfile().getCode())
                    .profileDescription(login.getProfile().getDescription())
                    .employeeName(login.getEmployee() != null ? login.getEmployee().getName() : null);
        }

        return builder.build();
    }
}
