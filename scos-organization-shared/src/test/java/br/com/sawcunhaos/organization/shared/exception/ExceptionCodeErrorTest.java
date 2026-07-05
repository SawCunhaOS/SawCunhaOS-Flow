
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

package br.com.sawcunhaos.organization.shared.exception;

import org.junit.jupiter.api.Test;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_AUTHORITY_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_002;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_DEPARTMENT_004;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_LOGIN_010;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_POSITION_003;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_USER_001;
import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_USER_004;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica {@code httpCode}/{@code title} de {@link ExceptionCodeError}, um representante por categoria
 * (404/409/422/502/500/401 e o default 400 dos códigos órfãos).
 */
class ExceptionCodeErrorTest {

    @Test
    void notFoundCodeReturns404() {
        assertEquals(404, SCOS_DEPARTMENT_001.getHttpCode());
        assertEquals("SCOS_TITLE_NOT_FOUND", SCOS_DEPARTMENT_001.getTitle());
        assertEquals(404, SCOS_AUTHORITY_001.getHttpCode());
    }

    @Test
    void duplicateCodeReturns409() {
        assertEquals(409, SCOS_DEPARTMENT_002.getHttpCode());
        assertEquals("SCOS_TITLE_CONFLICT", SCOS_DEPARTMENT_002.getTitle());
    }

    @Test
    void businessRuleViolationReturns422() {
        assertEquals(422, SCOS_DEPARTMENT_004.getHttpCode());
        assertEquals("SCOS_TITLE_BUSINESS_RULE_VIOLATION", SCOS_DEPARTMENT_004.getTitle());
        assertEquals(422, SCOS_POSITION_003.getHttpCode());
    }

    @Test
    void externalIntegrationFailureReturns502() {
        assertEquals(502, SCOS_USER_001.getHttpCode());
        assertEquals("SCOS_TITLE_EXTERNAL_INTEGRATION_FAILURE", SCOS_USER_001.getTitle());
    }

    @Test
    void internalErrorReturns500() {
        assertEquals(500, SCOS_USER_004.getHttpCode());
        assertEquals("SCOS_TITLE_INTERNAL_ERROR", SCOS_USER_004.getTitle());
    }

    @Test
    void unauthorizedReturns401() {
        assertEquals(401, SCOS_LOGIN_010.getHttpCode());
        assertEquals("SCOS_TITLE_UNAUTHORIZED", SCOS_LOGIN_010.getTitle());
    }

    @Test
    void orphanCodeKeepsGenericDefault() {
        assertEquals(400, SCOS_LOGIN_001.getHttpCode());
        assertEquals("SCOS_TITLE_GENERIC", SCOS_LOGIN_001.getTitle());
    }
}
