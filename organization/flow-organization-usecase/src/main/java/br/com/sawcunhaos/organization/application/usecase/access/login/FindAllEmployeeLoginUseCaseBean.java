
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

package br.com.sawcunhaos.organization.application.usecase.access.login;

import br.com.sawcunhaos.organization.api.dto.GetAllLoginsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reaproveita {@link FindAllLoginUseCase} escopado só por employeeId — sem duplicar paginação/filtro/mapeamento. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllEmployeeLoginUseCaseBean implements FindAllEmployeeLoginUseCase {

    private final FindAllLoginUseCase findAllLoginUseCase;

    @Override
    public GetAllLoginsResponse execute(@NonNull Long employeeId, @NonNull PaginationFilter paginationFilter) {
        log.info("Find All Employee Logins, EmployeeId: {}", employeeId);
        return findAllLoginUseCase.execute(paginationFilter, null, null, employeeId);
    }
}
