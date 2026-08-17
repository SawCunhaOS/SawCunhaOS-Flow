
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

import br.com.sawcunhaos.organization.api.dto.GetSystemAccessApproversResponse;
import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.AuthorityResponseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FindSystemAccessApproversUseCaseBeanTest {

    private static final String PERMISSION_APPROVE_SYSTEM_ACCESS = "APPROVE_SYSTEM_ACCESS";

    @Mock
    private AuthorityResponseService authorityResponseService;

    @InjectMocks
    private FindSystemAccessApproversUseCaseBean useCase;

    private AuthorityResponseOutput authority(Long loginId, String login) {
        return AuthorityResponseOutput.builder()
                .loginId(loginId).login(login).type("EMPLOYEE").status("ACTIVE")
                .permissions(List.of(PERMISSION_APPROVE_SYSTEM_ACCESS))
                .build();
    }

    @Test
    void executeShouldMapEachHolderToLoginSummary() {
        given(authorityResponseService.findAllByPermission(PERMISSION_APPROVE_SYSTEM_ACCESS))
                .willReturn(List.of(authority(1L, "supervisor.a"), authority(2L, "manager.b")));

        GetSystemAccessApproversResponse response = useCase.execute();

        then(authorityResponseService).should().findAllByPermission(PERMISSION_APPROVE_SYSTEM_ACCESS);
        assertThat(response.data()).hasSize(2);
        assertThat(response.data()).extracting("login").containsExactly("supervisor.a", "manager.b");
    }

    @Test
    void executeShouldReturnEmptyListWhenNoHolders() {
        given(authorityResponseService.findAllByPermission(PERMISSION_APPROVE_SYSTEM_ACCESS)).willReturn(List.of());

        GetSystemAccessApproversResponse response = useCase.execute();

        assertThat(response.data()).isEmpty();
    }
}
