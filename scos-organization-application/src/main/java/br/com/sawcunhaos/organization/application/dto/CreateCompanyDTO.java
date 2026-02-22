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

/**
 * DTO para criação de uma nova empresa.
 *
 * Validações obrigatórias:
 * - name: 2-250 caracteres
 * - nameTreatment: máx 100 caracteres
 * - taxIdentifier: CNPJ válido e único no sistema
 * - foundationDate: data válida, não futura
 * - sectorOfActivity: valor válido do enum
 * - parentCompanyId: opcional, se informado deve existir e estar ATIVA
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCompanyDTO {
    private String name;
    private String nameTreatment;
    private String taxIdentifier;
    private LocalDate foundationDate;
    private String sectorOfActivity;
    private String observation;
    private Long parentCompanyId;
}

