package br.com.sawcunhaos.organization.infrastructure.liquibase;

import br.com.sawcunhaos.foundation.utils.configuration.liquibase.BaseLiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "scos.liquibase.organization")
class ScosFlowOrganizationLiquibaseProperties extends BaseLiquibaseProperties {

    private String changeLog = "classpath:/db/changelog/organization/db.changelog-master.yml";

}
