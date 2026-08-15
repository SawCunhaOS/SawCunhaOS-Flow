
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

import br.com.sawcunhaos.organization.application.usecase.access.resource.registry.RegistryResourceInput;
import br.com.sawcunhaos.organization.application.usecase.access.resource.registry.RegistryResourcesUseCase;
import br.com.sawcunhaos.organization.application.usecase.access.system.registry.RegistrySystemInput;
import br.com.sawcunhaos.organization.application.usecase.access.system.registry.RegistrySystemOutput;
import br.com.sawcunhaos.organization.application.usecase.access.system.registry.RegistrySystemUseCase;
import br.com.sawcunhaos.security.grpc.proto.Empty;
import br.com.sawcunhaos.security.grpc.proto.RegistryResourcesRequest;
import br.com.sawcunhaos.security.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.security.grpc.proto.RegistrySystemRequest;
import br.com.sawcunhaos.security.grpc.proto.RegistrySystemResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistreServiceImpl extends RegistryServiceGrpc.RegistryServiceImplBase {

    private final RegistrySystemUseCase registrySystemUseCase;
    private final RegistryResourcesUseCase registryResourcesUseCase;

    @Override
    public void registrySystem(RegistrySystemRequest request, StreamObserver<RegistrySystemResponse> responseObserver) {
        log.info("Registry System: {}", request.getCode());

        RegistrySystemInput input = RegistrySystemInput.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .version(request.getVersion())
        .build();

        RegistrySystemOutput registrySystemOutput = registrySystemUseCase.execute(input);

        responseObserver.onNext(
                RegistrySystemResponse.newBuilder()
                        .setSystemId(registrySystemOutput.systemId())
                        .setSecretKey(registrySystemOutput.secretKey())
                        .setUpdate(registrySystemOutput.update())
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void registryResources(RegistryResourcesRequest request, StreamObserver<Empty> responseObserver) {
        log.info("Registry Resources: {}", request.getSystemId());

        List<RegistryResourceInput> inputs = request.getResourcesList().stream()
                .map(resource -> RegistryResourceInput.builder()
                        .code(resource.getCode())
                        .descriptionPt(resource.getDescriptionPt())
                        .descriptionEn(resource.getDescriptionEn())
                        .group(resource.getGroup())
                        .subGroup(resource.getSubGroup())
                        .version(resource.getVersion())
                        .updatedAt(LocalDate.parse(resource.getUpdatedAt()).atStartOfDay(ZoneOffset.UTC).toInstant())
                        .active(resource.getActive())
                        .build())
                .collect(Collectors.toList());

        registryResourcesUseCase.execute(inputs);

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
