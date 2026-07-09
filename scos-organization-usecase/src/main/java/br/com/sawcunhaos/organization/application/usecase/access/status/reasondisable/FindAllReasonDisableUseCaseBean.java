
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasondisable;

import br.com.sawcunhaos.organization.api.dto.GetAllReasonDisableResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonDisableOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonDisableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindAllReasonDisableUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllReasonDisableUseCaseBean implements FindAllReasonDisableUseCase {

    private final ReasonDisableService reasonDisableService;

    @Override
    public GetAllReasonDisableResponse execute(@NonNull PaginationFilter paginationFilter,
                                               ReasonEntityType entityType,
                                               Boolean active
    ) {
        log.info("Find All ReasonDisable, Page: {}, Size: {}, Direction: {}, EntityType: {}, Active: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction(),
                entityType,
                active
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<ReasonDisableOutput> reasonDisableOutput = reasonDisableService.findAll(
                ReasonDisableApiMapper.toDomainEntityType(entityType),
                active,
                pageable
        );

        return GetAllReasonDisableResponse.builder()
                .data(
                        reasonDisableOutput.getContent().stream()
                                .map(ReasonDisableApiMapper::toApiReasonDisable)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(reasonDisableOutput)
                )
                .build();
    }
}
