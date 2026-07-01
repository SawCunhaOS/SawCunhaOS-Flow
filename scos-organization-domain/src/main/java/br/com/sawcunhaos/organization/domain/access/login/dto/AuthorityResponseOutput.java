
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

package br.com.sawcunhaos.organization.domain.access.login.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record AuthorityResponseOutput(
        Long loginId,
        String login,
        String type,
        String status,
        UUID externalId,
        Long profileId,
        String profileCode,
        String name,
        String email,
        Long companyId,
        String companyName,
        Long branchId,
        String branchName,
        Long employeeId,
        List<String>permissions
) {}
