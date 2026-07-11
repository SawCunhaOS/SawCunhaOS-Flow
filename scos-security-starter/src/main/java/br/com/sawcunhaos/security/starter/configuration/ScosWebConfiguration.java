package br.com.sawcunhaos.security.starter.configuration;

import br.com.sawcunhaos.foundation.utils.specification.LocaleService;
import br.com.sawcunhaos.security.starter.ScosUserAuthenticationBean;
import br.com.sawcunhaos.security.starter.configuration.properties.CorsProperties;
import br.com.sawcunhaos.security.starter.exception.AccessDeniedExceptionHandler;
import br.com.sawcunhaos.security.starter.exception.ExceptionHandlerFilter;
import br.com.sawcunhaos.security.starter.filter.ScosCorsFilter;
import br.com.sawcunhaos.security.starter.utils.FilterUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

public class ScosWebConfiguration {

    @Bean
    @RefreshScope
    public CorsProperties corsProperties() {
        return new CorsProperties();
    }

    @Bean
    public FilterUtils filterUtils(CorsProperties corsProperties) {
        return new FilterUtils(corsProperties);
    }

    @Bean
    public ScosCorsFilter scosCorsFilter(FilterUtils filterUtils) {
        return new ScosCorsFilter(filterUtils);
    }

    @Bean
    public AccessDeniedExceptionHandler accessDeniedExceptionHandler(
            LocaleService localeService, ObjectMapper objectMapper) {
        return new AccessDeniedExceptionHandler(localeService, objectMapper);
    }

    @Bean
    public ExceptionHandlerFilter exceptionHandlerFilter(
            LocaleService localeService, ObjectMapper objectMapper) {
        return new ExceptionHandlerFilter(localeService, objectMapper);
    }

    @Bean
    public ScosUserAuthenticationBean scosUserAuthenticationBean() {
        return new ScosUserAuthenticationBean();
    }
}
