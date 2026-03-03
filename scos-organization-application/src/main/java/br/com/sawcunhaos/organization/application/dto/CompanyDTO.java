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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CompanyDTO {

    private Long id;
    private String name;
    private String nameTreatment;
    private String taxIdentifier;
    private LocalDate foundationDate;
    private LocalDate dateCreated;
    private String sectorOfActivity;
    private String observation;
    private boolean active;
    private String status;
    private Long parentCompanyId;
    private String parentCompanyName;
    private String parentCompanyNameTreatment;
    private String parentCompanyTaxIdentifier;

}
