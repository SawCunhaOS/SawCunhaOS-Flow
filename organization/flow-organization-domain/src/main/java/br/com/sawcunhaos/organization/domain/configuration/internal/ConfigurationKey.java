
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

package br.com.sawcunhaos.organization.domain.configuration.internal;

import br.com.sawcunhaos.foundation.core.exception.ScosException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static br.com.sawcunhaos.organization.shared.exception.ExceptionCodeError.SCOS_CONFIGURATION_001;

@Getter
@Slf4j
public enum ConfigurationKey {

    EMPLOYEE_MIN_AGE(ConfigurationType.INTEGER),
    COMPANY_HIERARCHY_MAX_DEPTH(ConfigurationType.INTEGER),
    LOGIN_INACTIVITY_TIMEOUT_DAYS(ConfigurationType.INTEGER),
    EMPLOYEE_EMAIL_DOMAIN(ConfigurationType.STRING),
    DEFAULT_COMPANY_ID(ConfigurationType.INTEGER)

    ;

    private final ConfigurationType type;

    ConfigurationKey(ConfigurationType type) {
        this.type = type;
    }

    public static ConfigurationKey valueOfKey(String key) {
        try {
            return ConfigurationKey.valueOf(key.toUpperCase());
        } catch (Exception e) {
            log.debug("Error parsing key: {}", key, e);
        }
        throw new ScosException(SCOS_CONFIGURATION_001);
    }
}
