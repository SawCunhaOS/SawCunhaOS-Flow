
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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "scos.registry")
@Validated
@Getter
@Setter
public class ScosRegistryProperties {

    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    @NotBlank
    private String systemName;

    @NotBlank
    private String systemCode;

    @NotBlank
    private String systemDescription;

    @NotBlank
    private String keyAccess;

    private boolean tlsEnabled = false;   // ← plaintext por padrão (dev), true em produção
}
