
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
import org.jspecify.annotations.NonNull;

public interface FindAllLoginApprovalRequestUseCase {

    GetAllLoginApprovalRequestsResponse execute(@NonNull PaginationFilter paginationFilter, LoginApprovalRequestStatus status, Long loginId);

}
