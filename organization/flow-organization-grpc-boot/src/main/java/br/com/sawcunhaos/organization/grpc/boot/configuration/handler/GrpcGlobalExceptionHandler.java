
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

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import br.com.sawcunhaos.foundation.core.specification.LocaleService;
import br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

    /** Trailer que carrega o código de erro de negócio (ScosException.code) para o client. */
    public static final Metadata.Key<String> ERROR_CODE_KEY =
            Metadata.Key.of("scos-error-code", Metadata.ASCII_STRING_MARSHALLER);

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
    public StatusRuntimeException handleScosException(ScosException ex) {
        Status status = toStatus(ex.getCode())
                .withDescription(safeMessage(ex))
                .withCause(ex);

        log.warn("gRPC {}: code={} - {}", status.getCode(), ex.getCode(), status.getDescription());

        Metadata trailers = new Metadata();
        if (ex.getCode() != null) {
            trailers.put(ERROR_CODE_KEY, ex.getCode());
        }
        return status.asRuntimeException(trailers);
    }

    /**
     * Mapeia o {@code code} de negócio para um {@link Status} gRPC, resolvendo o HTTP status
     * do {@link ExceptionCodeError}. Códigos desconhecidos caem em {@code INTERNAL}.
     */
    private Status toStatus(String code) {
        int httpCode;
        try {
            httpCode = ExceptionCodeError.valueOf(code).getHttpCode();
        } catch (Exception ex) {
            log.error("Código de erro desconhecido: {}", code, ex);
            return Status.INTERNAL;
        }
        return switch (httpCode) {
            case 400 -> Status.INVALID_ARGUMENT;
            case 401 -> Status.UNAUTHENTICATED;
            case 403 -> Status.PERMISSION_DENIED;
            case 404 -> Status.NOT_FOUND;
            case 409 -> Status.ALREADY_EXISTS;
            case 422 -> Status.FAILED_PRECONDITION;
            default -> Status.INTERNAL;
        };
    }

    private String safeMessage(ScosException ex) {
        try {
            return localeService.getMessage(ex.getCode(), ex.getArgs());
        } catch (Exception e) {
            log.error("Falha ao resolver mensagem para code={}", ex.getCode(), e);
            return "Erro: " + ex.getCode();
        }
    }
}
