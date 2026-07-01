package br.com.sawcunhaos.organization.infrastructure.database;

import com.zaxxer.hikari.HikariDataSource;
import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(
        repositoryBaseClass = BaseJpaRepositoryImpl.class,
        entityManagerFactoryRef = "ScosEntityManagerFactory",
        transactionManagerRef = "ScosTransactionManager",
        basePackages = {
                "br.com.sawcunhaos.organization.domain.access.login.internal",
                "br.com.sawcunhaos.organization.domain.access.profile.internal",
                "br.com.sawcunhaos.organization.domain.access.resource.internal",
                "br.com.sawcunhaos.organization.domain.access.status.internal",
                "br.com.sawcunhaos.organization.domain.access.system.internal",
                "br.com.sawcunhaos.organization.domain.configuration.internal",
                "br.com.sawcunhaos.organization.domain.corporate.catalog.internal",
                "br.com.sawcunhaos.organization.domain.corporate.company.internal",
                "br.com.sawcunhaos.organization.domain.corporate.department.internal",
                "br.com.sawcunhaos.organization.domain.corporate.employee.internal",
                "br.com.sawcunhaos.organization.domain.corporate.position.internal",
                "br.com.sawcunhaos.organization.domain.outbox.internal",
        })
@EnableConfigurationProperties({ScosHikariConfigProperties.class})
@EnableJpaAuditing
@RequiredArgsConstructor
final class ScosDataSourceConfiguration {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    private final ScosHikariConfigProperties scosHikariConfigProperties;

    @Primary
    @Bean(name = "ScosDataSourcePropos")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties ScosDataSourcePropos() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "ScosDataSource")
    public DataSource ScosDataSource(@Qualifier("ScosDataSourcePropos") DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        dataSource.setMetricRegistry(meterRegistry);
        dataSource.setRegisterMbeans(scosHikariConfigProperties.isRegisterMbeans());
        dataSource.setMaximumPoolSize(scosHikariConfigProperties.getMaximumPoolSize());
        dataSource.setMinimumIdle(scosHikariConfigProperties.getMinimumIdle());
        dataSource.setIdleTimeout(scosHikariConfigProperties.getIdleTimeout());
        dataSource.setMaxLifetime(scosHikariConfigProperties.getMaxLifetime());
        dataSource.setConnectionTimeout(scosHikariConfigProperties.getConnectionTimeout());
        dataSource.setPoolName(scosHikariConfigProperties.getPoolName());
        dataSource.setValidationTimeout(scosHikariConfigProperties.getValidationTimeout());

        dataSource.setKeepaliveTime(scosHikariConfigProperties.getKeepaliveTime());
        dataSource.setIsolateInternalQueries(true);
        dataSource.setAutoCommit(false);

        if (scosHikariConfigProperties.getLeakDetectionThreshold() > 0) {
            dataSource.setLeakDetectionThreshold(scosHikariConfigProperties.getLeakDetectionThreshold());
        }

        // Configurações do driver JDBC
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "1000");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("reWriteBatchedInserts", "true"); // Batch rewriting
        dataSource.addDataSourceProperty("defaultRowFetchSize", "100");    // Fetch size otimizado
        dataSource.addDataSourceProperty("tcpKeepAlive", "true");         // Keep-alive TCP
        dataSource.addDataSourceProperty("loginTimeout", "10");            // Timeout de login (10s)
        dataSource.addDataSourceProperty("socketTimeout", "30");           // Socket timeout (30s)

        return dataSource;
    }

    @Primary
    @Bean(name = "ScosEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean ScosEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("ScosDataSource") DataSource dataSource
    ) {
        return builder.dataSource(dataSource)
                .packages(
                        "br.com.sawcunhaos.organization.domain.access.login.internal",
                        "br.com.sawcunhaos.organization.domain.access.profile.internal",
                        "br.com.sawcunhaos.organization.domain.access.resource.internal",
                        "br.com.sawcunhaos.organization.domain.access.status.internal",
                        "br.com.sawcunhaos.organization.domain.access.system.internal",
                        "br.com.sawcunhaos.organization.domain.configuration.internal",
                        "br.com.sawcunhaos.organization.domain.corporate.catalog.internal",
                        "br.com.sawcunhaos.organization.domain.corporate.company.internal",
                        "br.com.sawcunhaos.organization.domain.corporate.department.internal",
                        "br.com.sawcunhaos.organization.domain.corporate.employee.internal",
                        "br.com.sawcunhaos.organization.domain.corporate.position.internal",
                        "br.com.sawcunhaos.organization.domain.outbox.internal"
                )
                .persistenceUnit("ScosPersistenceUnit")

                .build();
    }

    @Primary
    @Bean(name = "ScosTransactionManager")
    @ConfigurationProperties("spring.jpa")
    public PlatformTransactionManager ScosTransactionManager(
            @Qualifier("ScosEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
