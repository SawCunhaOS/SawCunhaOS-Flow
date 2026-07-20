
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

package br.com.sawcunhaos.organization.domain.access.status.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonDisableRepository;
import io.hypersistence.utils.spring.repository.BaseJpaRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_DISABLE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ReasonDisableServiceBean}: unicidade de {@code code} por catálogo,
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class ReasonDisableServiceBeanTest {

    @Mock
    private ReasonDisableRepository reasonDisableRepository;
    @Mock
    private ReasonDisableMapper reasonDisableMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ReasonDisableServiceBean reasonDisableServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<ReasonDisable, Long> asBaseJpaRepository() {
        return reasonDisableRepository;
    }

    private ReasonDisable reasonDisable(Long id, String code, boolean active) {
        return ReasonDisable.builder().id(id).code(code).description("Inadimplência").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        ReasonDisableInput input = ReasonDisableInput.builder().code("INAD").description("Inadimplência").entityType(EntityType.COMPANY).build();
        ReasonDisable persisted = reasonDisable(1L, "INAD", true);

        when(reasonDisableRepository.existsByCodeAndEntityType("INAD", EntityType.COMPANY)).thenReturn(false);
        when(reasonDisableRepository.merge(any(ReasonDisable.class))).thenReturn(persisted);

        ReasonDisableOutput result = reasonDisableServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("INAD");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        ReasonDisableInput input = ReasonDisableInput.builder().code("INAD").description("Inadimplência").entityType(EntityType.COMPANY).build();

        when(reasonDisableRepository.existsByCodeAndEntityType("INAD", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> reasonDisableServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_002.getCode());

        verify(reasonDisableRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenReasonDisableDoesNotExist() {
        ReasonDisableInput input = ReasonDisableInput.builder().id(999L).code("INAD").description("Inadimplência").entityType(EntityType.COMPANY).build();

        when(reasonDisableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonDisableServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherReasonDisable() {
        ReasonDisable existing = reasonDisable(1L, "INAD", true);
        ReasonDisableInput input = ReasonDisableInput.builder().id(1L).code("OUTRO").description("Inadimplência").entityType(EntityType.COMPANY).build();

        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonDisableRepository.existsByCodeAndEntityTypeAndNotId("OUTRO", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reasonDisableServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        ReasonDisable existing = reasonDisable(1L, "INAD", true);
        ReasonDisableInput input = ReasonDisableInput.builder().id(1L).code("INAD").description("Inadimplência atualizada").entityType(EntityType.EMPLOYEE).build();

        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonDisableRepository.existsByCodeAndEntityTypeAndNotId("INAD", EntityType.EMPLOYEE, 1L)).thenReturn(false);

        reasonDisableServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Inadimplência atualizada");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(reasonDisableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonDisableServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        ReasonDisable existing = reasonDisable(1L, "INAD", true);
        ReasonDisableOutput output = ReasonDisableOutput.builder().id(1L).code("INAD").description("Inadimplência").entityType(EntityType.COMPANY).active(true).build();

        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonDisableMapper.toReasonDisableOutput(existing)).thenReturn(output);

        ReasonDisableOutput result = reasonDisableServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        ReasonDisable existing = reasonDisable(1L, "INAD", true);
        ReasonDisableOutput output = ReasonDisableOutput.builder().id(1L).code("INAD").build();

        when(reasonDisableRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonDisableMapper.toReasonDisableOutput(existing)).thenReturn(output);

        Page<ReasonDisableOutput> result = reasonDisableServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonDisableRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        ReasonDisable existing = reasonDisable(1L, "INAD", true);
        ReasonDisableOutput output = ReasonDisableOutput.builder().id(1L).code("INAD").build();

        when(reasonDisableRepository.findAllFiltered(EntityType.LOGIN, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonDisableMapper.toReasonDisableOutput(existing)).thenReturn(output);

        Page<ReasonDisableOutput> result = reasonDisableServiceBean.findAll(EntityType.LOGIN, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonDisableRepository).findAllFiltered(eq(EntityType.LOGIN), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveReasonDisable() {
        ReasonDisable inactive = reasonDisable(1L, "INAD", false);
        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(inactive));

        reasonDisableServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        ReasonDisable active = reasonDisable(1L, "INAD", true);
        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reasonDisableServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonDisableDoesNotExist() {
        when(reasonDisableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonDisableServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveReasonDisable() {
        ReasonDisable active = reasonDisable(1L, "INAD", true);
        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(active));

        reasonDisableServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        ReasonDisable inactive = reasonDisable(1L, "INAD", false);
        when(reasonDisableRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> reasonDisableServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonDisableDoesNotExist() {
        when(reasonDisableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonDisableServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_DISABLE_001.getCode());
    }
}
