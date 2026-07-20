
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
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.internal.AddressTypeRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_ADDRESS_TYPE_004;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link AddressTypeServiceBean}: unicidade de {@code code} por {@code entityType},
 * idempotência de {@code enable}/{@code disable} e resolução de not-found.
 */
@ExtendWith(MockitoExtension.class)
class AddressTypeServiceBeanTest {

    @Mock
    private AddressTypeRepository addressTypeRepository;
    @Mock
    private AddressTypeMapper addressTypeMapper;
    @Mock
    private ScosUserAuthentication scosUserAuthentication;

    @InjectMocks
    private AddressTypeServiceBean addressTypeServiceBean;

    @BeforeEach
    void setUp() {
        lenient().when(scosUserAuthentication.findUserAuthentication()).thenReturn("test-user");
    }

    private BaseJpaRepository<AddressType, Long> asBaseJpaRepository() {
        return addressTypeRepository;
    }

    private AddressType addressType(Long id, String code, boolean active) {
        return AddressType.builder().id(id).code(code).description("Residencial").entityType(EntityType.COMPANY).active(active).build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        AddressTypeInput input = AddressTypeInput.builder().code("RES").description("Residencial").entityType(EntityType.COMPANY).build();
        AddressType persisted = addressType(1L, "RES", true);

        when(addressTypeRepository.existsByCodeAndEntityType("RES", EntityType.COMPANY)).thenReturn(false);
        when(addressTypeRepository.merge(any(AddressType.class))).thenReturn(persisted);

        AddressTypeOutput result = addressTypeServiceBean.create(input);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("RES");
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        AddressTypeInput input = AddressTypeInput.builder().code("RES").description("Residencial").entityType(EntityType.COMPANY).build();

        when(addressTypeRepository.existsByCodeAndEntityType("RES", EntityType.COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> addressTypeServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_002.getCode());

        verify(addressTypeRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenAddressTypeDoesNotExist() {
        AddressTypeInput input = AddressTypeInput.builder().id(999L).code("RES").description("Residencial").entityType(EntityType.COMPANY).build();

        when(addressTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressTypeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherAddressType() {
        AddressType existing = addressType(1L, "RES", true);
        AddressTypeInput input = AddressTypeInput.builder().id(1L).code("COM").description("Residencial").entityType(EntityType.COMPANY).build();

        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressTypeRepository.existsByCodeAndEntityTypeAndNotId("COM", EntityType.COMPANY, 1L)).thenReturn(true);

        assertThatThrownBy(() -> addressTypeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_002.getCode());
    }

    @Test
    void updateShouldPersistWhenValid() {
        AddressType existing = addressType(1L, "RES", true);
        AddressTypeInput input = AddressTypeInput.builder().id(1L).code("RES").description("Residencial atualizado").entityType(EntityType.EMPLOYEE).build();

        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressTypeRepository.existsByCodeAndEntityTypeAndNotId("RES", EntityType.EMPLOYEE, 1L)).thenReturn(false);

        addressTypeServiceBean.update(input);

        verify(asBaseJpaRepository()).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Residencial atualizado");
        assertThat(existing.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(addressTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressTypeServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        AddressType existing = addressType(1L, "RES", true);
        AddressTypeOutput output = AddressTypeOutput.builder().id(1L).code("RES").description("Residencial").entityType(EntityType.COMPANY).active(true).build();

        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(addressTypeMapper.toAddressTypeOutput(existing)).thenReturn(output);

        AddressTypeOutput result = addressTypeServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnAllWhenEntityTypeFilterIsNull() {
        Pageable pageable = Pageable.unpaged();
        AddressType existing = addressType(1L, "RES", true);
        AddressTypeOutput output = AddressTypeOutput.builder().id(1L).code("RES").build();

        when(addressTypeRepository.findAllFiltered(null, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(addressTypeMapper.toAddressTypeOutput(existing)).thenReturn(output);

        Page<AddressTypeOutput> result = addressTypeServiceBean.findAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(addressTypeRepository).findAllFiltered(eq(null), eq(null), eq(pageable));
    }

    @Test
    void findAllShouldDelegateToRepositoryWithEntityTypeFilter() {
        Pageable pageable = Pageable.unpaged();
        AddressType existing = addressType(1L, "RES", true);
        AddressTypeOutput output = AddressTypeOutput.builder().id(1L).code("RES").build();

        when(addressTypeRepository.findAllFiltered(EntityType.COMPANY, null, pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(addressTypeMapper.toAddressTypeOutput(existing)).thenReturn(output);

        Page<AddressTypeOutput> result = addressTypeServiceBean.findAll(EntityType.COMPANY, null, pageable);

        assertThat(result.getContent()).containsExactly(output);
        verify(addressTypeRepository).findAllFiltered(eq(EntityType.COMPANY), eq(null), eq(pageable));
    }

    // ---- enable ----

    @Test
    void enableShouldActivateInactiveAddressType() {
        AddressType inactive = addressType(1L, "RES", false);
        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        addressTypeServiceBean.enable(1L);

        assertThat(inactive.isActive()).isTrue();
        verify(asBaseJpaRepository()).update(inactive);
    }

    @Test
    void enableShouldThrowWhenAlreadyActive() {
        AddressType active = addressType(1L, "RES", true);
        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> addressTypeServiceBean.enable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_003.getCode());
    }

    @Test
    void enableShouldThrowWhenAddressTypeDoesNotExist() {
        when(addressTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressTypeServiceBean.enable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_001.getCode());
    }

    // ---- disable ----

    @Test
    void disableShouldDeactivateActiveAddressType() {
        AddressType active = addressType(1L, "RES", true);
        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(active));

        addressTypeServiceBean.disable(1L);

        assertThat(active.isActive()).isFalse();
        verify(asBaseJpaRepository()).update(active);
    }

    @Test
    void disableShouldThrowWhenAlreadyInactive() {
        AddressType inactive = addressType(1L, "RES", false);
        when(addressTypeRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> addressTypeServiceBean.disable(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_004.getCode());
    }

    @Test
    void disableShouldThrowWhenAddressTypeDoesNotExist() {
        when(addressTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressTypeServiceBean.disable(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_ADDRESS_TYPE_001.getCode());
    }
}
