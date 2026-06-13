package br.com.sawcunhaos.security.starter.configuration;

import br.com.sawcunhaos.organization.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.organization.grpc.proto.ValidateAuthorityServiceGrpc;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.service.ScosSecurityService;
import br.com.sawcunhaos.security.starter.service.ScosSecurityStartupListener;
import br.com.sawcunhaos.security.starter.service.ScosSystemRegistrationService;
import br.com.sawcunhaos.security.starter.service.grpc.ScosAuthorityService;
import br.com.sawcunhaos.security.starter.service.grpc.ScosRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class ScosServiceConfiguration {

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
    public ScosSecurityService scosSecurityService(ScosAuthorityService scosAuthorityService) {
        return new ScosSecurityService(scosAuthorityService);
    }

    @Bean
    public ScosSystemRegistrationService scosSystemRegistrationService(
            ScosRegistryService scosRegistryService,
            ScosRegistryProperties scosRegistryProperties) {
        return new ScosSystemRegistrationService(scosRegistryService, scosRegistryProperties);
    }

    @Bean
    @Primary
    public ScosSecurityStartupListener scosSecurityStartupListener(
            ScosSystemRegistrationService scosSystemRegistrationService) {
        return new ScosSecurityStartupListener(scosSystemRegistrationService);
    }
}
