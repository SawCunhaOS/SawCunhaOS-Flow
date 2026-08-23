
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonActivateRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_ACTIVATE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ReasonActivateServiceBean}: unicidade de {@code code} por catálogo,
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class ReasonActivateServiceBeanTest {

    @Mock
    private ReasonActivateRepository reasonActivateRepository;
    @Mock
    private ReasonActivateMapper reasonActivateMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ReasonActivateServiceBean reasonActivateServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<ReasonActivate, Long> asBaseJpaRepository() {
        return reasonActivateRepository;
    }

    private ReasonActivate reasonActivate(Long id, String code, boolean active) {
        return ReasonActivate.builder().id(id).code(code).description("Renovação de contrato").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        ReasonActivateInput input = ReasonActivateInput.builder().code("RENOV").description("Renovação de contrato").entityType(EntityType.COMPANY).build();
        ReasonActivate persisted = reasonActivate(1L, "RENOV", true);

        when(reasonActivateRepository.existsByCodeAndEntityType("RENOV", EntityType.COMPANY)).thenReturn(false);
        when(reasonActivateRepository.merge(any(ReasonActivate.class))).thenReturn(persisted);

        ReasonActivateOutput result = reasonActivateServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("RENOV");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        ReasonActivateInput input = ReasonActivateInput.builder().code("RENOV").description("Renovação de contrato").entityType(EntityType.COMPANY).build();

        when(reasonActivateRepository.existsByCodeAndEntityType("RENOV", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> reasonActivateServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_002.getCode());

        verify(reasonActivateRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenReasonActivateDoesNotExist() {
        ReasonActivateInput input = ReasonActivateInput.builder().id(999L).code("RENOV").description("Renovação de contrato").entityType(EntityType.COMPANY).build();

        when(reasonActivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonActivateServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherReasonActivate() {
        ReasonActivate existing = reasonActivate(1L, "RENOV", true);
        ReasonActivateInput input = ReasonActivateInput.builder().id(1L).code("OUTRO").description("Renovação de contrato").entityType(EntityType.COMPANY).build();

        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonActivateRepository.existsByCodeAndEntityTypeAndNotId("OUTRO", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reasonActivateServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        ReasonActivate existing = reasonActivate(1L, "RENOV", true);
        ReasonActivateInput input = ReasonActivateInput.builder().id(1L).code("RENOV").description("Renovação atualizada").entityType(EntityType.EMPLOYEE).build();

        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonActivateRepository.existsByCodeAndEntityTypeAndNotId("RENOV", EntityType.EMPLOYEE, 1L)).thenReturn(false);

        reasonActivateServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Renovação atualizada");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(reasonActivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonActivateServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        ReasonActivate existing = reasonActivate(1L, "RENOV", true);
        ReasonActivateOutput output = ReasonActivateOutput.builder().id(1L).code("RENOV").description("Renovação de contrato").entityType(EntityType.COMPANY).active(true).build();

        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonActivateMapper.toReasonActivateOutput(existing)).thenReturn(output);

        ReasonActivateOutput result = reasonActivateServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        ReasonActivate existing = reasonActivate(1L, "RENOV", true);
        ReasonActivateOutput output = ReasonActivateOutput.builder().id(1L).code("RENOV").build();

        when(reasonActivateRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonActivateMapper.toReasonActivateOutput(existing)).thenReturn(output);

        Page<ReasonActivateOutput> result = reasonActivateServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonActivateRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        ReasonActivate existing = reasonActivate(1L, "RENOV", true);
        ReasonActivateOutput output = ReasonActivateOutput.builder().id(1L).code("RENOV").build();

        when(reasonActivateRepository.findAllFiltered(EntityType.LOGIN, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonActivateMapper.toReasonActivateOutput(existing)).thenReturn(output);

        Page<ReasonActivateOutput> result = reasonActivateServiceBean.findAll(EntityType.LOGIN, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonActivateRepository).findAllFiltered(eq(EntityType.LOGIN), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveReasonActivate() {
        ReasonActivate inactive = reasonActivate(1L, "RENOV", false);
        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(inactive));

        reasonActivateServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        ReasonActivate active = reasonActivate(1L, "RENOV", true);
        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reasonActivateServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonActivateDoesNotExist() {
        when(reasonActivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonActivateServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveReasonActivate() {
        ReasonActivate active = reasonActivate(1L, "RENOV", true);
        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(active));

        reasonActivateServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        ReasonActivate inactive = reasonActivate(1L, "RENOV", false);
        when(reasonActivateRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> reasonActivateServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonActivateDoesNotExist() {
        when(reasonActivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonActivateServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_ACTIVATE_001.getCode());
    }
}
