
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public final class ScosSecurityContext {

    private String   login;
    private String   name;
    private String   email;
    private Long     companyId;
    private String   companyName;
    private Long     branchId;
    private String   branchName;
    private Long     employeeId;
    private List<String> permissions;

    // construtor, getters — imutável por design

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
