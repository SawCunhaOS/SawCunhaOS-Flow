package br.com.sawcunhaos.security.starter;

import br.com.sawcunhaos.security.starter.configuration.GrantedAuthorityConfiguration;
import br.com.sawcunhaos.security.starter.configuration.InsideHttpSecurityConfiguration;
import br.com.sawcunhaos.security.starter.configuration.ScosGrpcClientConfiguration;
import br.com.sawcunhaos.security.starter.configuration.ScosServiceConfiguration;
import br.com.sawcunhaos.security.starter.configuration.ScosWebConfiguration;
import br.com.sawcunhaos.security.starter.keycloak.WebSecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "scos.security.enabled", havingValue = "true", matchIfMissing = true)
@Import({
    GrantedAuthorityConfiguration.class,
    ScosGrpcClientConfiguration.class,
    ScosWebConfiguration.class,
    ScosServiceConfiguration.class,
    InsideHttpSecurityConfiguration.class,
    WebSecurityConfig.class
})
public class SecurityConfiguration {}
