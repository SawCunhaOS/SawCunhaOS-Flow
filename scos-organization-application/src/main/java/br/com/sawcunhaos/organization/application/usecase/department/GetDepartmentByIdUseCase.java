
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

package br.com.sawcunhaos.organization.application.usecase.department;

import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.organization.application.dto.DepartmentDTO;
import br.com.sawcunhaos.organization.application.mapper.department.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static br.com.sawcunhaos.organization.domain.exception.ExceptionCodeError.SCOS_DEPARTMENT_001;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetDepartmentByIdUseCase implements ScosBaseUseCase<Long, DepartmentDTO> {

    private final DepartmentRepository departmentRepository;
    private final DepartmentDomainService departmentDomainService;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentDTO execute(@NonNull Long departmentId) {
        log.info("Getting department by id: {}", departmentId);

        // Validate department exists
        departmentDomainService.validateDepartmentExistsValidation(departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ScosException(SCOS_DEPARTMENT_001));

        return departmentMapper.toDepartmentDTO(department);
    }
}

