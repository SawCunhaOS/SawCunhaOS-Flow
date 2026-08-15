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

package br.com.sawcunhaos.security.starter.configuration.grpc;

import br.com.sawcunhaos.security.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.security.grpc.proto.ValidateAuthorityServiceGrpc;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.interceptor.ScosSystemAuthInterceptor;
import com.netflix.discovery.EurekaClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@Slf4j
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
    @ConditionalOnProperty(name = "scos.registry.discovery-enabled", havingValue = "true")
    public ManagedChannel scosRegistryChannelWithDiscovery(
            ScosRegistryProperties properties,
            EurekaClient eurekaClient) {

        log.info("[ScosGrpcClient] Discovery mode — service: '{}', refresh: {}s",
                properties.getServiceName(),
                properties.getDiscoveryRefreshIntervalSeconds());

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forTarget("eureka:///" + properties.getServiceName())
                .nameResolverFactory(new ScosEurekaNameResolverFactory(
                        eurekaClient,
                        properties.getServiceName(),
                        properties.getDiscoveryRefreshIntervalSeconds()
                ))
                .defaultLoadBalancingPolicy("round_robin");

        this.scosRegistryChannel = applyTls(builder, properties).build();
        return this.scosRegistryChannel;
    }

    @Bean
    @ConditionalOnProperty(
            name = "scos.registry.discovery-enabled",
            havingValue = "false",
            matchIfMissing = true)
    public ManagedChannel scosRegistryChannel(ScosRegistryProperties properties) {

        log.info("[ScosGrpcClient] Direct mode — host: '{}', port: {}",
                properties.getHost(),
                properties.getPort());

        ManagedChannelBuilder<?> builder = ManagedChannelBuilder
                .forAddress(properties.getHost(), properties.getPort());

        this.scosRegistryChannel = applyTls(builder, properties).build();
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
    public ValidateAuthorityServiceGrpc.ValidateAuthorityServiceBlockingV2Stub validateAuthorityServiceBlockingV2Stub(
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

    private ManagedChannelBuilder<?> applyTls(
            ManagedChannelBuilder<?> builder,
            ScosRegistryProperties properties) {
        return properties.isTlsEnabled()
                ? builder.useTransportSecurity()
                : builder.usePlaintext();
    }
}
