
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

package br.com.sawcunhaos.organization.domain.configuration.specification;

import br.com.sawcunhaos.organization.domain.configuration.dto.ConfigurationOutput;
import br.com.sawcunhaos.organization.domain.configuration.dto.KeyConfigurationOutput;

import java.util.List;

public interface ConfigurationService {

    List<KeyConfigurationOutput> getAllKeys();

    List<ConfigurationOutput> getAllConfigurations();
    ConfigurationOutput getConfiguration(String key);
    void updateConfiguration(String key, String value);
}
