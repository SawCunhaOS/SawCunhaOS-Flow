
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

import br.com.sawcunhaos.foundation.validation.valueobjects.Cnpj;
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
 * Teste de integração da CTE recursiva de detecção de ciclo (AD-7) em {@link CompanyRepository}.
 *
 * <p>Sem MockMvc — não existe endpoint que edite {@code parentCompanyId} (guard antecipado, Story
 * 1.1). Autowira {@link CompanyRepository} direto e monta a hierarquia via {@code merge(...)} para
 * provar que a query nativa (única forma de detectar ciclo indireto contra Postgres real — H2 do
 * módulo {@code domain} não roda {@code WITH RECURSIVE} contra o schema {@code scos.}) funciona.
 * Sufixo {@code *ControllerTest} mantido por convenção do projeto mesmo sem chamada HTTP.
 */
public class CompanyCycleGuardControllerTest extends ScosOrganizationTestUtil {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("wouldCreateCycle — hierarquia de 3+ níveis detecta ciclo indireto ao fechar o laço")
    void wouldCreateCycleShouldDetectIndirectCycleAcrossThreeLevels() {
        Company companyC = companyRepository.merge(newCompany("11111222000106", null));
        Company companyB = companyRepository.merge(newCompany("22222333000106", companyC));
        Company companyA = companyRepository.merge(newCompany("33333444000106", companyB));

        assertTrue(companyRepository.wouldCreateCycle(companyC.getId(), companyA.getId()));
    }

    @Test
    @DisplayName("wouldCreateCycle — empresas sem relação de hierarquia não detecta ciclo")
    void wouldCreateCycleShouldReturnFalseWhenNoHierarchyRelation() {
        Company standalone = companyRepository.merge(newCompany("44444555000106", null));
        Company candidateFilial = companyRepository.merge(newCompany("55555666000106", null));

        assertFalse(companyRepository.wouldCreateCycle(candidateFilial.getId(), standalone.getId()));
    }

    private Company newCompany(String taxIdentifier, Company parentCompany) {
        Company company = Company.builder()
                .name("Empresa Teste LTDA")
                .nameTreatment("Teste")
                .taxIdentifier(new Cnpj(taxIdentifier))
                .foundationDate(LocalDate.of(2020, 1, 1))
                .sectorOfActivity("Tecnologia da Informação")
                .status(StatusCompany.ACTIVE)
                .parentCompany(parentCompany)
                .build();
        company.updateAuditInfo("story-1.1-test");
        return company;
    }
}
