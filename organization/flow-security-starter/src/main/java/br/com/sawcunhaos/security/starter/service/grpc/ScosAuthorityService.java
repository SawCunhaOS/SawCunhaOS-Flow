
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

package br.com.sawcunhaos.security.starter.service.grpc;

import br.com.sawcunhaos.organization.grpc.proto.AuthorityRequest;
import br.com.sawcunhaos.organization.grpc.proto.AuthorityResponse;
import br.com.sawcunhaos.organization.grpc.proto.ValidateAuthorityServiceGrpc;
import io.grpc.StatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@RequiredArgsConstructor
@Slf4j
public class ScosAuthorityService {

    private final ValidateAuthorityServiceGrpc.ValidateAuthorityServiceBlockingV2Stub validateAuthorityServiceStub;

    public AuthorityResponse validate(@NonNull String login) throws StatusException {
        AuthorityRequest authorityRequest = AuthorityRequest.newBuilder().setLogin(login).build();

        log.info("Validating Authority: {}", authorityRequest.getLogin());
        return validateAuthorityServiceStub.validateAuthority(authorityRequest);
    }

}
