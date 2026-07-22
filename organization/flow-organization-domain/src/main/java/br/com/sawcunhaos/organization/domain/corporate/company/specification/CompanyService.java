
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

package br.com.sawcunhaos.organization.domain.corporate.company.specification;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyInput;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CompanyOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.internal.StatusCompany;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZoneId;

/**
 * Casos de uso de domínio do cadastro da {@code Company} (UC-001..005).
 * Concentra as regras que dependem do banco ou de outros agregados: unicidade de CNPJ,
 * integridade das FKs, compatibilidade do motivo de ativação, empresa mãe ativa e profundidade da hierarquia.
 */
public interface CompanyService {

    /** Cria empresa matriz ({@code parentCompanyId} nulo) ou filial e o histórico de status inicial {@code ACTIVE}. */
    CompanyOutput create(@NonNull CompanyInput companyInput);

    /** Atualiza os dados cadastrais de uma empresa existente — {@code parentCompanyId} é imutável. */
    void update(@NonNull CompanyInput companyInput);

    /** Busca uma empresa pelo id, com os dados completos. */
    CompanyOutput findById(@NonNull Long companyId);

    /** Lista paginada de empresas, filtrando por {@code status} e/ou {@code name} quando informados. */
    Page<CompanyOutput> findAll(StatusCompany status, String name, @NonNull Pageable pageable);

    /** Resolve o fuso horário efetivo, subindo a cadeia de {@code parentCompany} até achar o primeiro não nulo (D2). */
    ZoneId resolveEffectiveZoneId(@NonNull Long companyId);

    /**
     * Guarda antecipada (AD-7): garante que atribuir {@code candidateParentCompanyId} como pai de
     * {@code companyId} não fecha um ciclo (direto ou indireto) na hierarquia de empresas. Hoje não
     * há nenhum call site — {@code parentCompanyId} é imutável após a criação — mas o guard fica
     * pronto para a Etapa futura que abrir edição de hierarquia.
     *
     * @throws ScosException SCOS_COMPANY_004 se a atribuição fechar um ciclo.
     */
    void assertNoCycle(@NonNull Long companyId, @NonNull Long candidateParentCompanyId);

}
