package br.com.sawcunhaos.organization.infrastructure.liquibase;

import br.com.sawcunhaos.foundation.utils.configuration.liquibase.BaseLiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "liquibase.scos")
public class ScosLiquibaseProperties extends BaseLiquibaseProperties {

}
