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

package br.com.sawcunhaos.organization.api.enumeration;

import br.com.sawcunhaos.foundation.utils.sort.PropertiesOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DepartmentOrder implements PropertiesOrder {
    ID("id"),
    CODE("code");

    private final String properties;

    @Override
    public String properties() {
        return this.getProperties();
    }

    @Override
    public String value(String name) {
        try {
            return DepartmentOrder.valueOf(name).getProperties();
        } catch (Exception exception) {
            return this.getProperties();
        }
    }
}
