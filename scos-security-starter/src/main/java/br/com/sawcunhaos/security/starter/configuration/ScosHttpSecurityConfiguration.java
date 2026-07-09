package br.com.sawcunhaos.security.starter.configuration;

import br.com.sawcunhaos.security.starter.exception.AccessDeniedExceptionHandler;
import br.com.sawcunhaos.security.starter.exception.ExceptionHandlerFilter;
import br.com.sawcunhaos.security.starter.filter.ScosAuthorizationRequiredFilter;
import br.com.sawcunhaos.security.starter.filter.ScosCorsFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;

@RequiredArgsConstructor
public class ScosHttpSecurityConfiguration {

    private final ScosCorsFilter corsFilter;
    private final ScosAuthorizationRequiredFilter scosAuthorizationRequiredFilter;
    private final AccessDeniedExceptionHandler accessDeniedExceptionHandler;
    private final ExceptionHandlerFilter exceptionHandlerFilter;

    @Primary
    @Bean("ScosHttpSecurityConfiguration")
    public HttpSecurity scosHttpSecurityConfiguration(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/v*/api-docs/**", "/actuator/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.accessDeniedHandler(accessDeniedExceptionHandler)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                .addFilterBefore(exceptionHandlerFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(scosAuthorizationRequiredFilter, SessionManagementFilter.class)
                .addFilterBefore(corsFilter,SessionManagementFilter.class);
        return httpSecurity;
    }

    @Bean
    public SecurityContextHolderStrategy strategy() {
        SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
        );
        return SecurityContextHolder.getContextHolderStrategy();
    }

}
