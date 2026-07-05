
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

package br.com.sawcunhaos.organization.application.usecase.access.status.reasoninactivate;

import br.com.sawcunhaos.organization.api.dto.GetAllReasonInactivateResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ReasonEntityType;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.access.status.dto.ReasonInactivateOutput;
import br.com.sawcunhaos.organization.domain.access.status.specification.ReasonInactivateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindAllReasonInactivateUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindAllReasonInactivateUseCaseBean implements FindAllReasonInactivateUseCase {

    private final ReasonInactivateService reasonInactivateService;

    @Override
    public GetAllReasonInactivateResponse execute(@NonNull PaginationFilter paginationFilter,
                                                  ReasonEntityType entityType,
                                                  Boolean active
    ) {
        log.info("Find All ReasonInactivate, Page: {}, Size: {}, Direction: {}, EntityType: {}, Active: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction(),
                entityType,
                active
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<ReasonInactivateOutput> reasonInactivateOutput = reasonInactivateService.findAll(
                ReasonInactivateApiMapper.toDomainEntityType(entityType),
                active,
                pageable
        );

        return GetAllReasonInactivateResponse.builder()
                .data(
                        reasonInactivateOutput.getContent().stream()
                                .map(ReasonInactivateApiMapper::toApiReasonInactivate)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(reasonInactivateOutput)
                )
                .build();
    }
}
