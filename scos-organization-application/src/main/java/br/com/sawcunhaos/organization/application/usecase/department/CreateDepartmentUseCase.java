
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

import br.com.sawcunhaos.foundation.exception.error.ScosNoRollbackException;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import br.com.sawcunhaos.foundation.utils.specification.ScosBaseUseCase;
import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import br.com.sawcunhaos.organization.application.dto.CreateDepartmentDTO;
import br.com.sawcunhaos.organization.application.mapper.department.DepartmentMapper;
import br.com.sawcunhaos.organization.domain.model.department.Department;
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class CreateDepartmentUseCase implements ScosBaseUseCase<CreateDepartmentDTO, Long> {

    private final DepartmentRepository departmentRepository;
    private final DepartmentDomainService departmentDomainService;
    private final DepartmentMapper departmentMapper;
    private final ScosUserAuthentication scosUserAuthentication;

    @Override
    public Long execute(CreateDepartmentDTO createDepartmentDTO) {
        log.info("Creating department: {}", createDepartmentDTO);
        departmentDomainService.validateDepartmentCodeExistsValidation(createDepartmentDTO.getCode());

        Department department = departmentMapper.toDepartment(createDepartmentDTO);
        department.updateAuditInfo(scosUserAuthentication.findUserAuthentication());

        return departmentRepository.persist(department).getId();
    }
}
