
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

package br.com.sawcunhaos.security.starter.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.stream.Collectors;

public class ScosAuthentication extends AbstractAuthenticationToken {

    private final ScosSecurityContext scosContext;
    private final Jwt jwt;

    public ScosAuthentication(ScosSecurityContext context, Jwt jwt) {
        super(context.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        this.scosContext = context;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override public Object getPrincipal()   { return scosContext; }
    @Override public Object getCredentials() { return jwt; }
    @Override public String getName()        { return scosContext.getLogin(); }

    public ScosSecurityContext getScosContext() { return scosContext; }
}
