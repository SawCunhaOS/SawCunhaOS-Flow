
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

package br.com.sawcunhaos.organization.domain.corporate.catalog.service;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.ContactTypeRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONTACT_TYPE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ContactTypeServiceBean}: unicidade de {@code code} por {@code entityType},
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class ContactTypeServiceBeanTest {

    @Mock
    private ContactTypeRepository contactTypeRepository;
    @Mock
    private ContactTypeMapper contactTypeMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private ContactTypeServiceBean contactTypeServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<ContactType, Long> asBaseJpaRepository() {
        return contactTypeRepository;
    }

    private ContactType contactType(Long id, String code, boolean active) {
        return ContactType.builder().id(id).code(code).description("Telefone").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        ContactTypeInput input = ContactTypeInput.builder().code("TEL").description("Telefone").entityType(EntityType.COMPANY).build();
        ContactType persisted = contactType(1L, "TEL", true);

        when(contactTypeRepository.existsByCodeAndEntityType("TEL", EntityType.COMPANY)).thenReturn(false);
        when(contactTypeRepository.merge(any(ContactType.class))).thenReturn(persisted);

        ContactTypeOutput result = contactTypeServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("TEL");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        ContactTypeInput input = ContactTypeInput.builder().code("TEL").description("Telefone").entityType(EntityType.COMPANY).build();

        when(contactTypeRepository.existsByCodeAndEntityType("TEL", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> contactTypeServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_002.getCode());

        verify(contactTypeRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenContactTypeDoesNotExist() {
        ContactTypeInput input = ContactTypeInput.builder().id(999L).code("TEL").description("Telefone").entityType(EntityType.COMPANY).build();

        when(contactTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactTypeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherContactType() {
        ContactType existing = contactType(1L, "TEL", true);
        ContactTypeInput input = ContactTypeInput.builder().id(1L).code("EMAIL").description("Telefone").entityType(EntityType.COMPANY).build();

        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contactTypeRepository.existsByCodeAndEntityAndNotId( "EMAIL", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> contactTypeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        ContactType existing = contactType(1L, "TEL", true);
        ContactTypeInput input = ContactTypeInput.builder().id(1L).code("TEL").description("Telefone atualizado").entityType(EntityType.EMPLOYEE).build();

        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contactTypeRepository.existsByCodeAndEntityAndNotId( "TEL", EntityType.EMPLOYEE, 1L)).thenReturn(false);
        when(contactTypeRepository.update(existing)).thenReturn(existing);

        contactTypeServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Telefone atualizado");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(contactTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactTypeServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        ContactType existing = contactType(1L, "TEL", true);
        ContactTypeOutput output = ContactTypeOutput.builder().id(1L).code("TEL").description("Telefone").entityType(EntityType.COMPANY).active(true).build();

        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(contactTypeMapper.toContactTypeOutput(existing)).thenReturn(output);

        ContactTypeOutput result = contactTypeServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        ContactType existing = contactType(1L, "TEL", true);
        ContactTypeOutput output = ContactTypeOutput.builder().id(1L).code("TEL").build();

        when(contactTypeRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(contactTypeMapper.toContactTypeOutput(existing)).thenReturn(output);

        Page<ContactTypeOutput> result = contactTypeServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(contactTypeRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        ContactType existing = contactType(1L, "TEL", true);
        ContactTypeOutput output = ContactTypeOutput.builder().id(1L).code("TEL").build();

        when(contactTypeRepository.findAllFiltered(EntityType.COMPANY, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(contactTypeMapper.toContactTypeOutput(existing)).thenReturn(output);

        Page<ContactTypeOutput> result = contactTypeServiceBean.findAll(EntityType.COMPANY, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(contactTypeRepository).findAllFiltered(eq(EntityType.COMPANY), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveContactType() {
        ContactType inactive = contactType(1L, "TEL", false);
        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        contactTypeServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        ContactType active = contactType(1L, "TEL", true);
        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> contactTypeServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenContactTypeDoesNotExist() {
        when(contactTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactTypeServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveContactType() {
        ContactType active = contactType(1L, "TEL", true);
        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(active));

        contactTypeServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        ContactType inactive = contactType(1L, "TEL", false);
        when(contactTypeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> contactTypeServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenContactTypeDoesNotExist() {
        when(contactTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactTypeServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CONTACT_TYPE_001.getCode());
    }
}
