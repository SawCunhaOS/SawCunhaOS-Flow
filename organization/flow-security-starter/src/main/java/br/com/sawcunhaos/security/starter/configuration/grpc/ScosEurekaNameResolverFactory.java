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

import com.netflix.discovery.EurekaClient;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NameResolver backed by Eureka for gRPC client-side load balancing.
 *
 * <p>On {@code start()}, resolves immediately and schedules periodic re-resolution
 * every {@code refreshIntervalSeconds}. Each re-resolution fetches the current
 * healthy instance list from Eureka and notifies the gRPC channel, which
 * redistributes load using the configured policy (round_robin).
 *
 * <p>gRPC also calls {@link #refresh()} automatically whenever a connected
 * subchannel fails, so the resolver reacts to instance failure even between
 * scheduled intervals.
 */
@Slf4j
public class ScosEurekaNameResolverFactory extends NameResolver.Factory {

    private final EurekaClient eurekaClient;
    private final String serviceName;
    private final long refreshIntervalSeconds;

    public ScosEurekaNameResolverFactory(
            EurekaClient eurekaClient,
            String serviceName,
            long refreshIntervalSeconds) {
        this.eurekaClient = eurekaClient;
        this.serviceName = serviceName;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        return new EurekaNameResolver();
    }

    @Override
    public String getDefaultScheme() {
        return "eureka";
    }

    // -------------------------------------------------------------------------

    private final class EurekaNameResolver extends NameResolver {

        private Listener2 listener;
        private ScheduledExecutorService scheduler;
        private ScheduledFuture<?> scheduledTask;
        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        @Override
        public String getServiceAuthority() {
            return serviceName;
        }

        /**
         * Called once when the channel is created.
         * Resolves immediately, then schedules periodic re-resolution.
         */
        @Override
        public void start(Listener2 listener) {
            this.listener = listener;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "eureka-name-resolver-" + serviceName);
                thread.setDaemon(true);
                return thread;
            });

            // resolve imediatamente ao iniciar
            resolve();

            // agenda re-resolução periódica
            this.scheduledTask = scheduler.scheduleAtFixedRate(
                    this::resolve,
                    refreshIntervalSeconds,
                    refreshIntervalSeconds,
                    TimeUnit.SECONDS
            );

            log.info("[EurekaNameResolver] Started for service '{}' — refresh every {}s",
                    serviceName, refreshIntervalSeconds);
        }

        /**
         * Called by gRPC automatically when a subchannel fails.
         * Forces an immediate re-resolution without esperar o próximo ciclo.
         */
        @Override
        public void refresh() {
            if (!shutdown.get()) {
                log.info("[EurekaNameResolver] Manual refresh triggered for '{}'", serviceName);
                resolve();
            }
        }

        @Override
        public void shutdown() {
            if (shutdown.compareAndSet(false, true)) {
                if (scheduledTask != null) {
                    scheduledTask.cancel(false);
                }
                if (scheduler != null) {
                    scheduler.shutdown();
                }
                log.info("[EurekaNameResolver] Shutdown for service '{}'", serviceName);
            }
        }

        // ---------------------------------------------------------------------

        private void resolve() {
            try {
                var instances = eurekaClient.getInstancesByVipAddress(serviceName, false);

                if (instances == null || instances.isEmpty()) {
                    log.warn("[EurekaNameResolver] No instances found in Eureka for '{}'", serviceName);
                    listener.onError(
                            io.grpc.Status.UNAVAILABLE
                                    .withDescription("No instances registered in Eureka for: " + serviceName)
                    );
                    return;
                }

                List<EquivalentAddressGroup> addresses = instances.stream()
                        .filter(info -> "UP".equals(info.getStatus().name()))
                        .map(info -> {
                            log.debug("[EurekaNameResolver] Found instance: {}:{}", info.getIPAddr(), info.getPort());
                            return new EquivalentAddressGroup(
                                    new InetSocketAddress(info.getIPAddr(), info.getPort())
                            );
                        })
                        .toList();

                if (addresses.isEmpty()) {
                    log.warn("[EurekaNameResolver] All instances for '{}' are DOWN", serviceName);
                    listener.onError(
                            io.grpc.Status.UNAVAILABLE
                                    .withDescription("All instances of " + serviceName + " are DOWN in Eureka")
                    );
                    return;
                }

                log.info("[EurekaNameResolver] Resolved {} UP instance(s) for '{}'",
                        addresses.size(), serviceName);

                listener.onResult(
                        ResolutionResult.newBuilder()
                                .setAddresses(addresses)
                                .setAttributes(Attributes.EMPTY)
                                .build()
                );

            } catch (Exception e) {
                log.error("[EurekaNameResolver] Failed to resolve instances for '{}': {}",
                        serviceName, e.getMessage(), e);
                listener.onError(
                        io.grpc.Status.UNAVAILABLE
                                .withDescription("Eureka resolution failed for: " + serviceName)
                                .withCause(e)
                );
            }
        }
    }
}
