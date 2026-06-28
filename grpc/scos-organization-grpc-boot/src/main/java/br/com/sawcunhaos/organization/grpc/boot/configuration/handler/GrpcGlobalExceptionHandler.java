
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

package br.com.sawcunhaos.organization.grpc.boot.configuration.handler;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;

/**
 * gRPC equivalent of {@code @ExceptionHandler}.
 * Converts unhandled exceptions into proper gRPC Status codes so the client
 * always receives a valid trailer frame instead of an unexpected EOS.
 */
@Slf4j
@GrpcAdvice
@RequiredArgsConstructor
public class GrpcGlobalExceptionHandler {

    private final LocaleService localeService;

    @GrpcExceptionHandler(AccessDeniedException.class)
    public Status handleAccessDenied(AccessDeniedException ex) {
        log.warn("gRPC ACCESS_DENIED: {}", ex.getMessage());
        return Status.PERMISSION_DENIED.withDescription("Acesso negado");
    }

    @GrpcExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public Status handleNoCredentials(AuthenticationCredentialsNotFoundException ex) {
        log.warn("gRPC UNAUTHENTICATED - sem credenciais: {}", ex.getMessage());
        return Status.UNAUTHENTICATED.withDescription("Não autenticado");
    }

    @GrpcExceptionHandler(AuthenticationException.class)
    public Status handleAuthentication(AuthenticationException ex) {
        log.warn("gRPC UNAUTHENTICATED: {}", ex.getMessage());
        return Status.UNAUTHENTICATED.withDescription("Falha na autenticação");
    }

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public Status handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("gRPC INVALID_ARGUMENT: {}", ex.getMessage());
        return Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
    }

    @GrpcExceptionHandler(IllegalStateException.class)
    public Status handleIllegalState(IllegalStateException ex) {
        log.warn("gRPC FAILED_PRECONDITION: {}", ex.getMessage());
        return Status.FAILED_PRECONDITION.withDescription(ex.getMessage());
    }

    @GrpcExceptionHandler(SecurityException.class)
    public Status handleSecurity(SecurityException ex) {
        log.warn("gRPC PERMISSION_DENIED: {}", ex.getMessage());
        return Status.PERMISSION_DENIED.withDescription(ex.getMessage());
    }

    // Fallback — captura qualquer Exception não mapeada
    @GrpcExceptionHandler(Exception.class)
    public Status handleGeneric(Exception ex) {
        log.error("gRPC INTERNAL unhandled exception", ex);
        return Status.INTERNAL.withDescription("Internal server error");
    }

    @GrpcExceptionHandler(ScosException.class)
    public Status handleScosException(ScosException ex) {
        log.error("gRPC INTERNAL unhandled exception", ex);
        return Status.INTERNAL.withDescription(safeMessage(ex.getCode())).withCause(ex);
    }

    private String safeMessage(String code) {
        try {
            return localeService.getMessage(code);
        } catch (Exception ex) {
            log.error("Falha ao resolver mensagem para code={}", code, ex);
            return "Erro: " + code;
        }
    }
}
