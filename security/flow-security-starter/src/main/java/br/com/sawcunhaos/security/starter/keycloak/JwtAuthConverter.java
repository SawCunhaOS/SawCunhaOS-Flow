package br.com.sawcunhaos.security.starter.keycloak;

import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.model.ScosAuthentication;
import br.com.sawcunhaos.security.starter.model.ScosSecurityContext;
import br.com.sawcunhaos.security.starter.specification.ScosSecurity;
import br.com.sawcunhaos.security.starter.utils.SecurityExceptionCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PREFERRED_USERNAME = "preferred_username";
    private final ScosSecurity scosSecurityService;
    private final ScosRegistryProperties scosRegistryProperties;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String login = getPrincipalClaimName(jwt);

        if (login == null) {
            throw new ScosSecurityException(SecurityExceptionCode.SCOS_AUTH_001);
        }

        ScosSecurityContext context = scosSecurityService.getSecurityContext(login);

        return new ScosAuthentication(context, jwt);
    }

    private String getPrincipalClaimName(Jwt jwt) {
        return jwt.getClaim(PREFERRED_USERNAME);
    }
}
