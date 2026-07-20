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

package br.com.sawcunhaos.organization.application.usecase.access.authority.validate;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.AuthorityResponseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link ValidateAuthorityUseCaseBean}: delegação do login e projeção dos campos de autoridade. */
@ExtendWith(MockitoExtension.class)
class ValidateAuthorityUseCaseBeanTest {

    @Mock
    private AuthorityResponseService authorityResponseService;

    @InjectMocks
    private ValidateAuthorityUseCaseBean useCase;

    @Test
    @DisplayName("delega o login e projeta os campos de autoridade no output do contrato")
    void executes_delegatesLoginAndProjectsAuthorityFields() {
        AuthorityResponseOutput output = new AuthorityResponseOutput(
                1L, "john.doe", "INTERNAL", "ACTIVE", UUID.randomUUID(), 2L, "ADMIN",
                "John Doe", "john@acme.com", 10L, "ACME", 20L, "HQ", 30L,
                List.of("READ", "WRITE"));
        given(authorityResponseService.validate("john.doe")).willReturn(output);

        ValidateAuthorityOutput result = useCase.execute("john.doe");

        then(authorityResponseService).should().validate("john.doe");
        assertThat(result.login()).isEqualTo("john.doe");
        assertThat(result.name()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@acme.com");
        assertThat(result.companyId()).isEqualTo(10L);
        assertThat(result.companyName()).isEqualTo("ACME");
        assertThat(result.branchId()).isEqualTo(20L);
        assertThat(result.branchName()).isEqualTo("HQ");
        assertThat(result.employeeId()).isEqualTo(30L);
        assertThat(result.permissions()).containsExactly("READ", "WRITE");
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(authorityResponseService.validate(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute("john.doe")).isInstanceOf(ScosException.class);
    }
}
