
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

package br.com.sawcunhaos.flow.geotemporal.infrastructure.enumaration;

import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import br.com.sawcunhaos.security.starter.specification.ScosSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ScosGeotemporalSystem implements ScosSystem {

    ORGANIZATION_SYSTEM(
            "Scos Geotemporal",
            "SCOS_GEOTEMPORAL",
            "Sistema de Gestão de Endereços e Dias.",
            Arrays.stream(ScosGeotemporalPermission.values())
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
