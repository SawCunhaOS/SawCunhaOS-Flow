package br.com.sawcunhaos.geotemporal.infrastructure.liquibase;

import liquibase.integration.spring.MultiTenantSpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@ConditionalOnProperty(
        prefix = "scos.liquibase.geotemporal",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
final class ScosFlowGeotemporalLiquibaseConfiguration {

    private final ScosFlowGeotemporalLiquibaseProperties liquibaseProperties;

    @Bean("ScosFlowGeotemporalLiquibase")
    @Primary
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
