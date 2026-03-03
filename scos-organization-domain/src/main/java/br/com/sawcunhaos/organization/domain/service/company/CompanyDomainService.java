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

package br.com.sawcunhaos.organization.domain.service.company;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.model.company.Company;
import br.com.sawcunhaos.organization.domain.model.company.StatusCompany;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyAddressRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyContactRepository;
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyDomainService {

    private static final int MAX_HIERARCHY_DEPTH = 5;

    private final CompanyRepository companyRepository;
    private final CompanyContactRepository companyContactRepository;
    private final CompanyAddressRepository companyAddressRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Company validations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida se a empresa existe (não deletada).
     * Erro: SCOS_COMPANY_001 (404)
     */
    public void validateCompanyExistsValidation(@NonNull Long companyId) {
        log.info("Validating company exists: {}", companyId);
        if (companyRepository.findNotDeletedById(companyId).isEmpty()) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }
    }

    /**
     * Valida que o CNPJ (já normalizado) não está em uso.
     * Erro: SCOS_COMPANY_002 (409)
     */
    public void validateTaxIdentifierUniqueValidation(@NonNull String cnpjNormalized) {
        log.info("Validating CNPJ uniqueness: {}", cnpjNormalized);
        if (companyRepository.existsByTaxIdentifier(cnpjNormalized)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_002);
        }
    }

    /**
     * Valida que a empresa-mãe existe e está ativa (para criação/atualização).
     * Erro: SCOS_COMPANY_001 (404) se não existir
     * Erro: SCOS_COMPANY_007 (422) se não estiver ativa
     */
    public void validateParentCompanyExistsAndActiveValidation(@NonNull Long parentCompanyId) {
        log.info("Validating parent company exists and is active: {}", parentCompanyId);
        Company parent = companyRepository.findNotDeletedById(parentCompanyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));
        if (!parent.isActive()) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_007);
        }
    }

    /**
     * Detecta ciclos na hierarquia de empresas (transitivos, até MAX_HIERARCHY_DEPTH=5 níveis).
     * A trigger de BD cobre APENAS auto-referência direta;
     * ciclos transitivos (A→B→C→A) são responsabilidade desta validação.
     * Erro: SCOS_COMPANY_004 (400)
     */
    public void validateCycleInHierarchyValidation(@NonNull Long companyId, @NonNull Long newParentId) {
        log.info("Validating no cycle in hierarchy: companyId={}, newParentId={}", companyId, newParentId);
        Long current = newParentId;
        int depth = 0;
        while (current != null && depth < MAX_HIERARCHY_DEPTH) {
            Company ancestor = companyRepository.findNotDeletedById(current)
                    .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));
            if (ancestor.getId().equals(companyId)) {
                throw new ScosException(ExceptionCodeError.SCOS_COMPANY_004);
            }
            current = ancestor.getParentCompany() != null ? ancestor.getParentCompany().getId() : null;
            depth++;
        }
    }

    /**
     * Valida que a empresa não possui dependências activas (filiais).
     * Se possuir, o delete deve ser soft-delete (muda para DELETED).
     * Erro: SCOS_COMPANY_003 (409)
     */
    public void validateCompanyHasDependenciesValidation(@NonNull Long companyId) {
        log.info("Validating company has no active subsidiaries: {}", companyId);
        if (companyRepository.existsByParentCompanyId(companyId)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_003);
        }
    }

    /**
     * Valida a regra de matriz: inativar/deletar uma matriz só é permitido
     * se existir outra empresa ativa (que assumirá o papel de matriz).
     * Erro: SCOS_COMPANY_005 (409)
     */
    public void validateRootCompanyConstraintValidation(@NonNull Long companyId) {
        log.info("Validating root company constraint for companyId: {}", companyId);
        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));
        if (company.isMatrix() && !companyRepository.existsByStatus(companyId, StatusCompany.ACTIVE)) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_005);
        }
    }

    /**
     * Valida que a empresa está ativa para aceitar sub-recursos (contact/address).
     * Erro: SCOS_COMPANY_007 (422)
     */
    public void validateCompanyIsActiveForSubResourceValidation(@NonNull Long companyId) {
        log.info("Validating company is active for sub-resource operations: {}", companyId);
        Company company = companyRepository.findNotDeletedById(companyId)
                .orElseThrow(() -> new ScosException(ExceptionCodeError.SCOS_COMPANY_001));
        if (!company.isActive()) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_007);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CompanyContact validations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida se o contato existe e pertence à empresa.
     * Erro: SCOS_COMPANY_006 (404)
     */
    public void validateCompanyContactExistsValidation(@NonNull Long companyId, @NonNull Long contactId) {
        log.info("Validating company contact exists: companyId={}, contactId={}", companyId, contactId);
        if (companyContactRepository.findCompanyContactByCompany(companyId, contactId).isEmpty()) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_006);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CompanyAddress validations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valida se o endereço existe e pertence à empresa.
     * Erro: SCOS_COMPANY_006 (404)
     */
    public void validateCompanyAddressExistsValidation(@NonNull Long companyId, @NonNull Long addressId) {
        log.info("Validating company address exists: companyId={}, addressId={}", companyId, addressId);
        if (companyAddressRepository.findCompanyContactByCompany(companyId, addressId).isEmpty()) {
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_006);
        }
    }

}
