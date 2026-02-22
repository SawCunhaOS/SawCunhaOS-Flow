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

package br.com.sawcunhaos.organization.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO para representar endereço de uma empresa.
 *
 * Tipos de endereço:
 * - COMMERCIAL: Endereço comercial/matriz
 * - HEADQUARTERS: Sede
 * - BRANCH: Filial
 * - BILLING: Endereço de faturamento
 * - OTHER: Outro tipo
 *
 * Coordenadas geográficas opcionais para localização em mapas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAddressDTO {
    private Long addressId;
    private Long companyId;
    private String type;
    private long number;
    private String complement;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

