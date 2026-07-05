
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
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateInput;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivate;
import br.com.sawcunhaos.organization.domain.access.status.internal.ReasonInactivateRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_REASON_INACTIVATE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ReasonInactivateServiceBean}: unicidade de {@code code} por catálogo,
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class ReasonInactivateServiceBeanTest {

    @Mock
    private ReasonInactivateRepository reasonInactivateRepository;
    @Mock
    private ReasonInactivateMapper reasonInactivateMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ReasonInactivateServiceBean reasonInactivateServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<ReasonInactivate, Long> asBaseJpaRepository() {
        return reasonInactivateRepository;
    }

    private ReasonInactivate reasonInactivate(Long id, String code, boolean active) {
        return ReasonInactivate.builder().id(id).code(code).description("Fim de contrato").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        ReasonInactivateInput input = ReasonInactivateInput.builder().code("FIMCTR").description("Fim de contrato").entityType(EntityType.COMPANY).build();
        ReasonInactivate persisted = reasonInactivate(1L, "FIMCTR", true);

        when(reasonInactivateRepository.existsByCodeAndEntityType("FIMCTR", EntityType.COMPANY)).thenReturn(false);
        when(reasonInactivateRepository.merge(any(ReasonInactivate.class))).thenReturn(persisted);

        ReasonInactivateOutput result = reasonInactivateServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("FIMCTR");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        ReasonInactivateInput input = ReasonInactivateInput.builder().code("FIMCTR").description("Fim de contrato").entityType(EntityType.COMPANY).build();

        when(reasonInactivateRepository.existsByCodeAndEntityType("FIMCTR", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> reasonInactivateServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_002.getCode());

        verify(reasonInactivateRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenReasonInactivateDoesNotExist() {
        ReasonInactivateInput input = ReasonInactivateInput.builder().id(999L).code("FIMCTR").description("Fim de contrato").entityType(EntityType.COMPANY).build();

        when(reasonInactivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonInactivateServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherReasonInactivate() {
        ReasonInactivate existing = reasonInactivate(1L, "FIMCTR", true);
        ReasonInactivateInput input = ReasonInactivateInput.builder().id(1L).code("OUTRO").description("Fim de contrato").entityType(EntityType.COMPANY).build();

        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonInactivateRepository.existsByCodeAndEntityTypeAndNotId("OUTRO", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reasonInactivateServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        ReasonInactivate existing = reasonInactivate(1L, "FIMCTR", true);
        ReasonInactivateInput input = ReasonInactivateInput.builder().id(1L).code("FIMCTR").description("Fim de contrato atualizado").entityType(EntityType.EMPLOYEE).build();

        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonInactivateRepository.existsByCodeAndEntityTypeAndNotId("FIMCTR", EntityType.EMPLOYEE, 1L)).thenReturn(false);

        reasonInactivateServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Fim de contrato atualizado");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(reasonInactivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonInactivateServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        ReasonInactivate existing = reasonInactivate(1L, "FIMCTR", true);
        ReasonInactivateOutput output = ReasonInactivateOutput.builder().id(1L).code("FIMCTR").description("Fim de contrato").entityType(EntityType.COMPANY).active(true).build();

        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(reasonInactivateMapper.toReasonInactivateOutput(existing)).thenReturn(output);

        ReasonInactivateOutput result = reasonInactivateServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        ReasonInactivate existing = reasonInactivate(1L, "FIMCTR", true);
        ReasonInactivateOutput output = ReasonInactivateOutput.builder().id(1L).code("FIMCTR").build();

        when(reasonInactivateRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonInactivateMapper.toReasonInactivateOutput(existing)).thenReturn(output);

        Page<ReasonInactivateOutput> result = reasonInactivateServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonInactivateRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        ReasonInactivate existing = reasonInactivate(1L, "FIMCTR", true);
        ReasonInactivateOutput output = ReasonInactivateOutput.builder().id(1L).code("FIMCTR").build();

        when(reasonInactivateRepository.findAllFiltered(EntityType.LOGIN, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(reasonInactivateMapper.toReasonInactivateOutput(existing)).thenReturn(output);

        Page<ReasonInactivateOutput> result = reasonInactivateServiceBean.findAll(EntityType.LOGIN, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(reasonInactivateRepository).findAllFiltered(eq(EntityType.LOGIN), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveReasonInactivate() {
        ReasonInactivate inactive = reasonInactivate(1L, "FIMCTR", false);
        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(inactive));

        reasonInactivateServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        ReasonInactivate active = reasonInactivate(1L, "FIMCTR", true);
        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reasonInactivateServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenReasonInactivateDoesNotExist() {
        when(reasonInactivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonInactivateServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveReasonInactivate() {
        ReasonInactivate active = reasonInactivate(1L, "FIMCTR", true);
        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(active));

        reasonInactivateServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        ReasonInactivate inactive = reasonInactivate(1L, "FIMCTR", false);
        when(reasonInactivateRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> reasonInactivateServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenReasonInactivateDoesNotExist() {
        when(reasonInactivateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reasonInactivateServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_REASON_INACTIVATE_001.getCode());
    }
}
