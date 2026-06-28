
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

package br.com.sawcunhaos.organization.domain.access.login.service;

import br.com.sawcunhaos.organization.domain.access.login.dto.AuthorityResponseOutput;
import br.com.sawcunhaos.organization.domain.access.login.internal.VwAuthorityResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorityResponseMapper {
    AuthorityResponseOutput toOutput(VwAuthorityResponse vwAuthorityResponse);
}
