package br.com.sawcunhaos.security.starter.keycloak;

import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.specification.ScosSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

public class WebSecurityConfig {

    @Bean
    public JwtAuthConverter jwtAuthConverter(ScosSecurity scosSecurityService, ScosRegistryProperties scosRegistryProperties) {
        return new JwtAuthConverter(scosSecurityService, scosRegistryProperties);
    }

    @Bean("ScosAuthenticationEntryPoint")
    public ScosAuthenticationEntryPoint scosAuthenticationEntryPoint(
        LocaleService localeService,
        ObjectMapper objectMapper
    ) {
        return new ScosAuthenticationEntryPoint(localeService, objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            @Qualifier("ScosHttpSecurityConfiguration") HttpSecurity httpSecurity,
            JwtAuthConverter jwtAuthConverter,
            @Qualifier("ScosAuthenticationEntryPoint") ScosAuthenticationEntryPoint scosAuthenticationEntryPoint
    ) {
        httpSecurity
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        ).authenticationEntryPoint(scosAuthenticationEntryPoint)
                );
        return httpSecurity.build();
    }

}
