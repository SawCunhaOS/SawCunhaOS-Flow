
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

package br.com.sawcunhaos.flow.audit.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import br.com.sawcunhaos.security.starter.specification.ScosSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ScosAuditSystem implements ScosSystem {

    ORGANIZATION_SYSTEM(
            "Scos Audit",
            "SCOS_AUDIT",
            "Sistema de Auditoria - Geração de relatórios e validação de acessos.",
            Arrays.stream(ScosAuditPermission.values())
                    .map(p -> (ScosPermission) p).toList()
    );

    private final String name;
    private final String code;
    private final String description;
    private final List<ScosPermission> permissions;

    @Override
    public String getSystemName() {
        return name;
    }

    @Override
    public String getSystemCode() {
        return code;
    }

    @Override
    public String getSystemDescription() {
        return description;
    }

    @Override
    public List<ScosPermission> getPermissions() {
        return permissions;
    }
}
