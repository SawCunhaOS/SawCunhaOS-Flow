
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

package br.com.sawcunhaos.organization.domain.corporate.company.internal;

import lombok.Getter;

@Getter
public enum StatusCompany {
    ACTIVE("Ativa"),
    INACTIVE("Inativa"),
    DISABLED("Desabilitada"),
    DELETED("Deletada");

    private final String displayName;

    StatusCompany(String displayName) {
        this.displayName = displayName;
    }
}
