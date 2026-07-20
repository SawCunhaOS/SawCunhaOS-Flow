
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

package br.com.sawcunhaos.organization.domain.access.system.service;

import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystem;
import br.com.sawcunhaos.organization.domain.access.system.internal.ScosSystemRepository;
import br.com.sawcunhaos.organization.shared.utils.SystemSecretCryptoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_002;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ScosSystemServiceBean#validateSecretKey}, incluindo o grace period do secret
 * anterior via {@link Clock#fixed}. {@code Clock} não é mockado (o teste quer controlar o instante,
 * não verificar interação) — o bean é construído manualmente em cada teste, sem {@code @InjectMocks}.
 */
@ExtendWith(MockitoExtension.class)
class ScosSystemServiceBeanTest {

    private static final String SYSTEM_CODE = "SYSTEM_CODE";
    private static final Instant NOW = Instant.parse("2026-07-19T10:00:00Z");

    @Mock
    private ScosSystemRepository scosSystemRepository;
    @Mock
    private SystemSecretCryptoService systemSecretCryptoService;

    private ScosSystemServiceBean serviceAt(Instant fixedNow) {
        return new ScosSystemServiceBean(
                scosSystemRepository, systemSecretCryptoService, Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    private ScosSystem systemWithSecret(String secretKey) {
        return ScosSystem.builder()
                .code(SYSTEM_CODE)
                .secretKey(secretKey)
                .status("ACTIVE")
                .build();
    }

    private ScosSystem systemWithSecretAndGrace(String secretKey, String previousSecretKey, Instant previousSecretExpiresAt) {
        return ScosSystem.builder()
                .code(SYSTEM_CODE)
                .secretKey(secretKey)
                .previousSecretKey(previousSecretKey)
                .previousSecretExpiresAt(previousSecretExpiresAt)
                .status("ACTIVE")
                .build();
    }

    @Test
    void validateSecretKeyShouldNotThrowWhenSecretMatches() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecret("correct-secret")));

        assertThatCode(() -> serviceAt(NOW).validateSecretKey(SYSTEM_CODE, "correct-secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSecretKeyShouldNotThrowWhenPreviousSecretWithinGraceWindow() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecretAndGrace(
                        "current-secret", "previous-secret", NOW.plusSeconds(3600))));

        assertThatCode(() -> serviceAt(NOW).validateSecretKey(SYSTEM_CODE, "previous-secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSecretKeyShouldThrowScosSystem002WhenPreviousSecretOutsideGraceWindow() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecretAndGrace(
                        "current-secret", "previous-secret", NOW.minusSeconds(1))));

        assertThatThrownBy(() -> serviceAt(NOW).validateSecretKey(SYSTEM_CODE, "previous-secret"))
                .hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_002.getCode());
    }

    @Test
    void validateSecretKeyShouldThrowScosSystem002WhenSecretDoesNotMatchAndNoGrace() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecret("correct-secret")));

        assertThatThrownBy(() -> serviceAt(NOW).validateSecretKey(SYSTEM_CODE, "wrong-secret"))
                .hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_002.getCode());
    }

    @Test
    void validateSecretKeyShouldThrowScosSystem001WhenCodeNotFound() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt(NOW).validateSecretKey(SYSTEM_CODE, "any-secret"))
                .hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_001.getCode());
    }
}
