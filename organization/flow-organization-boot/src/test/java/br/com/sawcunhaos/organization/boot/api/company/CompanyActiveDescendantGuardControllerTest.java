
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

package br.com.sawcunhaos.organization.boot.api.company;

import br.com.sawcunhaos.foundation.utils.valueobjects.Cnpj;
import br.com.sawcunhaos.organization.boot.infrastructure.ScosOrganizationTestUtil;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.Company;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.CompanyRepository;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração da CTE recursiva descendente (AD-7) em {@link CompanyRepository}.
 *
 * <p>Sem MockMvc — a guarda vive inteiramente em {@code CompanyServiceBean} (Story 1.3), sem novo
 * endpoint. Autowira {@link CompanyRepository} direto e monta a hierarquia via {@code merge(...)}
 * para provar que a query nativa (Postgres real — H2 do módulo {@code domain} não roda
 * {@code WITH RECURSIVE} contra o schema {@code scos.}) encontra descendente ativo em qualquer
 * nível e aceita quando não existe nenhum. Sufixo {@code *ControllerTest} mantido por convenção
 * do projeto mesmo sem chamada HTTP (mesma decisão da Story 1.1).
 */
public class CompanyActiveDescendantGuardControllerTest extends ScosOrganizationTestUtil {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("hasActiveDescendant — descendente indireto ACTIVE (nível 2) é encontrado")
    void hasActiveDescendantShouldReturnTrueWhenIndirectDescendantIsActive() {
        Company matrix = companyRepository.merge(newCompany("11111222000106", null, StatusCompany.ACTIVE));
        Company filialLevel1 = companyRepository.merge(newCompany("22222333000106", matrix, StatusCompany.INACTIVE));
        companyRepository.merge(newCompany("33333444000106", filialLevel1, StatusCompany.ACTIVE));

        assertTrue(companyRepository.hasActiveDescendant(matrix.getId()));
    }

    @Test
    @DisplayName("hasActiveDescendant — matriz sem nenhum descendente ativo retorna false")
    void hasActiveDescendantShouldReturnFalseWhenNoDescendantIsActive() {
        Company matrix = companyRepository.merge(newCompany("44444555000106", null, StatusCompany.ACTIVE));
        Company filialLevel1 = companyRepository.merge(newCompany("55555666000106", matrix, StatusCompany.INACTIVE));
        companyRepository.merge(newCompany("66666777000106", filialLevel1, StatusCompany.DISABLED));

        assertFalse(companyRepository.hasActiveDescendant(matrix.getId()));
    }

    private Company newCompany(String taxIdentifier, Company parentCompany, StatusCompany status) {
        Company company = Company.builder()
                .name("Empresa Teste LTDA")
                .nameTreatment("Teste")
                .taxIdentifier(new Cnpj(taxIdentifier))
                .foundationDate(LocalDate.of(2020, 1, 1))
                .sectorOfActivity("Tecnologia da Informação")
                .status(status)
                .parentCompany(parentCompany)
                .build();
        company.updateAuditInfo("story-1.3-test");
        return company;
    }
}
