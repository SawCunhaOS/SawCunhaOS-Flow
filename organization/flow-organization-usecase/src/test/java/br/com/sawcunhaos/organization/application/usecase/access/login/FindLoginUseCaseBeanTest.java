
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.Login;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import br.com.sawcunhaos.organization.domain.access.login.internal.LoginType;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/** Testes de {@link FindLoginUseCaseBean}: mapeamento LoginOutput denormalizado → Login (api dto). */
@ExtendWith(MockitoExtension.class)
class FindLoginUseCaseBeanTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private FindLoginUseCaseBean useCase;

    @Test
    @DisplayName("mapeia LoginOutput (com profile/employee denormalizados) para Login completo")
    void executes_mapsDenormalizedOutputToApiLogin() {
        LoginOutput output = LoginOutput.builder()
                .id(10L).login("john.doe").type(LoginType.EMPLOYEE).status(LoginStatus.PENDING_APPROVAL)
                .profileId(1L).profileCode("ADMIN").profileDescription("Administrator")
                .employeeId(2L).employeeName("John Doe")
                .build();
        given(loginService.findById(10L)).willReturn(output);

        Login result = useCase.execute(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.login()).isEqualTo("john.doe");
        assertThat(result.profile().code()).isEqualTo("ADMIN");
        assertThat(result.employee().name()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("propaga ScosException quando o service não encontra o Login")
    void whenServiceThrows_propagatesScosException() {
        given(loginService.findById(999L)).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(999L)).isInstanceOf(ScosException.class);
    }
}
