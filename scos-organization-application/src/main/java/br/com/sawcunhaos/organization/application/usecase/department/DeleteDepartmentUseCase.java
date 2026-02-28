
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
import br.com.sawcunhaos.organization.domain.repository.department.DepartmentRepository;
import br.com.sawcunhaos.organization.domain.service.department.DepartmentDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = ScosException.class, noRollbackFor = ScosNoRollbackException.class)
public class DeleteDepartmentUseCase implements ScosBaseUseCase<Long, Void> {

    private final DepartmentRepository departmentRepository;
    private final DepartmentDomainService departmentDomainService;

    public Void execute(@NonNull Long departmentId) {
        log.info("Deleting department: {}", departmentId);
        departmentDomainService.validateDepartmentExistsValidation(departmentId);
        departmentDomainService.validateDepartmentLinkedToPositionValidation(departmentId);

        departmentRepository.deleteById(departmentId);
        log.info("Department deleted: {}", departmentId);
        return null;
    }
}
