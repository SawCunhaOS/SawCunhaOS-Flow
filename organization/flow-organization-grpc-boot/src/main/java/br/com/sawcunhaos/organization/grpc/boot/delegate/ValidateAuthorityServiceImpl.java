
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

package br.com.sawcunhaos.organization.grpc.boot.delegate;

import br.com.sawcunhaos.organization.application.usecase.access.authority.validate.ValidateAuthorityOutput;
import br.com.sawcunhaos.organization.application.usecase.access.authority.validate.ValidateAuthorityUseCase;
import br.com.sawcunhaos.organization.grpc.proto.AuthorityRequest;
import br.com.sawcunhaos.organization.grpc.proto.AuthorityResponse;
import br.com.sawcunhaos.organization.grpc.proto.ValidateAuthorityServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static br.com.sawcunhaos.foundation.utils.configuration.rest.filter.LoggingInitialFilter.REQUEST_ID_HEADER;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateAuthorityServiceImpl extends ValidateAuthorityServiceGrpc.ValidateAuthorityServiceImplBase {

    private final ValidateAuthorityUseCase validateAuthorityUseCase;
    private static final Marker AUDIT = MarkerFactory.getMarker("SCOS_AUDIT");

    @Override
    public void validateAuthority(AuthorityRequest request, StreamObserver<AuthorityResponse> responseObserver) {
        log.info(AUDIT, "AUTHORITY_QUERY system={} login={} requestId={}",
                SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
                request.getLogin(),
                MDC.get(REQUEST_ID_HEADER)
        );

        ValidateAuthorityOutput validateAuthorityOutput = validateAuthorityUseCase.execute(request.getLogin());

        responseObserver.onNext(
                AuthorityResponse.newBuilder()
                        .setLogin(validateAuthorityOutput.login())
                        .setName(validateAuthorityOutput.name())
                        .setEmail(validateAuthorityOutput.email())
                        .setCompanyId(validateAuthorityOutput.companyId())
                        .setCompanyName(validateAuthorityOutput.companyName())
                        .setBranchId(validateAuthorityOutput.branchId())
                        .setBranchName(validateAuthorityOutput.branchName())
                        .setEmployeeId(validateAuthorityOutput.employeeId())
                        .addAllPermissions(validateAuthorityOutput.permissions())
                        .build()
        );
        responseObserver.onCompleted();
    }
}
