
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

package br.com.sawcunhaos.organization.application.usecase.utils;

import br.com.sawcunhaos.foundation.core.sort.PropertiesOrder;
import br.com.sawcunhaos.foundation.web.PaginationUtils;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.api.dto.ScosPaginated;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PaginatioUtils {

    public static Pageable createPageable(PaginationFilter paginationFilter, PropertiesOrder propertiesOrder) {
        int page = Objects.nonNull(paginationFilter.page()) ? paginationFilter.page() : 1;
        int sizePerPage = Objects.nonNull(paginationFilter.sizePerPage()) ? paginationFilter.sizePerPage() : 10;
        String direction = Objects.nonNull(paginationFilter.direction()) ? paginationFilter.direction().getValue() : "ASC";

        return PaginationUtils.createPageable(
                page,
                sizePerPage,
                Sort.Direction.valueOf(direction),
                paginationFilter.order(),
                propertiesOrder
        );
    }

    public static Pageable createPageable(PaginationFilter paginationFilter) {
        int page = Objects.nonNull(paginationFilter.page()) ? paginationFilter.page() : 1;
        int sizePerPage = Objects.nonNull(paginationFilter.sizePerPage()) ? paginationFilter.sizePerPage() : 10;
        String direction = Objects.nonNull(paginationFilter.direction()) ? paginationFilter.direction().getValue() : "ASC";

        return PaginationUtils.createPageable(
                page,
                sizePerPage,
                direction
        );
    }

    public static ScosPaginated createScosPaginated(Page<?> page) {
        return ScosPaginated.builder()
                .sizePerPage(page.getSize())
                .totalElements(page.getTotalElements())
                .totalElementsPerPage((long) page.getContent().size())
                .totalPages(page.getTotalPages())
                .build();
    }

}
