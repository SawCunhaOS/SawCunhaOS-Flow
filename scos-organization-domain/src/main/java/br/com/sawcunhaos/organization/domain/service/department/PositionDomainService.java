
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

package br.com.sawcunhaos.organization.domain.service.department;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import br.com.sawcunhaos.organization.domain.repository.employee.EmployeeQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PositionDomainService {

    private final PositionRepository positionRepository;
    private final EmployeeQueryRepository employeeQueryRepository;
    private final DepartmentDomainService departmentDomainService;

    /**
     * Valida se o código da Position já existe
     * Erro: SCOS_POSITION_002
     */
    public void validatePositionCodeExistsValidation(@NonNull String positionCode) {
        log.info("Validating position code uniqueness: {}", positionCode);
        if (positionRepository.existsByCode(positionCode)) {
            throw new ScosException(ExceptionCodeError.SCOS_POSITION_002);
        }
    }

    /**
     * Valida se a Position existe
     * Erro: SCOS_POSITION_001
     */
    public void validatePositionExistsValidation(@NonNull Long positionId) {
        log.info("Validating position exists: {}", positionId);
        if (!positionRepository.existsById(positionId)) {
            throw new ScosException(ExceptionCodeError.SCOS_POSITION_001);
        }
    }

    /**
     * Valida se a Position possui Employees vinculados
     * Erro: SCOS_POSITION_003
     */
    public void validatePositionLinkedToEmployeeValidation(@NonNull Long positionId) {
        log.info("Validating position linked to employees: {}", positionId);
        if (employeeQueryRepository.existsByPositionId(positionId)) {
            throw new ScosException(ExceptionCodeError.SCOS_POSITION_003);
        }
    }

    /**
     * Valida se o código da Position é único para UPDATE
     * Excludes current position ID
     * Erro: SCOS_POSITION_002
     */
    public void validatePositionCodeUniquenessValidation(@NonNull String positionCode, @NonNull Long positionId) {
        log.info("Validating position code uniqueness excluding current id: {}", positionCode);
        if (positionRepository.existsByCodeAndNotId(positionId, positionCode)) {
            throw new ScosException(ExceptionCodeError.SCOS_POSITION_002);
        }
    }

    /**
     * Valida se o Department existe e está ativo.
     * Delega para DepartmentDomainService (A-ARQ-01).
     * Erro: SCOS_DEPARTMENT_001 (404) se não existir, SCOS_DEPARTMENT_006 (422) se inativo.
     */
    public void validateDepartmentExistsAndActiveValidation(@NonNull Long departmentId) {
        departmentDomainService.validateDepartmentExistsAndActiveValidation(departmentId);
    }

}

