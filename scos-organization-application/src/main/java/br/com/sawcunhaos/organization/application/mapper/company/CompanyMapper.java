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

package br.com.sawcunhaos.organization.application.mapper.company;

import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.application.dto.CompanyDTO;
import br.com.sawcunhaos.organization.application.dto.CreateCompanyDTO;
import br.com.sawcunhaos.organization.application.dto.UpdateCompanyDTO;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversões entre Company Entity e DTOs.
 *
 * Responsabilidades:
 * - CreateCompanyDTO -> Company (nova instância)
 * - UpdateCompanyDTO + Company -> Company (atualização)
 * - Company -> CompanyDTO (resposta)
 */
@Component
public class CompanyMapper {

    /**
     * Converte CreateCompanyDTO para Entity Company.
     *
     * @param createDTO DTO de criação
     * @return Entidade Company pronta para persistência
     */
    public Company toCompany(CreateCompanyDTO createDTO) {
        if (createDTO == null) {
            return null;
        }

        return Company.builder()
                .name(createDTO.getName())
                .nameTreatment(createDTO.getNameTreatment())
                .taxIdentifier(new Cnpj(createDTO.getTaxIdentifier()))
                .foundationDate(createDTO.getFoundationDate())
                .sectorOfActivity(createDTO.getSectorOfActivity())
                .observation(createDTO.getObservation())
                .status(StatusCompany.ACTIVE)
                .active(true)
                .build();
    }

    /**
     * Atualiza uma entidade Company com dados de UpdateCompanyDTO.
     *
     * @param updateDTO DTO de atualização
     * @param company Entidade a ser atualizada
     * @return Entidade Company atualizada
     */
    public Company updateCompany(UpdateCompanyDTO updateDTO, Company company) {
        if (updateDTO == null || company == null) {
            return company;
        }

        if (updateDTO.getName() != null) {
            company.setName(updateDTO.getName());
        }
        if (updateDTO.getNameTreatment() != null) {
            company.setNameTreatment(updateDTO.getNameTreatment());
        }
        if (updateDTO.getFoundationDate() != null) {
            company.setFoundationDate(updateDTO.getFoundationDate());
        }
        if (updateDTO.getSectorOfActivity() != null) {
            company.setSectorOfActivity(updateDTO.getSectorOfActivity());
        }
        if (updateDTO.getObservation() != null) {
            company.setObservation(updateDTO.getObservation());
        }
        if (updateDTO.getStatus() != null) {
            company.setStatus(updateDTO.getStatus());
        }

        return company;
    }

    /**
     * Converte Company Entity para CompanyDTO (resposta).
     *
     * @param company Entidade Company
     * @return DTO de resposta com dados de auditoria
     */
    public CompanyDTO toCompanyDTO(Company company) {
        if (company == null) {
            return null;
        }

        String parentCompanyName = null;

        if (company.getParentCompany() != null) {
            parentCompanyName = company.getParentCompany().getName();
        }

        return CompanyDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .nameTreatment(company.getNameTreatment())
                .taxIdentifier(company.getTaxIdentifier() != null ? company.getTaxIdentifier().getCnpj() : null)
                .foundationDate(company.getFoundationDate())
                .sectorOfActivity(company.getSectorOfActivity())
                .observation(company.getObservation())
                .status(company.getStatus().name())
                .active(company.isActive())
                .parentCompanyId(company.getParentCompany() != null ? company.getParentCompany().getId() : null)
                .parentCompanyName(parentCompanyName)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}

