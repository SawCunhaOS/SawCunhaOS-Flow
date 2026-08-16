
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

package br.com.sawcunhaos.organization.domain.access.login.internal.rules;

import br.com.sawcunhaos.organization.domain.access.login.internal.LoginStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_014;
import static org.assertj.core.api.Assertions.assertThat;

class LoginPendingApprovalRuleTest {

    private final LoginPendingApprovalRule rule = new LoginPendingApprovalRule();

    @Test
    @DisplayName("Given status PENDING_APPROVAL When validate Then retorna o código SCOS_LOGIN_014")
    void givenPendingApprovalStatus_whenValidate_thenReturnsErrorCode() {
        Optional<String> result = rule.validate(LoginStatus.PENDING_APPROVAL);

        assertThat(result).contains(SCOS_LOGIN_014.getCode());
    }

    @Test
    @DisplayName("Given status ACTIVE When validate Then retorna vazio")
    void givenActiveStatus_whenValidate_thenReturnsEmpty() {
        Optional<String> result = rule.validate(LoginStatus.ACTIVE);

        assertThat(result).isEmpty();
    }
}
