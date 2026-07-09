package br.com.sawcunhaos.security.starter.keycloak;

import br.com.sawcunhaos.security.starter.specification.ScosSecurity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class WebSecurityConfig {

    @Bean
    public JwtAuthConverter jwtAuthConverter(ScosSecurity scosSecurityService) {
        return new JwtAuthConverter(scosSecurityService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            @Qualifier("ScosHttpSecurityConfiguration") HttpSecurity httpSecurity,
            JwtAuthConverter jwtAuthConverter
    ) {
        httpSecurity
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                );
        return httpSecurity.build();
    }

}
