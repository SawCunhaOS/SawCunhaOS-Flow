
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

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.application.dto.configuration.ConfigurationDTO;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import br.com.sawcunhaos.organization.domain.repository.configuration.PartnersConfigurationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetConfigurationUseCaseImpl")
class GetConfigurationUseCaseImplTest {

    @Mock
    PartnersConfigurationRepository repository;

    @InjectMocks
    GetConfigurationUseCaseImpl useCase;

    @Nested
    @DisplayName("when configuration exists")
    class WhenExists {

        @Test
        @DisplayName("returns DTO with all fields")
        void returnsDTO() {
            var conf = PartnersConfiguration.builder()
                    .id("TOKEN_EXPIRY_MINUTES").value("30").type("INTEGER").build();
            given(repository.findById("TOKEN_EXPIRY_MINUTES")).willReturn(Optional.of(conf));

            ConfigurationDTO result = useCase.execute("TOKEN_EXPIRY_MINUTES");

            assertThat(result.id()).isEqualTo("TOKEN_EXPIRY_MINUTES");
            assertThat(result.value()).isEqualTo("30");
            assertThat(result.type()).isEqualTo("INTEGER");
        }

        @Test
        @DisplayName("masks value for sensitive key")
        void masksValueForSensitiveKey() {
            var conf = PartnersConfiguration.builder()
                    .id("DB_PASSWORD").value("real-secret").type("STRING").build();
            given(repository.findById("DB_PASSWORD")).willReturn(Optional.of(conf));

            ConfigurationDTO result = useCase.execute("DB_PASSWORD");

            assertThat(result.value()).isEqualTo("****");
        }

        @Test
        @DisplayName("does not mask value for non-sensitive key")
        void doesNotMaskNonSensitiveKey() {
            var conf = PartnersConfiguration.builder()
                    .id("MAX_RETRIES").value("5").type("INTEGER").build();
            given(repository.findById("MAX_RETRIES")).willReturn(Optional.of(conf));

            ConfigurationDTO result = useCase.execute("MAX_RETRIES");

            assertThat(result.value()).isEqualTo("5");
        }
    }

    @Nested
    @DisplayName("when configuration does not exist")
    class WhenNotFound {

        @Test
        @DisplayName("throws ScosException with SCOS_CONFIGURATION_001")
        void throwsScosExceptionWithCorrectCode() {
            given(repository.findById("NAO_EXISTE")).willReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute("NAO_EXISTE"))
                    .isInstanceOf(ScosException.class)
                    .extracting(e -> ((ScosException) e).getCode())
                    .isEqualTo(ExceptionCodeError.SCOS_CONFIGURATION_001.getCode());
        }
    }
}
