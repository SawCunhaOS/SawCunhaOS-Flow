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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.contacttype;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.ContactType;
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.ContactTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link FindContactTypeUseCaseBean}: delegação do id e mapeamento output→api. */
@ExtendWith(MockitoExtension.class)
class FindContactTypeUseCaseBeanTest {

    @Mock
    private ContactTypeService contactTypeService;

    @InjectMocks
    private FindContactTypeUseCaseBean useCase;

    @Test
    @DisplayName("delega o id ao service e retorna o output mapeado")
    void executes_delegatesIdAndReturnsMappedOutput() {
        ContactTypeOutput output = ContactTypeOutput.builder()
                .id(5L).code("HOME").description("Home address")
                .entityType(EntityType.COMPANY).active(true).build();
        given(contactTypeService.findById(5L)).willReturn(output);

        ContactType result = useCase.execute(5L);

        then(contactTypeService).should().findById(5L);
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.code()).isEqualTo("HOME");
        assertThat(result.description()).isEqualTo("Home address");
        assertThat(result.entityType()).isEqualTo(CatalogEntityType.COMPANY);
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        given(contactTypeService.findById(any())).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(99L)).isInstanceOf(ScosException.class);
    }
}
