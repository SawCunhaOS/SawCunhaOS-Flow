
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

package br.com.sawcunhaos.organization.application.usecase.configuration;

import br.com.sawcunhaos.organization.application.dto.configuration.ConfigurationDTO;
import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import br.com.sawcunhaos.organization.domain.repository.configuration.PartnersConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListConfigurationsUseCaseImpl")
class ListConfigurationsUseCaseImplTest {

    @Mock
    PartnersConfigurationRepository repository;

    @InjectMocks
    ListConfigurationsUseCaseImpl useCase;

    private static PartnersConfiguration config(String id, String value) {
        return PartnersConfiguration.builder().id(id).value(value).type("STRING").build();
    }

    @Nested
    @DisplayName("when listing configurations")
    class WhenListing {

        @Test
        @DisplayName("returns page content mapped to DTOs")
        void returnsPageContentMappedToDTOs() {
            var pageable = PageRequest.of(0, 10);
            var conf = config("MAX_RETRIES", "3");
            given(repository.findAll(pageable)).willReturn(new PageImpl<>(List.of(conf)));

            Page<ConfigurationDTO> result = useCase.execute(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo("MAX_RETRIES");
            assertThat(result.getContent().get(0).value()).isEqualTo("3");
        }

        @Test
        @DisplayName("non-sensitive key is not masked")
        void nonSensitiveKeyIsNotMasked() {
            var pageable = PageRequest.of(0, 10);
            given(repository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(config("TOKEN_EXPIRY_MINUTES", "30"))));

            ConfigurationDTO dto = useCase.execute(pageable).getContent().get(0);

            assertThat(dto.value()).isEqualTo("30");
        }
    }

    @Nested
    @DisplayName("when key has sensitive suffix")
    class WhenSensitive {

        @ParameterizedTest(name = "masks value for id ending with {0}")
        @ValueSource(strings = {"DB_PASSWORD", "API_SECRET", "JWT_TOKEN", "SIGNING_KEY"})
        @DisplayName("masks value with ****")
        void masksValueForSensitiveSuffix(String id) {
            var pageable = PageRequest.of(0, 10);
            given(repository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(config(id, "super-secret"))));

            ConfigurationDTO dto = useCase.execute(pageable).getContent().get(0);

            assertThat(dto.value()).isEqualTo("****");
        }

        @Test
        @DisplayName("case-insensitive match on suffix")
        void caseInsensitiveMatch() {
            var pageable = PageRequest.of(0, 10);
            given(repository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(config("db_password", "secret"))));

            ConfigurationDTO dto = useCase.execute(pageable).getContent().get(0);

            assertThat(dto.value()).isEqualTo("****");
        }

        @Test
        @DisplayName("preserves id and type even when value is masked")
        void preservesIdAndTypeWhenMasked() {
            var pageable = PageRequest.of(0, 10);
            var conf = PartnersConfiguration.builder().id("DB_PASSWORD").value("secret").type("STRING").build();
            given(repository.findAll(pageable)).willReturn(new PageImpl<>(List.of(conf)));

            ConfigurationDTO dto = useCase.execute(pageable).getContent().get(0);

            assertThat(dto.id()).isEqualTo("DB_PASSWORD");
            assertThat(dto.type()).isEqualTo("STRING");
            assertThat(dto.value()).isEqualTo("****");
        }
    }
}
