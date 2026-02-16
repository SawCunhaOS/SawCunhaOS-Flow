
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
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.repository.department.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DepartmentDomainService {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    public void validateDepartmentCodeExistsValidation(@NonNull String departmentCode){
        log.info("Validating department code: {}", departmentCode);
        if (departmentRepository.existsByCode(departmentCode)) throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_002);
    }

    public void validateDepartmentLinkedToPositionValidation(@NonNull Long departmentId){
        log.info("Validating department linked to position: {}", departmentId);
        if (positionRepository.existsByDepartmentId(departmentId)) throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_003);
    }

    public void validateDepartmentExistsValidation(@NonNull Long departmentId) {
        log.info("Validating department exists: {}", departmentId);
        if (!departmentRepository.existsById(departmentId)) throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_001);
    }

    public void validateDepartmentCodeUniquenessValidation(@NonNull String departmentCode, @NonNull Long departmentId) {
        log.info("Validating department code uniqueness: {}", departmentCode);
        if (departmentRepository.existsByCodeAndNotId(departmentId, departmentCode)) throw new ScosException(ExceptionCodeError.SCOS_DEPARTMENT_002);
    }

}
