
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

package br.com.sawcunhaos.security.starter.interceptor;

import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.model.ScosSystemContext;
import br.com.sawcunhaos.security.starter.model.ScosSystemContextHolder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.Base64;
import java.util.UUID;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class ScosSystemAuthInterceptor implements ClientInterceptor {

    private final ScosRegistryProperties scosRegistryProperties;

    public ScosSystemAuthInterceptor(ScosRegistryProperties scosRegistryProperties) {
        this.scosRegistryProperties = scosRegistryProperties;
    }

    @Override
    public <Q, R> ClientCall<Q, R> interceptCall(
            MethodDescriptor<Q, R> method,
            CallOptions options,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, options)) {

            @Override
            public void start(Listener<R> responseListener, Metadata headers) {
                if (ScosSystemContextHolder.isInitialized()) {
                    ScosSystemContext sys = ScosSystemContextHolder.get();

                    headers.put(
                            Metadata.Key.of("Authentication", ASCII_STRING_MARSHALLER),
                            Base64.getEncoder().encodeToString(
                                    String.format("%s:%s",sys.getSystemCode(), sys.getSecretKey()).getBytes()
                            )
                    );
                }
                headers.put(Metadata.Key.of("KEY-ACCESS", ASCII_STRING_MARSHALLER), scosRegistryProperties.getKeyAccess());
                headers.put(Metadata.Key.of("X-Request-ID", ASCII_STRING_MARSHALLER), UUID.randomUUID().toString());
                super.start(responseListener, headers);
            }
        };
    }
}
