package br.com.sawcunhaos.security.starter.service;


import br.com.sawcunhaos.organization.grpc.proto.AuthorityResponse;
import br.com.sawcunhaos.security.starter.model.ScosSecurityContext;
import br.com.sawcunhaos.security.starter.service.grpc.ScosAuthorityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Slf4j
@RequiredArgsConstructor
public class ScosSecurityService {

    private final ScosAuthorityService scosAuthorityService;

    public ScosSecurityContext getSecurityContext(@NonNull String login) {
        log.info("Getting all granted authority for login: {}", login);

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
            throw new RuntimeException(e);
        }
    }

}
