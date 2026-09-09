
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

package br.com.sawcunhaos.flow.geotemporal.infrastructure.liquibase;

import br.com.sawcunhaos.foundation.jpa.liquibase.BaseLiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "scos.liquibase.geotemporal")
class ScosFlowGeotemporalLiquibaseProperties extends BaseLiquibaseProperties {

    private String changeLog = "classpath:/db/changelog/geotemporal/db.changelog-master.yml";

}
