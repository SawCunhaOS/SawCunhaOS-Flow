
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
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonEnableOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnable;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonEnableRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ENABLE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ReasonEnableServiceBean}: unicidade de {@code code} por catálogo,
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class ReasonEnableServiceBeanTest {

    @Mock
    private ReasonEnableRepository reasonEnableRepository;
    @Mock
    private ReasonEnableMapper reasonEnableMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ReasonEnableServiceBean reasonEnableServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<ReasonEnable, Long> asBaseJpaRepository() {
        return reasonEnableRepository;
    }

    private ReasonEnable reasonEnable(Long id, String code, boolean active) {
        return ReasonEnable.builder().id(id).code(code).description("Regularização de pendência").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        ReasonEnableInput input = ReasonEnableInput.builder().code("REGUL").description("Regularização de pendência").entityType(EntityType.COMPANY).build();
        ReasonEnable persisted = reasonEnable(1L, "REGUL", true);

        when(reasonEnableRepository.existsByCodeAndEntityType("REGUL", EntityType.COMPANY)).thenReturn(false);
        when(reasonEnableRepository.merge(any(ReasonEnable.class))).thenReturn(persisted);

        ReasonEnableOutput result = reasonEnableServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("REGUL");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        ReasonEnableInput input = ReasonEnableInput.builder().code("REGUL").description("Regularização de pendência").entityType(EntityType.COMPANY).build();

        when(reasonEnableRepository.existsByCodeAndEntityType("REGUL", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> reasonEnableServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_002.getCode());

        verify(reasonEnableRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenReasonEnableDoesNotExist() {
        ReasonEnableInput input = ReasonEnableInput.builder().id(999L).code("REGUL").description("Regularização de pendência").entityType(EntityType.COMPANY).build();

        when(reasonEnableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonEnableServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherReasonEnable() {
        ReasonEnable existing = reasonEnable(1L, "REGUL", true);
        ReasonEnableInput input = ReasonEnableInput.builder().id(1L).code("OUTRO").description("Regularização de pendência").entityType(EntityType.COMPANY).build();

        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonEnableRepository.existsByCodeAndEntityTypeAndNotId("OUTRO", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reasonEnableServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        ReasonEnable existing = reasonEnable(1L, "REGUL", true);
        ReasonEnableInput input = ReasonEnableInput.builder().id(1L).code("REGUL").description("Regularização atualizada").entityType(EntityType.EMPLOYEE).build();

        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonEnableRepository.existsByCodeAndEntityTypeAndNotId("REGUL", EntityType.EMPLOYEE, 1L)).thenReturn(false);

        reasonEnableServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Regularização atualizada");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(reasonEnableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonEnableServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        ReasonEnable existing = reasonEnable(1L, "REGUL", true);
        ReasonEnableOutput output = ReasonEnableOutput.builder().id(1L).code("REGUL").description("Regularização de pendência").entityType(EntityType.COMPANY).active(true).build();

        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonEnableMapper.toReasonEnableOutput(existing)).thenReturn(output);

        ReasonEnableOutput result = reasonEnableServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        ReasonEnable existing = reasonEnable(1L, "REGUL", true);
        ReasonEnableOutput output = ReasonEnableOutput.builder().id(1L).code("REGUL").build();

        when(reasonEnableRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonEnableMapper.toReasonEnableOutput(existing)).thenReturn(output);

        Page<ReasonEnableOutput> result = reasonEnableServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonEnableRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        ReasonEnable existing = reasonEnable(1L, "REGUL", true);
        ReasonEnableOutput output = ReasonEnableOutput.builder().id(1L).code("REGUL").build();

        when(reasonEnableRepository.findAllFiltered(EntityType.LOGIN, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonEnableMapper.toReasonEnableOutput(existing)).thenReturn(output);

        Page<ReasonEnableOutput> result = reasonEnableServiceBean.findAll(EntityType.LOGIN, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonEnableRepository).findAllFiltered(eq(EntityType.LOGIN), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveReasonEnable() {
        ReasonEnable inactive = reasonEnable(1L, "REGUL", false);
        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(inactive));

        reasonEnableServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        ReasonEnable active = reasonEnable(1L, "REGUL", true);
        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reasonEnableServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonEnableDoesNotExist() {
        when(reasonEnableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonEnableServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveReasonEnable() {
        ReasonEnable active = reasonEnable(1L, "REGUL", true);
        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(active));

        reasonEnableServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        ReasonEnable inactive = reasonEnable(1L, "REGUL", false);
        when(reasonEnableRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> reasonEnableServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonEnableDoesNotExist() {
        when(reasonEnableRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonEnableServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ENABLE_001.getCode());
    }
}
