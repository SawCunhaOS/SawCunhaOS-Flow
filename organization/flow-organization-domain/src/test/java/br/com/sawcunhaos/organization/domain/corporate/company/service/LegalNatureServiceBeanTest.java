
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
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNature;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.LegalNatureRepository;
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

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LEGAL_NATURE_003;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link LegalNatureServiceBean}: unicidade de {@code code}, resolução de not-found
 * e guarda de exclusão física (vínculo via {@code SCOS_COMPANY.legalNatureId}).
 */
@ExtendWith(MockitoExtension.class)
class LegalNatureServiceBeanTest {

    @Mock
    private LegalNatureRepository legalNatureRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private LegalNatureMapper legalNatureMapper;

    @InjectMocks
    private LegalNatureServiceBean legalNatureServiceBean;

    private LegalNature legalNature(Long id, String code) {
        return LegalNature.builder().id(id).code(code).description("Sociedade Empresária Limitada").build();
    }

    // ---- create ----

    @Test
    void createShouldPersistWhenCodeIsUnique() {
        LegalNatureInput input = LegalNatureInput.builder().code("2062").description("Sociedade Empresária Limitada").build();
        LegalNature persisted = legalNature(1L, "2062");
        LegalNatureOutput output = LegalNatureOutput.builder().id(1L).code("2062").description("Sociedade Empresária Limitada").build();

        when(legalNatureRepository.existsByCode("2062")).thenReturn(false);
        when(legalNatureRepository.merge(any(LegalNature.class))).thenReturn(persisted);
        when(legalNatureMapper.toLegalNatureOutput(persisted)).thenReturn(output);

        LegalNatureOutput result = legalNatureServiceBean.create(input);

        assertThat(result).isEqualTo(output);
    }

    @Test
    void createShouldThrowWhenCodeAlreadyExists() {
        LegalNatureInput input = LegalNatureInput.builder().code("2062").description("Sociedade Empresária Limitada").build();

        when(legalNatureRepository.existsByCode("2062")).thenReturn(true);

        assertThatThrownBy(() -> legalNatureServiceBean.create(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_002.getCode());

        verify(legalNatureRepository, never()).merge(any());
    }

    // ---- update ----

    @Test
    void updateShouldThrowWhenLegalNatureDoesNotExist() {
        LegalNatureInput input = LegalNatureInput.builder().id(999L).code("2062").description("x").build();

        when(legalNatureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> legalNatureServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_001.getCode());
    }

    @Test
    void updateShouldThrowWhenCodeBelongsToAnotherLegalNature() {
        LegalNature existing = legalNature(1L, "2062");
        LegalNatureInput input = LegalNatureInput.builder().id(1L).code("2135").description("Empresário Individual").build();

        when(legalNatureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(legalNatureRepository.existsByCodeAndNotId("2135", 1L)).thenReturn(true);

        assertThatThrownBy(() -> legalNatureServiceBean.update(input))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_002.getCode());

        verify(legalNatureRepository, never()).update(any(LegalNature.class));
    }

    @Test
    void updateShouldPersistWhenValid() {
        LegalNature existing = legalNature(1L, "2062");
        LegalNatureInput input = LegalNatureInput.builder().id(1L).code("2062").description("Descrição atualizada").build();

        when(legalNatureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(legalNatureRepository.existsByCodeAndNotId("2062", 1L)).thenReturn(false);
        when(legalNatureRepository.update(existing)).thenReturn(existing);

        legalNatureServiceBean.update(input);

        verify(legalNatureRepository).update(existing);
        assertThat(existing.getDescription()).isEqualTo("Descrição atualizada");
    }

    // ---- findById ----

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(legalNatureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> legalNatureServiceBean.findById(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_001.getCode());
    }

    @Test
    void findByIdShouldReturnMappedOutput() {
        LegalNature existing = legalNature(1L, "2062");
        LegalNatureOutput output = LegalNatureOutput.builder().id(1L).code("2062").description("Sociedade Empresária Limitada").build();

        when(legalNatureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(legalNatureMapper.toLegalNatureOutput(existing)).thenReturn(output);

        LegalNatureOutput result = legalNatureServiceBean.findById(1L);

        assertThat(result).isEqualTo(output);
    }

    // ---- findAll ----

    @Test
    void findAllShouldReturnMappedPage() {
        Pageable pageable = Pageable.unpaged();
        LegalNature existing = legalNature(1L, "2062");
        LegalNatureOutput output = LegalNatureOutput.builder().id(1L).code("2062").build();

        when(legalNatureRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existing)));
        when(legalNatureMapper.toLegalNatureOutput(existing)).thenReturn(output);

        Page<LegalNatureOutput> result = legalNatureServiceBean.findAll(pageable);

        assertThat(result.getContent()).containsExactly(output);
    }

    // ---- delete ----

    @Test
    void deleteShouldThrowWhenLegalNatureDoesNotExist() {
        when(legalNatureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> legalNatureServiceBean.delete(999L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_001.getCode());

        verify(legalNatureRepository, never()).delete(any(LegalNature.class));
    }

    @Test
    void deleteShouldThrowWhenReferencedByCompany() {
        LegalNature existing = legalNature(1L, "2062");
        when(legalNatureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByLegalNatureId(1L)).thenReturn(true);

        assertThatThrownBy(() -> legalNatureServiceBean.delete(1L))
                .isInstanceOf(ScosException.class)
                .hasFieldOrPropertyWithValue("code", SCOS_LEGAL_NATURE_003.getCode());

        verify(legalNatureRepository, never()).delete(any(LegalNature.class));
    }

    @Test
    void deleteShouldRemoveWhenNotReferenced() {
        LegalNature existing = legalNature(1L, "2062");
        when(legalNatureRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.existsByLegalNatureId(1L)).thenReturn(false);

        legalNatureServiceBean.delete(1L);

        verify(legalNatureRepository).delete(existing);
    }
}
