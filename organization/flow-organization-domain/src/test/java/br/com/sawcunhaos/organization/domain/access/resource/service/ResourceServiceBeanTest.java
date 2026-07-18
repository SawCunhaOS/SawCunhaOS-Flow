
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

package br.com.sawcunhaos.organization.domain.access.resource.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.resource.dto.RegisterResourceInput;
import br.com.sawcunhaos.organization.domain.access.resource.internal.ResourceRepository;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ResourceServiceBean}: propagação dos metadados ao upsert, desativação
 * (active=false) e resolução de sistema inexistente para {@code SCOS_SYSTEM_001}.
 *
 * <p>Cobertura Mockito: verifica os argumentos passados ao upsert nativo. O gatilho condicional
 * do {@code DO UPDATE} (só quando {@code active}/{@code definitionUpdatedAt} mudam) vive no SQL
 * PostgreSQL e não é exercitável por unit test — requer integração (fora deste módulo).
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceBeanTest {

    private static final String SYSTEM_CODE = "ORG";
    private static final UUID SYSTEM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate DEFINITION_UPDATED_AT = LocalDate.of(2026, 7, 9);

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ScosSystemRepository scosSystemRepository;

    @InjectMocks
    private ResourceServiceBean service;

    private RegisterResourceInput input(boolean active) {
        return RegisterResourceInput.builder()
                .code("GET_DEPARTMENT")
                .descriptionPt("Consultar departamento")
                .descriptionEn("Get department")
                .group("Corporate")
                .subGroup("Department")
                .version("1.0.0")
                .updatedAt(DEFINITION_UPDATED_AT)
                .active(active)
                .systemCode(SYSTEM_CODE)
                .build();
    }

    @Test
    void register_whenSystemExists_callsUpsertWithAllMetadata() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(ScosSystem.builder().id(SYSTEM_ID).build()));

        service.register(input(true));

        verify(resourceRepository).upsert(
                eq(SYSTEM_ID),
                eq("GET_DEPARTMENT"),
                eq("Consultar departamento"),
                eq("Get department"),
                eq("Corporate"),
                eq("Department"),
                eq("1.0.0"),
                eq(DEFINITION_UPDATED_AT),
                eq(true),
                eq(SYSTEM_CODE)
        );
    }

    @Test
    void register_whenActiveFalse_passesActiveFalseToUpsert() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(ScosSystem.builder().id(SYSTEM_ID).build()));

        service.register(input(false));

        verify(resourceRepository).upsert(
                any(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), eq(false), anyString());
    }

    @Test
    void register_whenSystemMissing_throwsScosSystem001AndSkipsUpsert() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(input(true)))
                .isInstanceOf(ScosException.class)
                .extracting(ex -> ((ScosException) ex).getCode())
                .isEqualTo(SCOS_SYSTEM_001.getCode());

        verify(resourceRepository, never()).upsert(
                any(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), anyBoolean(), anyString());
    }

    @Test
    void register_whenSystemMissing_exceptionCarriesSystemCodeArg() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(input(true)))
                .isInstanceOfSatisfying(ScosException.class,
                        ex -> assertThat(ex.getArgs()).containsExactly(SYSTEM_CODE));
    }
}
