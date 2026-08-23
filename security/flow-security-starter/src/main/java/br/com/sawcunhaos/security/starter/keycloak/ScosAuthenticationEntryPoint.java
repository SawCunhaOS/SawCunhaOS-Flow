
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

package br.com.sawcunhaos.security.starter.keycloak;

import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import br.com.sawcunhaos.foundation.exception.model.ScosProblemDetails;
import br.com.sawcunhaos.security.starter.utils.AuthenticationUtils;
import br.com.sawcunhaos.security.starter.utils.SecurityExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@RequiredArgsConstructor
public class ScosAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        ProblemDetail problem = ScosProblemDetails.enrich(ScosProblemDetails.of(
                HttpStatus.UNAUTHORIZED,
                SecurityExceptionCode.SCOS_AUTH_003,
                localeService.getMessage(SecurityExceptionCode.SCOS_AUTH_003.getCode()),
                req.getRequestURI()));
        AuthenticationUtils.writeProblemDetail(res, 401, problem, objectMapper);
    }
}
