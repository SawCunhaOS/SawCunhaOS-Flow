
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.cnae;

import br.com.sawcunhaos.organization.api.dto.GetAllCnaesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.CnaeOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.CnaeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Implementação de {@link FindAllCnaeUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
class FindAllCnaeUseCaseBean implements FindAllCnaeUseCase {

    private final CnaeService cnaeService;

    @Override
    public GetAllCnaesResponse execute(@NonNull PaginationFilter paginationFilter) {
        log.info("Find All Cnaes, Page: {}, Size: {}, Direction: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction()
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<CnaeOutput> cnaeOutput = cnaeService.findAll(pageable);

        return GetAllCnaesResponse.builder()
                .data(
                        cnaeOutput.getContent().stream()
                                .map(CnaeApiMapper::toApiCnae)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(cnaeOutput)
                )
                .build();
    }
}
