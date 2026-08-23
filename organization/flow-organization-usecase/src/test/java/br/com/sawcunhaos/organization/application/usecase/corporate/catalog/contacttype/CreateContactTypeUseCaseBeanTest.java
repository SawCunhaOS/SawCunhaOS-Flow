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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.ContactType;
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.CreateContactTypeRequest;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.ContactTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.ContactTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/** Testes de {@link CreateContactTypeUseCaseBean}: mapeamento request→input, output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class CreateContactTypeUseCaseBeanTest {

    @Mock
    private ContactTypeService contactTypeService;

    @InjectMocks
    private CreateContactTypeUseCaseBean useCase;

    @Test
    @DisplayName("mapeia request para input (id nulo) e retorna o output mapeado")
    void executes_mapsRequestToInputAndReturnsMappedOutput() {
        CreateContactTypeRequest request =
                new CreateContactTypeRequest("HOME", "Home address", CatalogEntityType.COMPANY);
        ContactTypeOutput output = ContactTypeOutput.builder()
                .id(1L).code("HOME").description("Home address")
                .entityType(EntityType.COMPANY).active(true).build();
        given(contactTypeService.create(any(ContactTypeInput.class))).willReturn(output);

        ContactType result = useCase.execute(request);

        ArgumentCaptor<ContactTypeInput> captor = ArgumentCaptor.forClass(ContactTypeInput.class);
        then(contactTypeService).should().create(captor.capture());
        ContactTypeInput input = captor.getValue();
        assertThat(input.id()).isNull();
        assertThat(input.code()).isEqualTo("HOME");
        assertThat(input.description()).isEqualTo("Home address");
        assertThat(input.entityType()).isEqualTo(EntityType.COMPANY);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("HOME");
        assertThat(result.description()).isEqualTo("Home address");
        assertThat(result.entityType()).isEqualTo(CatalogEntityType.COMPANY);
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("propaga ScosException quando o service falha")
    void whenServiceThrows_propagatesScosException() {
        CreateContactTypeRequest request =
                new CreateContactTypeRequest("HOME", "Home address", CatalogEntityType.COMPANY);
        given(contactTypeService.create(any(ContactTypeInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(contactTypeService).shouldHaveNoInteractions();
    }
}
