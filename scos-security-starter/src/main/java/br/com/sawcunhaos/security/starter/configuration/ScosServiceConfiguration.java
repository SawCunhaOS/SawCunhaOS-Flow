package br.com.sawcunhaos.security.starter.configuration;

import br.com.sawcunhaos.organization.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.organization.grpc.proto.ValidateAuthorityServiceGrpc;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.service.ScosSecurityService;
import br.com.sawcunhaos.security.starter.service.ScosSecurityStartupListener;
import br.com.sawcunhaos.security.starter.service.ScosSystemRegistrationService;
import br.com.sawcunhaos.security.starter.service.grpc.ScosAuthorityService;
import br.com.sawcunhaos.security.starter.service.grpc.ScosRegistryService;
import br.com.sawcunhaos.security.starter.specification.ScosSecurity;
import br.com.sawcunhaos.security.starter.specification.ScosSystemRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@RequiredArgsConstructor
public class ScosServiceConfiguration {

    private final BuildProperties buildProperties;

    @Bean
    public ScosAuthorityService scosAuthorityService(
            ValidateAuthorityServiceGrpc.ValidateAuthorityServiceBlockingV2Stub validateAuthorityServiceStub) {
        return new ScosAuthorityService(validateAuthorityServiceStub);
    }

    @Bean
    public ScosRegistryService scosRegistryService(
            RegistryServiceGrpc.RegistryServiceBlockingV2Stub registryServiceStub) {
        return new ScosRegistryService(registryServiceStub);
    }

    @Bean
    @ConditionalOnMissingBean(ScosSecurity.class)
    public ScosSecurity scosSecurityService(ScosAuthorityService scosAuthorityService) {
        return new ScosSecurityService(scosAuthorityService);
    }

    @Bean
    public ScosSystemRegistration scosSystemRegistrationService(
            ScosRegistryService scosRegistryService,
            ScosRegistryProperties scosRegistryProperties,
            MessageSource permissionMessageSource
    ) {
        return new ScosSystemRegistrationService(
                scosRegistryService, scosRegistryProperties, buildProperties, permissionMessageSource);
    }

    @Bean
    @Primary
    public ScosSecurityStartupListener scosSecurityStartupListener(
            ScosSystemRegistration scosSystemRegistrationService) {
        return new ScosSecurityStartupListener(scosSystemRegistrationService);
    }
}
