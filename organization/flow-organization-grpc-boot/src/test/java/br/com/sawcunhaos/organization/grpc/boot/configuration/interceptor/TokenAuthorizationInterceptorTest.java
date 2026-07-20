
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

package br.com.sawcunhaos.organization.grpc.boot.configuration.interceptor;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.access.system.specification.ScosSystemService;
import br.com.sawcunhaos.organization.grpc.boot.configuration.properties.ScosGRPCProperties;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_SYSTEM_001;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenAuthorizationInterceptorTest {

    private static final Metadata.Key<String> KEY_ACCESS =
            Metadata.Key.of("KEY-ACCESS", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authentication", Metadata.ASCII_STRING_MARSHALLER);

    private static final String EXPECTED_KEY_ACCESS = "expected-key";
    private static final String PROTECTED_METHOD =
            "br.com.sawcunhaos.organization.grpc.proto.RegistryService/registryResources";
    private static final String UNPROTECTED_METHOD =
            "br.com.sawcunhaos.organization.grpc.proto.SomeOtherService/someMethod";

    private ScosSystemService scosSystemService;
    private ScosGRPCProperties scosGRPCProperties;
    private TokenAuthorizationInterceptor interceptor;

    private ServerCall<Object, Object> call;
    private ServerCallHandler<Object, Object> next;
    private ServerCall.Listener<Object> nextListener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        scosSystemService = mock(ScosSystemService.class);
        scosGRPCProperties = new ScosGRPCProperties();
        scosGRPCProperties.setKeyAccess(EXPECTED_KEY_ACCESS);
        interceptor = new TokenAuthorizationInterceptor(scosSystemService, scosGRPCProperties);

        call = mock(ServerCall.class);
        next = mock(ServerCallHandler.class);
        nextListener = mock(ServerCall.Listener.class);
        when(next.startCall(any(), any())).thenReturn(nextListener);
    }

    private void stubMethodName(String methodName) {
        MethodDescriptor<Object, Object> methodDescriptor = mock(MethodDescriptor.class);
        when(methodDescriptor.getFullMethodName()).thenReturn(methodName);
        when(call.getMethodDescriptor()).thenReturn(methodDescriptor);
    }

    private static String encodeToken(String code, String secret) {
        return Base64.getEncoder().encodeToString((code + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void keyAccessAusenteRetornaUnauthenticated() {
        Metadata headers = new Metadata();

        ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
        assertNotNull(listener);
    }

    @Test
    void keyAccessIncorretoRetornaUnauthenticated() {
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, "wrong-key");

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
    }

    @Test
    void metodoNaoProtegidoComKeyAccessCorretoSeguemDiretoParaNext() {
        stubMethodName(UNPROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);

        ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);

        verify(next).startCall(call, headers);
        verify(call, never()).close(any(), any());
        assertThat(listener).isSameAs(nextListener);
    }

    @Test
    void metodoProtegidoSemTokenRetornaUnauthenticated() {
        stubMethodName(PROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
    }

    @Test
    void tokenComBase64InvalidoRetornaUnauthenticated() {
        stubMethodName(PROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);
        headers.put(AUTHORIZATION_KEY, "@@@nao-e-base64@@@");

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
    }

    @Test
    void tokenDecodificadoSemSeparadorRetornaUnauthenticated() {
        stubMethodName(PROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);
        headers.put(AUTHORIZATION_KEY,
                Base64.getEncoder().encodeToString("semseparador".getBytes(StandardCharsets.UTF_8)));

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
    }

    @Test
    void codeDeSistemaInexistenteRetornaUnauthenticated() {
        stubMethodName(PROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);
        headers.put(AUTHORIZATION_KEY, encodeToken("UNKNOWN_SYSTEM", "any-secret"));
        doThrow(new ScosException(SCOS_SYSTEM_001))
                .when(scosSystemService).validateSecretKey("UNKNOWN_SYSTEM", "any-secret");

        interceptor.interceptCall(call, headers, next);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(call).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(next, never()).startCall(any(), any());
    }

    @Test
    void caminhoFelizInvocaNextStartCallENaoFechaChamada() {
        stubMethodName(PROTECTED_METHOD);
        Metadata headers = new Metadata();
        headers.put(KEY_ACCESS, EXPECTED_KEY_ACCESS);
        headers.put(AUTHORIZATION_KEY, encodeToken("SYSTEM_CODE", "correct-secret"));
        doNothing().when(scosSystemService).validateSecretKey("SYSTEM_CODE", "correct-secret");

        ServerCall.Listener<Object> listener = interceptor.interceptCall(call, headers, next);

        verify(next).startCall(call, headers);
        verify(call, never()).close(any(), any());
        assertNotNull(listener);
        assertThat(listener).isNotSameAs(nextListener);

        listener.onMessage(new Object());
        listener.onHalfClose();
        verify(nextListener).onMessage(any());
        verify(nextListener).onHalfClose();
    }
}
