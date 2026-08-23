package br.com.sawcunhaos.security.starter;

import br.com.sawcunhaos.foundation.core.specification.ScosUserAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

@Slf4j
public class ScosUserAuthenticationBean implements ScosUserAuthentication {

    private static final String NO_LOGGED_USER = "Non-logged-in user";

    @Override
    public String findUserAuthentication() {
        log.debug("Find User Authentication");

        if(Objects.isNull(SecurityContextHolder.getContext())) return NO_LOGGED_USER;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Objects.nonNull(authentication) ? authentication.getName() : NO_LOGGED_USER;
    }
}
