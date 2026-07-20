
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasonactivate;

import br.com.sawcunhaos.organization.api.dto.GetAllReasonActivateResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonActivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonActivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindAllReasonActivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllReasonActivateUseCaseBean implements FindAllReasonActivateUseCase {

    private final ReasonActivateService reasonActivateService;

    @Override
    public GetAllReasonActivateResponse execute(@NonNull PaginationFilter paginationFilter,
                                                ReasonEntityType entityType,
                                                Boolean active
    ) {
        log.info("Find All ReasonActivate, Page: {}, Size: {}, Direction: {}, EntityType: {}, Active: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction(),
                entityType,
                active
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<ReasonActivateOutput> reasonActivateOutput = reasonActivateService.findAll(
                ReasonActivateApiMapper.toDomainEntityType(entityType),
                active,
                pageable
        );

        return GetAllReasonActivateResponse.builder()
                .data(
                        reasonActivateOutput.getContent().stream()
                                .map(ReasonActivateApiMapper::toApiReasonActivate)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(reasonActivateOutput)
                )
                .build();
    }
}
