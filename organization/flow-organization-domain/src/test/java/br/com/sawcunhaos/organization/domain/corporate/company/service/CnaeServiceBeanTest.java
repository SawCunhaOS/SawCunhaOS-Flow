
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

package br.com.sawcunhaos.organization.domain.corporate.company.service;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Cnae;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CnaeRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyCnaeSecondaryRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CNAE_003;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link CnaeServiceBean}: unicidade de {@code code}, resolução de not-found
 * e guarda de exclusão física (vínculo principal ou secundário).
 */
@ExtendWith(MockitoExtension.class)
class CnaeServiceBeanTest {

    @Mock
    private CnaeRepository cnaeRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyCnaeSecondaryRepository companyCnaeSecondaryRepository;
    @Mock
    private CnaeMapper cnaeMapper;

    @InjectMocks
    private CnaeServiceBean cnaeServiceBean;

    private Cnae cnae(Long id, String code) {
        return Cnae.builder().id(id).code(code).description("Cultivo de arroz").build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        CnaeInput input = CnaeInput.builder().code("0111301").description("Cultivo de arroz").build();
        Cnae persisted = cnae(1L, "0111301");
        CnaeOutput output = CnaeOutput.builder().id(1L).code("0111301").description("Cultivo de arroz").build();

        when(cnaeRepository.existsByCode("0111301")).thenReturn(false);
        when(cnaeRepository.merge(any(Cnae.class))).thenReturn(persisted);
        when(cnaeMapper.toCnaeOutput(persisted)).thenReturn(output);

        CnaeOutput result = cnaeServiceBean.create(input);

        assertThat(result).isEqualTo(output);
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        CnaeInput input = CnaeInput.builder().code("0111301").description("Cultivo de arroz").build();

        when(cnaeRepository.existsByCode("0111301")).thenReturn(true);

        assertThatThrownBy(() -> cnaeServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_002.getCode());

        verify(cnaeRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenCnaeDoesNotExist() {
        CnaeInput input = CnaeInput.builder().id(999L).code("0111301").description("Cultivo de arroz").build();

        when(cnaeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cnaeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherCnae() {
        Cnae existing = cnae(1L, "0111301");
        CnaeInput input = CnaeInput.builder().id(1L).code("0111302").description("Cultivo de milho").build();

        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cnaeRepository.existsByCodeAndNotId("0111302", 1L)).thenReturn(true);

        assertThatThrownBy(() -> cnaeServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_002.getCode());

        verify(cnaeRepository, never()).update(any(Cnae.class));
    }

    @Test
    void updateShouldPersistWhenValid() {
        Cnae existing = cnae(1L, "0111301");
        CnaeInput input = CnaeInput.builder().id(1L).code("0111301").description("Cultivo de arroz atualizado").build();

        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cnaeRepository.existsByCodeAndNotId("0111301", 1L)).thenReturn(false);
        when(cnaeRepository.update(existing)).thenReturn(existing);

        cnaeServiceBean.update(input);

        verify(cnaeRepository).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Cultivo de arroz atualizado");
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(cnaeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cnaeServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        Cnae existing = cnae(1L, "0111301");
        CnaeOutput output = CnaeOutput.builder().id(1L).code("0111301").description("Cultivo de arroz").build();

        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cnaeMapper.toCnaeOutput(existing)).thenReturn(output);

        CnaeOutput result = cnaeServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnMappedPage() {
        Pageable pageable = Pageable.unpaged();
        Cnae existing = cnae(1L, "0111301");
        CnaeOutput output = CnaeOutput.builder().id(1L).code("0111301").build();

        when(cnaeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(cnaeMapper.toCnaeOutput(existing)).thenReturn(output);

        Page<CnaeOutput> result = cnaeServiceBean.findAll(pageable);

        assertThat(result.getContent()).containsExactly(output);
    }

    // ---- delete ----

    @Test
    void deleteShouldThrowWhenCnaeDoesNotExist() {
        when(cnaeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cnaeServiceBean.delete(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_001.getCode());

        verify(cnaeRepository, never()).delete(any(Cnae.class));
    }

    @Test
    void deleteShouldThrowWhenReferencedAsPrincipal() {
        Cnae existing = cnae(1L, "0111301");
        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByCnaePrincipalId(1L)).thenReturn(true);

        assertThatThrownBy(() -> cnaeServiceBean.delete(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_003.getCode());

        verify(cnaeRepository, never()).delete(any(Cnae.class));
    }

    @Test
    void deleteShouldThrowWhenReferencedAsSecondary() {
        Cnae existing = cnae(1L, "0111301");
        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByCnaePrincipalId(1L)).thenReturn(false);
        when(companyCnaeSecondaryRepository.existsByCnaeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> cnaeServiceBean.delete(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_CNAE_003.getCode());

        verify(cnaeRepository, never()).delete(any(Cnae.class));
    }

    @Test
    void deleteShouldRemoveWhenNotReferenced() {
        Cnae existing = cnae(1L, "0111301");
        when(cnaeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByCnaePrincipalId(1L)).thenReturn(false);
        when(companyCnaeSecondaryRepository.existsByCnaeId(1L)).thenReturn(false);

        cnaeServiceBean.delete(1L);

        verify(cnaeRepository).delete(existing);
    }
}
