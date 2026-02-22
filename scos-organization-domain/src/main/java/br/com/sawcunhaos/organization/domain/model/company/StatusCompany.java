
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

package br.com.sawcunhaos.organization.domain.model.company;

import lombok.Getter;

/**
 * Enum representando os status possíveis de uma empresa.
 *
 * ACTIVE: Empresa está ativa e operacional
 * INACTIVE: Empresa está inativa, mas não deletada (soft-delete)
 * DELETED: Empresa foi deletada (soft-delete)
 */
@Getter
public enum StatusCompany {
    ACTIVE("Ativa"),
    INACTIVE("Inativa"),
    DELETED("Deletada");

    private final String displayName;

    StatusCompany(String displayName) {
        this.displayName = displayName;
    }
}
