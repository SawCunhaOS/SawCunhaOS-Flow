
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

package br.com.sawcunhaos.organization.application.usecase.access.resource.registry;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.resource.dto.RegisterResourceInput;
import br.com.sawcunhaos.organization.domain.access.resource.specification.ResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Testes de {@link RegistryResourcesUseCaseBean}: propagação dos metadados e do {@code systemCode}
 * (principal autenticado) para cada resource, e propagação de exceção (que dispararia o rollback
 * do lote atômico em runtime via {@code @Transactional}).
 */
@ExtendWith(MockitoExtension.class)
class RegistryResourcesUseCaseBeanTest {

    private static final String SYSTEM_CODE = "ORG";
    private static final Instant DEFINITION_UPDATED_AT =
            LocalDate.of(2026, 7, 9).atStartOfDay(ZoneOffset.UTC).toInstant();

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private RegistryResourcesUseCaseBean useCase;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
    }

    private RegistryResourceInput input(String code, boolean active) {
        return RegistryResourceInput.builder()
                .code(code)
                .descriptionPt("pt-" + code)
                .descriptionEn("en-" + code)
                .group("Corporate")
                .subGroup("Department")
                .version("1.0.0")
                .updatedAt(DEFINITION_UPDATED_AT)
                .active(active)
                .build();
    }

    @Test
    void execute_registersEachResourceWithSystemCodeAndMetadata() {
        authenticateAs(SYSTEM_CODE);

        useCase.execute(List.of(input("GET_DEPARTMENT", true), input("CREATE_DEPARTMENT", false)));

        ArgumentCaptor<RegisterResourceInput> captor = ArgumentCaptor.forClass(RegisterResourceInput.class);
        verify(resourceService, times(2)).register(captor.capture());

        List<RegisterResourceInput> registered = captor.getAllValues();
        assertThat(registered).extracting(RegisterResourceInput::code)
                .containsExactly("GET_DEPARTMENT", "CREATE_DEPARTMENT");
        assertThat(registered).allSatisfy(r -> {
            assertThat(r.systemCode()).isEqualTo(SYSTEM_CODE);
            assertThat(r.group()).isEqualTo("Corporate");
            assertThat(r.subGroup()).isEqualTo("Department");
            assertThat(r.version()).isEqualTo("1.0.0");
            assertThat(r.updatedAt()).isEqualTo(DEFINITION_UPDATED_AT);
        });
        assertThat(registered).extracting(RegisterResourceInput::active)
                .containsExactly(true, false);
    }

    @Test
    void execute_whenOneResourceFails_propagatesScosExceptionForRollback() {
        authenticateAs(SYSTEM_CODE);
        doThrow(new ScosException()).when(resourceService).register(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> useCase.execute(List.of(input("GET_DEPARTMENT", true))))
                .isInstanceOf(ScosException.class);
    }
}
