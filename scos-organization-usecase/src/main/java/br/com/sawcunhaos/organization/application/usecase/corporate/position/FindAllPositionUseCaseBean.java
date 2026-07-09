
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

package br.com.sawcunhaos.organization.application.usecase.corporate.position;

import br.com.sawcunhaos.organization.api.dto.GetAllPositionsResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.position.dto.PositionOutput;
import br.com.sawcunhaos.organization.domain.corporate.position.specification.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllPositionUseCaseBean implements FindAllPositionUseCase {

    private final PositionService positionService;

    @Override
    public GetAllPositionsResponse execute(@NonNull PaginationFilter paginationFilter, @NonNull Long departmentId, @NonNull Boolean active) {
        log.info("Find All Position, Page: {}, Size: {}, Direction: {}, DepartmentId: {}, Active: {}", paginationFilter.page(), paginationFilter.sizePerPage(), paginationFilter.direction(), departmentId, active);

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<PositionOutput> positionOutputPage = positionService.findAll(departmentId, active, pageable);

        return GetAllPositionsResponse.builder()
                .data(
                        positionOutputPage.getContent().stream()
                                .map(PositionApiMapper::toApiPosition)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(positionOutputPage)
                )
                .build();
    }

}
