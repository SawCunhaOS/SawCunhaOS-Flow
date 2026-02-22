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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta para operações com empresa.
 * Inclui dados de auditoria (createdAt, updatedAt, createdBy, updatedBy).
 *
 * Utilizado em:
 * - GET /v1/companies/{id} - Retorna empresa específica
 * - POST /v1/companies - Resposta de criação
 * - PUT /v1/companies/{id} - Resposta de atualização
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDTO {
    private Long id;
    private String name;
    private String nameTreatment;
    private String taxIdentifier;
    private LocalDate foundationDate;
    private String sectorOfActivity;
    private String observation;
    private String status;
    private boolean active;
    private Long parentCompanyId;
    private String parentCompanyName;

    // Campos de auditoria
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

