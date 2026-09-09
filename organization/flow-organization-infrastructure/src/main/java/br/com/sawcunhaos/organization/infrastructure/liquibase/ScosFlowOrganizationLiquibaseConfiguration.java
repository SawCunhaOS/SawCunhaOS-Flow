package br.com.sawcunhaos.organization.infrastructure.liquibase;

import liquibase.integration.spring.MultiTenantSpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

@ConditionalOnProperty(
        prefix = "scos.liquibase.organization",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
final class ScosFlowOrganizationLiquibaseConfiguration {

    private final ScosFlowOrganizationLiquibaseProperties liquibaseProperties;

    @Bean("ScosFlowOrganizationLiquibase")
    @DependsOn("ScosDataSource")
    public MultiTenantSpringLiquibase ScosLiquibase(@Qualifier("ScosDataSource") DataSource dataSource) {
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
