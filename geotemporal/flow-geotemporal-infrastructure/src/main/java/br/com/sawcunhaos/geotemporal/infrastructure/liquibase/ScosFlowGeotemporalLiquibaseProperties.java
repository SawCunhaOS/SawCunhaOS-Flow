package br.com.sawcunhaos.geotemporal.infrastructure.liquibase;

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
