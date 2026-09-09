
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

package br.com.sawcunhaos.flow.audit.infrastructure.liquibase;

import liquibase.integration.spring.MultiTenantSpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

@ConditionalOnProperty(
        prefix = "scos.liquibase.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
final class ScosFlowAuditLiquibaseConfiguration {

    private final ScosFlowAuditLiquibaseProperties liquibaseProperties;

    @Bean("scosFlowAuditLiquibase")
    @DependsOn("ScosDataSource")
    public MultiTenantSpringLiquibase scosFlowAuditLiquibase(@Qualifier("ScosDataSource") DataSource dataSource) {
        MultiTenantSpringLiquibase liquibase = new MultiTenantSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(liquibaseProperties.getChangeLog());
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        liquibase.setRollbackFile(liquibaseProperties.getRollbackFile());
        liquibase.setLiquibaseTablespace(liquibaseProperties.getLiquibaseTablespace());
        liquibase.setDatabaseChangeLogTable(liquibaseProperties.getDatabaseChangeLogTable());
        liquibase.setDatabaseChangeLogLockTable(liquibaseProperties.getDatabaseChangeLogLockTable());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        return liquibase;
    }
}
