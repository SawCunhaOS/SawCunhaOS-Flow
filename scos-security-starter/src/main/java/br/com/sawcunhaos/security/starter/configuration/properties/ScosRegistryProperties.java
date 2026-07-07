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

package br.com.sawcunhaos.security.starter.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do cliente gRPC que aponta para o scos-organization-grpc-boot.
 *
 * <p><b>Modo discovery (recomendado — load balancing automático):</b>
 * <pre>
 * scos.registry:
 *   discovery-enabled: true
 *   service-name: scos-organization-grpc-boot   # nome registrado no Eureka
 *   discovery-refresh-interval-seconds: 30       # re-resolução periódica (padrão: 30)
 *   system-name: meu-sistema
 *   system-code: MEU_SISTEMA
 *   system-description: Meu sistema
 *   key-access: minha-chave
 * </pre>
 *
 * <p><b>Modo direto (endereço fixo — sem Eureka, para desenvolvimento local):</b>
 * <pre>
 * scos.registry:
 *   discovery-enabled: false
 *   host: localhost
 *   port: 9090
 *   system-name: meu-sistema
 *   system-code: MEU_SISTEMA
 *   system-description: Meu sistema
 *   key-access: minha-chave
 * </pre>
 */
@ConfigurationProperties(prefix = "scos.registry")
@Validated
@Getter
@Setter
public class ScosRegistryProperties {

    /**
     * Habilita service discovery via Eureka.
     * Quando true, usa serviceName + Eureka para resolver instâncias com load balancing.
     * Quando false, usa host + port fixos (modo direto).
     */
    private boolean discoveryEnabled = false;

    /**
     * Nome do serviço registrado no Eureka.
     * Obrigatório quando discoveryEnabled = true.
     * Exemplo: scos-organization-grpc-boot
     */
    private String serviceName;

    /**
     * Intervalo em segundos para re-resolução periódica das instâncias no Eureka.
     * Padrão: 30 segundos.
     * Só relevante quando discoveryEnabled = true.
     */
    private long discoveryRefreshIntervalSeconds = 30;

    /**
     * Host do servidor gRPC.
     * Obrigatório quando discoveryEnabled = false.
     */
    private String host;

    /**
     * Porta do servidor gRPC.
     * Obrigatório quando discoveryEnabled = false.
     */
    private Integer port;

    @NotBlank
    private String systemName;

    @NotBlank
    private String systemCode;

    @NotBlank
    private String systemDescription;

    @NotBlank
    private String keyAccess;

    private boolean tlsEnabled = false;
}
