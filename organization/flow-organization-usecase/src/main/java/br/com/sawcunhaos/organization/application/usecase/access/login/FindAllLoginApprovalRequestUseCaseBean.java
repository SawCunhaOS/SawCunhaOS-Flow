
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

import br.com.sawcunhaos.organization.api.dto.GetAllLoginApprovalRequestsResponse;
import br.com.sawcunhaos.organization.api.dto.LoginApprovalRequestStatus;
import br.com.sawcunhaos.organization.api.dto.PaginationFilter;
import br.com.sawcunhaos.organization.application.usecase.utils.PaginatioUtils;
import br.com.sawcunhaos.organization.domain.access.login.dto.LoginApprovalRequestOutput;
import br.com.sawcunhaos.organization.domain.access.login.specification.LoginApprovalRequestService;
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
class FindAllLoginApprovalRequestUseCaseBean implements FindAllLoginApprovalRequestUseCase {

    private final LoginApprovalRequestService loginApprovalRequestService;

    @Override
    public GetAllLoginApprovalRequestsResponse execute(@NonNull PaginationFilter paginationFilter, LoginApprovalRequestStatus status, Long loginId) {
        log.info("Find All Login Approval Requests, Status: {}, LoginId: {}", status, loginId);

        Pageable pageable = PaginatioUtils.createPageable(paginationFilter);

        Page<LoginApprovalRequestOutput> page = loginApprovalRequestService.findAll(
                LoginApprovalRequestApiMapper.toDomainStatus(status), loginId, pageable);

        return GetAllLoginApprovalRequestsResponse.builder()
                .data(page.getContent().stream().map(LoginApprovalRequestApiMapper::toApiLoginApprovalRequest).toList())
                .paginatedDTO(PaginatioUtils.createScosPaginated(page))
                .build();
    }
}
