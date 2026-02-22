
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
import br.com.sawcunhaos.organization.domain.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Serviço de domínio para validações e regras de negócio da entidade Company.
 *
 * Responsabilidades:
 * - Validação de unicidade de CNPJ
 * - Validação de parent company (existência, status, ciclos)
 * - Validação de datas (foundationDate)
 * - Validação de deleção (dependências)
 * - Validação de inativação (matriz sem alternativa)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyDomainService {

    private final CompanyRepository companyRepository;

    private static final int MAX_HIERARCHY_DEPTH = 5;

    /**
     * Valida se o CNPJ já existe no sistema.
     *
     * @param taxIdentifier CNPJ formatado ou não
     * @throws ScosException com código SCOS_COMPANY_002 se CNPJ já existe
     */
    public void validateCnpjUniqueness(String taxIdentifier) {
        log.debug("Validating CNPJ uniqueness: {}", taxIdentifier);
        if (companyRepository.existsByTaxIdentifier(taxIdentifier)) {
            log.warn("CNPJ already exists: {}", taxIdentifier);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_002);
        }
    }

    /**
     * Valida se a parent company existe no sistema.
     *
     * @param parentCompanyId ID da empresa-mãe
     * @throws ScosException com código SCOS-012 se não existe
     */
    public void validateParentCompanyExists(Long parentCompanyId) {
        log.debug("Validating parent company exists: {}", parentCompanyId);
        if (!companyRepository.existsById(parentCompanyId)) {
            log.warn("Parent company not found: {}", parentCompanyId);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }
    }

    /**
     * Valida se a parent company está ativa.
     *
     * @param parentCompanyId ID da empresa-mãe
     * @throws ScosException com código SCOS_COMPANY_001 se não está ativa
     */
    public void validateParentCompanyIsActive(Long parentCompanyId) {
        log.debug("Validating parent company is active: {}", parentCompanyId);
        Optional<Company> parentCompany = companyRepository.findById(parentCompanyId);

        if (parentCompany.isEmpty() || !parentCompany.get().isActive()) {
            log.warn("Parent company is not active: {}", parentCompanyId);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }
    }

    /**
     * Valida se não há ciclo na hierarquia de empresas.
     *
     * @param companyId ID da empresa sendo criada/atualizada
     * @param parentCompanyId ID da empresa-mãe
     * @throws ScosException com código SCOS_COMPANY_004 se há ciclo detectado
     */
    public void validateNoCyclicHierarchy(Long companyId, Long parentCompanyId) {
        log.debug("Validating no cyclic hierarchy: companyId={}, parentId={}", companyId, parentCompanyId);

        if (companyId != null && companyId.equals(parentCompanyId)) {
            log.warn("Cyclic hierarchy detected: company cannot be parent of itself");
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_004);
        }

        // Validar profundidade máxima (até 5 níveis)
        int depth = 0;
        Optional<Company> current = companyRepository.findById(parentCompanyId);

        while (current.isPresent() && depth < MAX_HIERARCHY_DEPTH) {
            Company company = current.get();

            if (company.getId().equals(companyId)) {
                log.warn("Cyclic hierarchy detected at depth {}", depth);
                throw new ScosException(ExceptionCodeError.SCOS_COMPANY_004);
            }

            if (company.getParentCompany() != null) {
                current = Optional.of(company.getParentCompany());
            } else {
                current = Optional.empty();
            }
            depth++;
        }

        if (depth >= MAX_HIERARCHY_DEPTH) {
            log.warn("Maximum hierarchy depth exceeded: {} levels", depth);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_004);
        }
    }

    /**
     * Valida se a data de fundação não é no futuro.
     *
     * @param foundationDate Data de fundação
     * @throws ScosException se data é futura
     */
    public void validateFoundationDateNotFuture(LocalDate foundationDate) {
        log.debug("Validating foundation date is not in future: {}", foundationDate);

        if (foundationDate != null && foundationDate.isAfter(LocalDate.now())) {
            log.warn("Foundation date is in the future: {}", foundationDate);
            throw new ScosException();
        }
    }

    /**
     * Valida se a empresa pode ser deletada (não tem filiais ativas).
     *
     * @param companyId ID da empresa
     * @throws ScosException com código SCOS_COMPANY_003, SCOS_COMPANY_006
     */
    public void validateCanDelete(Long companyId) {
        log.debug("Validating if company can be deleted: {}", companyId);

        // Verificar se tem filiais (branches)
        if (companyRepository.existsByParentCompanyId(companyId)) {
            log.warn("Company has branches: {}", companyId);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_003);
        }

        if (!hasActiveCompany(companyId)) {
            log.warn("Cannot delete last active company: {}", companyId);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_006);
        }
    }

    /**
     * Valida se a empresa pode ser inativada (verifica se é última matriz ativa).
     *
     * @param companyId ID da empresa
     * @throws ScosException com código SCOS_COMPANY_001, SCOS_COMPANY_005
     */
    public void validateCanInactivate(Long companyId) {
        log.debug("Validating if company can be inactivated: {}", companyId);

        Optional<Company> company = companyRepository.findById(companyId);

        if (company.isEmpty()) {
            log.warn("Company not found: {}", companyId);
            throw new ScosException(ExceptionCodeError.SCOS_COMPANY_001);
        }

        // Se é matriz, verificar se há outra matriz ativa
        if (company.get().isMatrix()) {
            if (!hasActiveCompany(companyId)) {
                log.warn("Cannot inactivate last active matrix company: {}", companyId);
                throw new ScosException(ExceptionCodeError.SCOS_COMPANY_005);
            }
        }
    }

    /**
     * Verifica se existe alguma empresa com o status ACTIVE.
     * Útil para validar se há pelo menos uma empresa operacional.
     *
     * @return true se existe empresa ativa, false caso contrário
     */
    public boolean hasActiveCompany(Long companyId) {
        return companyRepository.existsByStatus(companyId, StatusCompany.ACTIVE);
    }
}
