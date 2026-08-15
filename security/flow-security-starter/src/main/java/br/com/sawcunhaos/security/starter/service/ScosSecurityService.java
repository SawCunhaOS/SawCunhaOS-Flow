package br.com.sawcunhaos.security.starter.service;


import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.security.grpc.proto.AuthorityResponse;
import br.com.sawcunhaos.security.starter.model.ScosSecurityContext;
import br.com.sawcunhaos.security.starter.service.grpc.ScosAuthorityService;
import br.com.sawcunhaos.security.starter.specification.ScosSecurity;
import br.com.sawcunhaos.security.starter.utils.SecurityExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.cache.annotation.Cacheable;

import java.util.Objects;
import java.util.UUID;

import static br.com.sawcunhaos.foundation.utils.configuration.rest.filter.LoggingInitialFilter.REQUEST_ID_HEADER;

@Slf4j
@RequiredArgsConstructor
public class ScosSecurityService implements ScosSecurity {

    private final ScosAuthorityService scosAuthorityService;

    @Override
    @Cacheable(cacheNames = "scos:authority:ctx", keyGenerator = "ScosCacheKeyGenerator")
    public ScosSecurityContext getSecurityContext(@NonNull String systemCode, @NonNull String login) {
        log.info("Getting all granted authority for login: {}", login);
        if(Objects.isNull(MDC.get(REQUEST_ID_HEADER)) || MDC.get(REQUEST_ID_HEADER).isBlank()){
            MDC.put(REQUEST_ID_HEADER, UUID.randomUUID().toString());
        }
        try {
            AuthorityResponse authorityResponse = scosAuthorityService.validate(login);

            return ScosSecurityContext.builder()
                    .login(authorityResponse.getLogin())
                    .name(authorityResponse.getName())
                    .email(authorityResponse.getEmail())
                    .companyId(authorityResponse.getCompanyId())
                    .companyName(authorityResponse.getCompanyName())
                    .branchId(authorityResponse.getBranchId())
                    .branchName(authorityResponse.getBranchName())
                    .employeeId(authorityResponse.getEmployeeId())
                    .permissions(authorityResponse.getPermissionsList())
                    .build();
        } catch (Exception e) {
            log.error("Error getting all granted authority for login: {}", login, e);
            throw new ScosSecurityException(SecurityExceptionCode.SCOS_AUTH_002);
        }
    }

}
