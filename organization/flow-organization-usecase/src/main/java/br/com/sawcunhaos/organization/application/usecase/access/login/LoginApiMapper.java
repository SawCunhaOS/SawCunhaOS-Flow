
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.organization.api.dto.Login;
import br.com.sawcunhaos.organization.api.dto.LoginEmployee;
import br.com.sawcunhaos.organization.api.dto.LoginStatus;
import br.com.sawcunhaos.organization.api.dto.LoginSummary;
import br.com.sawcunhaos.organization.api.dto.LoginType;
import br.com.sawcunhaos.organization.api.dto.ProfileSummary;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;

final class LoginApiMapper {

    private LoginApiMapper() {
    }

    static Login toApiLogin(LoginOutput login) {
        return Login.builder()
                .id(login.id())
                .login(login.login())
                .keycloakId(login.externalId())
                .type(toApiType(login.type()))
                .status(toApiStatus(login.status()))
                .profile(ProfileSummary.builder()
                        .id(login.profileId())
                        .code(login.profileCode())
                        .description(login.profileDescription())
                        .build())
                .employee(login.employeeId() != null
                        ? LoginEmployee.builder().id(login.employeeId()).name(login.employeeName()).build()
                        : null)
                .build();
    }

    static LoginSummary toApiLoginSummary(LoginOutput login) {
        return LoginSummary.builder()
                .id(login.id())
                .login(login.login())
                .type(toApiType(login.type()))
                .status(toApiStatus(login.status()))
                .build();
    }

    static LoginType toApiType(br.com.sawcunhaos.organization.domain.access.login.internal.LoginType type) {
        return type == null ? null : LoginType.valueOf(type.name());
    }

    static LoginStatus toApiStatus(br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus status) {
        return status == null ? null : LoginStatus.valueOf(status.name());
    }

    static br.com.sawcunhaos.organization.domain.access.login.internal.LoginType toDomainType(LoginType type) {
        return type == null ? null : br.com.sawcunhaos.organization.domain.access.login.internal.LoginType.valueOf(type.name());
    }

    static br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus toDomainStatus(LoginStatus status) {
        return status == null ? null : br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus.valueOf(status.name());
    }
}
