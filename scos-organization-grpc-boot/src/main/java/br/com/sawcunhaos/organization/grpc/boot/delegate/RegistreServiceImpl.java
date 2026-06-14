
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

import br.com.sawcunhaos.organization.application.usecase.system.registry.RegistrySystemInput;
import br.com.sawcunhaos.organization.application.usecase.system.registry.RegistrySystemOutput;
import br.com.sawcunhaos.organization.application.usecase.system.registry.RegistrySystemUseCase;
import br.com.sawcunhaos.organization.grpc.proto.Empty;
import br.com.sawcunhaos.organization.grpc.proto.RegistryResourcesRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistreServiceImpl extends RegistryServiceGrpc.RegistryServiceImplBase {

    private final RegistrySystemUseCase registrySystemUseCase;

    @Override
    public void registrySystem(RegistrySystemRequest request, StreamObserver<RegistrySystemResponse> responseObserver) {
        log.info("Registry System: {}", request.getCode());

        RegistrySystemInput input = RegistrySystemInput.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
        .build();

        RegistrySystemOutput registrySystemOutput = registrySystemUseCase.execute(input);

        responseObserver.onNext(
                RegistrySystemResponse.newBuilder()
                        .setSystemId(registrySystemOutput.systemId())
                        .setSecretKey(registrySystemOutput.secretKey())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void registryResources(RegistryResourcesRequest request, StreamObserver<Empty> responseObserver) {
        log.info("Registry Resources: {}", request.getSystemId());

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
