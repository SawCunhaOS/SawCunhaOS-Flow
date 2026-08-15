
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
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

@Slf4j
@Component
@GlobalServerInterceptor
@Order(10) // depois do logging (Order 0), antes da lógica de negócio
@RequiredArgsConstructor
@EnableConfigurationProperties(ScosGRPCProperties.class)
public class TokenAuthorizationInterceptor implements ServerInterceptor {

    private final ScosSystemService scosSystemService;
    private final ScosGRPCProperties scosGRPCProperties;

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authentication", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> KEY_ACCESS =
            Metadata.Key.of("KEY-ACCESS", Metadata.ASCII_STRING_MARSHALLER);

    // Apenas estes métodos exigem o token — ajuste para os seus
    private static final Set<String> PROTECTED_METHODS = Set.of(
            "br.com.sawcunhaos.security.grpc.proto.RegistryService/registryResources",
            "br.com.sawcunhaos.security.grpc.proto.ValidateAuthorityService/validateAuthority"
    );

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String provided = headers.get(KEY_ACCESS);
        if (provided == null || !MessageDigest.isEqual(
                scosGRPCProperties.getKeyAccess().getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))
        ) {
            call.close(Status.UNAUTHENTICATED.withDescription("Bootstrap negado"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String methodName = call.getMethodDescriptor().getFullMethodName();

        if (!PROTECTED_METHODS.contains(methodName)) {
            return next.startCall(call, headers);
        }

        String rawToken = headers.get(AUTHORIZATION_KEY);
        if (rawToken == null || rawToken.isBlank()) {
            log.warn("Token ausente para método protegido: {}", methodName);
            call.close(Status.UNAUTHENTICATED.withDescription("Token ausente"), new Metadata());
            return noopListener();
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(rawToken), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            log.warn("Token mal formatado (base64 inválido) em: {}", methodName);
            call.close(Status.UNAUTHENTICATED.withDescription("Token mal formatado"), new Metadata());
            return noopListener();
        }

        String[] parts = decoded.split(":");
        if (parts.length != 2) {
            log.warn("Token decodificado em formato inesperado em: {}", methodName);
            call.close(Status.UNAUTHENTICATED.withDescription("Token mal formatado"), new Metadata());
            return noopListener();
        }

        try {
            scosSystemService.validateSecretKey(parts[0], parts[1]);
        } catch (ScosException ex) {
            log.error("Sistema informado nao existe: {}", parts[0]);
            call.close(Status.UNAUTHENTICATED.withDescription("Sistema informado nao existe"), new Metadata());
            return noopListener();
        }

        var authentication = new UsernamePasswordAuthenticationToken(parts[0], null, null);
        final SecurityContext context = new SecurityContextImpl(authentication);

        // Monta o listener real — ainda SEM colocar o context no Holder.
        // O context só é aplicado dentro dos callbacks, no momento certo.
        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);

        // ── Envolve o listener para que o SecurityContext esteja ativo
        //    exatamente quando a lógica de negócio é executada ───────────
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {

            @Override
            public void onMessage(ReqT message) {
                SecurityContextHolder.setContext(context);
                super.onMessage(message);
            }

            @Override
            public void onHalfClose() {
                // Para chamadas unárias, é aqui que o método @Service é efetivamente invocado.
                SecurityContextHolder.setContext(context);
                super.onHalfClose();
            }
        };
    }

    /** Listener vazio — usado quando a chamada é encerrada antes de chegar ao serviço. */
    private <ReqT> ServerCall.Listener<ReqT> noopListener() {
        return new ServerCall.Listener<>() {};
    }
}
