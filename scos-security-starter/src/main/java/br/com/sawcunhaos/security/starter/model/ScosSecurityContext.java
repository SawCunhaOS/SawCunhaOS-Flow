
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

package br.com.sawcunhaos.security.starter.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Builder
@Getter
public final class ScosSecurityContext {

    private final String   login;
    private final String   name;
    private final String   email;
    private final Long     companyId;
    private final String   companyName;
    private final Long     branchId;
    private final String   branchName;
    private final Long     employeeId;
    private final List<String> permissions;

    // construtor, getters — imutável por design

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
