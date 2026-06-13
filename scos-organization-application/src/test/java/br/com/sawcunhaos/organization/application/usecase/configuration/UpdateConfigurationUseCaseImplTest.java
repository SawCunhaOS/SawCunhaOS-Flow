
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
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.configuration.PartnersConfiguration;
import br.com.sawcunhaos.organization.domain.repository.configuration.PartnersConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateConfigurationUseCaseImpl")
class UpdateConfigurationUseCaseImplTest {

    @Mock
    PartnersConfigurationRepository repository;

    UpdateConfigurationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateConfigurationUseCaseImpl(repository, new ObjectMapper());
    }

    private PartnersConfiguration configWithType(String id, String type) {
        return PartnersConfiguration.builder().id(id).value("old").type(type).build();
    }

    @Nested
    @DisplayName("when configuration does not exist")
    class WhenNotFound {

        @Test
        @DisplayName("throws ScosException with SCOS_CONFIGURATION_001")
        void throwsScosExceptionWithCorrectCode() {
            given(repository.findById("NAO_EXISTE")).willReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute("NAO_EXISTE", "value"))
                    .isInstanceOf(ScosException.class)
                    .extracting(e -> ((ScosException) e).getCode())
                    .isEqualTo(ExceptionCodeError.SCOS_CONFIGURATION_001.getCode());

            then(repository).should().findById("NAO_EXISTE");
            then(repository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("type validation — STRING")
    class TypeString {

        @Test
        @DisplayName("accepts any string value")
        void acceptsAnyStringValue() {
            var conf = configWithType("FEATURE_NAME", "STRING");
            given(repository.findById("FEATURE_NAME")).willReturn(Optional.of(conf));

            assertThatCode(() -> useCase.execute("FEATURE_NAME", "qualquer-valor"))
                    .doesNotThrowAnyException();

            then(repository).should().merge(conf);
        }
    }

    @Nested
    @DisplayName("type validation — INTEGER")
    class TypeInteger {

        @ParameterizedTest(name = "accepts valid integer: {0}")
        @CsvSource({"0", "42", "-1", "9999999"})
        @DisplayName("accepts valid integer values")
        void acceptsValidIntegers(String value) {
            var conf = configWithType("MAX_RETRIES", "INTEGER");
            given(repository.findById("MAX_RETRIES")).willReturn(Optional.of(conf));

            assertThatCode(() -> useCase.execute("MAX_RETRIES", value))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "rejects invalid integer: {0}")
        @CsvSource({"abc", "1.5", "true", "nulo"})
        @DisplayName("throws SCOS_CONFIGURATION_002 for invalid integer")
        void rejectsInvalidIntegers(String value) {
            var conf = configWithType("MAX_RETRIES", "INTEGER");
            given(repository.findById("MAX_RETRIES")).willReturn(Optional.of(conf));

            assertThatThrownBy(() -> useCase.execute("MAX_RETRIES", value))
                    .isInstanceOf(ScosException.class)
                    .extracting(e -> ((ScosException) e).getCode())
                    .isEqualTo(ExceptionCodeError.SCOS_CONFIGURATION_002.getCode());

            then(repository).should().findById("MAX_RETRIES");
            then(repository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("type validation — BOOLEAN")
    class TypeBoolean {

        @ParameterizedTest(name = "accepts valid boolean: {0}")
        @CsvSource({"true", "false", "TRUE", "FALSE", "True", "False"})
        @DisplayName("accepts true and false (case-insensitive)")
        void acceptsValidBooleans(String value) {
            var conf = configWithType("FEATURE_FLAG", "BOOLEAN");
            given(repository.findById("FEATURE_FLAG")).willReturn(Optional.of(conf));

            assertThatCode(() -> useCase.execute("FEATURE_FLAG", value))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "rejects invalid boolean: {0}")
        @CsvSource({"yes", "no", "1", "0", "sim", "verdadeiro"})
        @DisplayName("throws SCOS_CONFIGURATION_002 for non-boolean value")
        void rejectsInvalidBooleans(String value) {
            var conf = configWithType("FEATURE_FLAG", "BOOLEAN");
            given(repository.findById("FEATURE_FLAG")).willReturn(Optional.of(conf));

            assertThatThrownBy(() -> useCase.execute("FEATURE_FLAG", value))
                    .isInstanceOf(ScosException.class)
                    .extracting(e -> ((ScosException) e).getCode())
                    .isEqualTo(ExceptionCodeError.SCOS_CONFIGURATION_002.getCode());
        }
    }

    @Nested
    @DisplayName("type validation — JSON")
    class TypeJson {

        @ParameterizedTest(name = "accepts valid JSON: {0}")
        @CsvSource(value = {
                "{\"key\":\"value\"}",
                "[1,2,3]",
                "\"string\"",
                "42"
        }, delimiterString = "||")
        @DisplayName("accepts any valid JSON")
        void acceptsValidJson(String value) {
            var conf = configWithType("COMPLEX_CONFIG", "JSON");
            given(repository.findById("COMPLEX_CONFIG")).willReturn(Optional.of(conf));

            assertThatCode(() -> useCase.execute("COMPLEX_CONFIG", value))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "rejects invalid JSON: {0}")
        @CsvSource(value = {"{invalido", "sem-aspas", "{key: value}"}, delimiterString = "||")
        @DisplayName("throws SCOS_CONFIGURATION_002 for invalid JSON")
        void rejectsInvalidJson(String value) {
            var conf = configWithType("COMPLEX_CONFIG", "JSON");
            given(repository.findById("COMPLEX_CONFIG")).willReturn(Optional.of(conf));

            assertThatThrownBy(() -> useCase.execute("COMPLEX_CONFIG", value))
                    .isInstanceOf(ScosException.class)
                    .extracting(e -> ((ScosException) e).getCode())
                    .isEqualTo(ExceptionCodeError.SCOS_CONFIGURATION_002.getCode());
        }
    }

    @Nested
    @DisplayName("when update succeeds")
    class WhenSucceeds {

        @Test
        @DisplayName("persists new value via merge")
        void persistsNewValueViaMerge() {
            var conf = configWithType("TOKEN_EXPIRY_MINUTES", "INTEGER");
            given(repository.findById("TOKEN_EXPIRY_MINUTES")).willReturn(Optional.of(conf));

            useCase.execute("TOKEN_EXPIRY_MINUTES", "60");

            then(repository).should().merge(any());
        }
    }
}
