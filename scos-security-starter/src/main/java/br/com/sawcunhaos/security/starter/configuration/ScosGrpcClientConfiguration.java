
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

package br.com.sawcunhaos.security.starter.configuration;

import br.com.sawcunhaos.organization.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.organization.grpc.proto.ValidateAuthorityServiceGrpc;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.interceptor.ScosSystemAuthInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties(ScosRegistryProperties.class)
public class ScosGrpcClientConfiguration implements DisposableBean {

    private ManagedChannel scosRegistryChannel;

    @Bean
    public ScosSystemAuthInterceptor scosSystemAuthInterceptor(
            ScosRegistryProperties properties
    ) {
        return new ScosSystemAuthInterceptor(properties);
    }

    @Bean
    public ManagedChannel scosRegistryChannel(ScosRegistryProperties properties) {
        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort());

        if (properties.isTlsEnabled()) {
            builder.useTransportSecurity();
        } else {
            builder.usePlaintext();
        }

        this.scosRegistryChannel = builder.build();
        return this.scosRegistryChannel;
    }

    @Bean
    public RegistryServiceGrpc.RegistryServiceBlockingV2Stub registryServiceBlockingV2Stub(
            ManagedChannel scosRegistryChannel,
            ScosSystemAuthInterceptor scosSystemAuthInterceptor) {

        return RegistryServiceGrpc
                .newBlockingV2Stub(scosRegistryChannel)
                .withInterceptors(scosSystemAuthInterceptor);
    }

    @Bean
    public ValidateAuthorityServiceGrpc.ValidateAuthorityServiceBlockingV2Stub
    validateAuthorityServiceBlockingV2Stub(
            ManagedChannel scosRegistryChannel,
            ScosSystemAuthInterceptor scosSystemAuthInterceptor) {

        return ValidateAuthorityServiceGrpc
                .newBlockingV2Stub(scosRegistryChannel)
                .withInterceptors(scosSystemAuthInterceptor);
    }

    @Override
    public void destroy() throws InterruptedException {
        if (scosRegistryChannel != null && !scosRegistryChannel.isShutdown()) {
            scosRegistryChannel
                    .shutdown()
                    .awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
