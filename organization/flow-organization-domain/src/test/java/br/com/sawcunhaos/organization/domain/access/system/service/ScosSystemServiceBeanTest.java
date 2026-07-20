
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_002;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes de {@link ScosSystemServiceBean#validateSecretKey}. Fora de escopo: grace period /
 * {@code previousSecretKey} — pertence à Story 0.2.
 */
@ExtendWith(MockitoExtension.class)
class ScosSystemServiceBeanTest {

    private static final String SYSTEM_CODE = "SYSTEM_CODE";

    @Mock
    private ScosSystemRepository scosSystemRepository;
    @Mock
    private SystemSecretCryptoService systemSecretCryptoService;

    @InjectMocks
    private ScosSystemServiceBean scosSystemServiceBean;

    private ScosSystem systemWithSecret(String secretKey) {
        return ScosSystem.builder()
                .code(SYSTEM_CODE)
                .secretKey(secretKey)
                .status("ACTIVE")
                .build();
    }

    @Test
    void validateSecretKeyShouldNotThrowWhenSecretMatches() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecret("correct-secret")));

        assertThatCode(() -> scosSystemServiceBean.validateSecretKey(SYSTEM_CODE, "correct-secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSecretKeyShouldThrowScosSystem002WhenSecretDoesNotMatch() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE))
                .thenReturn(Optional.of(systemWithSecret("correct-secret")));

        assertThatThrownBy(() -> scosSystemServiceBean.validateSecretKey(SYSTEM_CODE, "wrong-secret"))
                .hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_002.getCode());
    }

    @Test
    void validateSecretKeyShouldThrowScosSystem001WhenCodeNotFound() {
        when(scosSystemRepository.findByCode(SYSTEM_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scosSystemServiceBean.validateSecretKey(SYSTEM_CODE, "any-secret"))
                .hasFieldOrPropertyWithValue("code", SCOS_SYSTEM_001.getCode());
    }
}
