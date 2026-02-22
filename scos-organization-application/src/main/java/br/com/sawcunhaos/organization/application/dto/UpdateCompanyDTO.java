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

import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO para atualização de uma empresa existente.
 *
 * Validações:
 * - name: 2-250 caracteres
 * - nameTreatment: máx 100 caracteres
 * - foundationDate: data válida, não futura
 * - sectorOfActivity: valor válido do enum
 * - status: apenas atualizado via endpoint de status específico (PUT /companies/{id}/status)
 * - taxIdentifier: IMUTÁVEL, não pode ser alterado neste DTO
 * - parentCompanyId: opcional, se alterado valida ciclos e existência
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompanyDTO {
    private String name;
    private String nameTreatment;
    private LocalDate foundationDate;
    private String sectorOfActivity;
    private String observation;
    private Long parentCompanyId;
    private StatusCompany status;
}

