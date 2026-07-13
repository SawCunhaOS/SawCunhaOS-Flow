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

package br.com.sawcunhaos.organization.application.usecase.corporate.catalog.addresstype;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.api.dto.AddressType;
import br.com.sawcunhaos.organization.api.dto.CatalogEntityType;
import br.com.sawcunhaos.organization.api.dto.CreateAddressTypeRequest;
import br.com.sawcunhaos.organization.domain.access.status.internal.EntityType;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeInput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.dto.AddressTypeOutput;
import br.com.sawcunhaos.organization.domain.corporate.catalog.specification.AddressTypeService;
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

/** Testes de {@link CreateAddressTypeUseCaseBean}: mapeamento request→input, output→api e propagação. */
@ExtendWith(MockitoExtension.class)
class CreateAddressTypeUseCaseBeanTest {

    @Mock
    private AddressTypeService addressTypeService;

    @InjectMocks
    private CreateAddressTypeUseCaseBean useCase;

    @Test
    @DisplayName("mapeia request para input (id nulo) e retorna o output mapeado")
    void executes_mapsRequestToInputAndReturnsMappedOutput() {
        CreateAddressTypeRequest request =
                new CreateAddressTypeRequest("HOME", "Home address", CatalogEntityType.COMPANY);
        AddressTypeOutput output = AddressTypeOutput.builder()
                .id(1L).code("HOME").description("Home address")
                .entityType(EntityType.COMPANY).active(true).build();
        given(addressTypeService.create(any(AddressTypeInput.class))).willReturn(output);

        AddressType result = useCase.execute(request);

        ArgumentCaptor<AddressTypeInput> captor = ArgumentCaptor.forClass(AddressTypeInput.class);
        then(addressTypeService).should().create(captor.capture());
        AddressTypeInput input = captor.getValue();
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
        CreateAddressTypeRequest request =
                new CreateAddressTypeRequest("HOME", "Home address", CatalogEntityType.COMPANY);
        given(addressTypeService.create(any(AddressTypeInput.class))).willThrow(new ScosException());

        assertThatThrownBy(() -> useCase.execute(request)).isInstanceOf(ScosException.class);
    }

    @Test
    @DisplayName("request nulo lança NullPointerException e não chama o service")
    void nullRequest_throwsNullPointerExceptionAndDoesNotCallService() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(NullPointerException.class);
        then(addressTypeService).shouldHaveNoInteractions();
    }
}
