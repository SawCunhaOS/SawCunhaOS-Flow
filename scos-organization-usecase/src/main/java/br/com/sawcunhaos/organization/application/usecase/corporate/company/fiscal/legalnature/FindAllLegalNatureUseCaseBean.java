
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

package br.com.sawcunhaos.organization.application.usecase.corporate.company.fiscal.legalnature;

import br.com.sawcunhaos.organization.api.dto.GetAllLegalNaturesResponse;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.corporate.company.dto.LegalNatureOutput;
import br.com.sawcunhaos.organization.domain.corporate.company.specification.LegalNatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementação de {@link FindAllLegalNatureUseCase}. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
class FindAllLegalNatureUseCaseBean implements FindAllLegalNatureUseCase {

    private final LegalNatureService legalNatureService;

    @Override
    public GetAllLegalNaturesResponse execute(@NonNull PaginationFilter paginationFilter) {
        log.info("Find All LegalNatures, Page: {}, Size: {}, Direction: {}",
                paginationFilter.page(),
                paginationFilter.sizePerPage(),
                paginationFilter.direction()
        );

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<LegalNatureOutput> legalNatureOutput = legalNatureService.findAll(pageable);

        return GetAllLegalNaturesResponse.builder()
                .data(
                        legalNatureOutput.getContent().stream()
                                .map(LegalNatureApiMapper::toApiLegalNature)
                                .toList()
                )
                .paginatedDTO(
                        PaginatioUtils.createScosPaginated(legalNatureOutput)
                )
                .build();
    }
}
