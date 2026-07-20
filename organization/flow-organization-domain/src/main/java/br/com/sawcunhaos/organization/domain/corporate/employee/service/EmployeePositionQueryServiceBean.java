
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

package br.com.sawcunhaos.organization.domain.corporate.employee.service;

import br.com.sawcunhaos.organization.domain.corporate.employee.internal.EmployeeQueryRepository;
import br.com.sawcunhaos.organization.domain.corporate.employee.internal.StatusEmployee;
import br.com.sawcunhaos.organization.domain.corporate.employee.specification.EmployeePositionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeePositionQueryServiceBean implements EmployeePositionQueryService {

    private final EmployeeQueryRepository employeeQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveEmployeeInPosition(@NonNull Long positionId) {
        log.info("Check active employee in position: {}", positionId);
        return employeeQueryRepository.existsByPositionIdAndStatus(positionId, StatusEmployee.ACTIVE);
    }

}
