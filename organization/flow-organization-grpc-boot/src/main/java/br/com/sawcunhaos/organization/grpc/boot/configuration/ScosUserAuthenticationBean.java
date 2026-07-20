
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.grpc.boot.configuration;

import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScosUserAuthenticationBean implements ScosUserAuthentication {

    @Override
    public String findUserAuthentication() {
        log.debug("Find User Authentication");

        try {
            return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        } catch (Exception e) {
            log.debug("Error obtaining user authentication", e);
        }

        return "SYSTEM_GRPC";
    }
}
