
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

import br.com.sawcunhaos.organization.grpc.proto.RegistryResourcesRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistryServiceGrpc;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemResponse;
import io.grpc.StatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ScosRegistryService {

    private final RegistryServiceGrpc.RegistryServiceBlockingV2Stub registryServiceStub;

    public RegistrySystemResponse registrySystem(RegistrySystemRequest registrySystemRequest) throws StatusException {
        log.info("Registry System: {}", registrySystemRequest.getCode());
        return registryServiceStub.registrySystem(registrySystemRequest);
    }

    public void registryResources(RegistryResourcesRequest registryResourcesRequest) throws StatusException {
        log.info("Registry Resources: {}", registryResourcesRequest.getSystemId());
        registryServiceStub.registryResources(registryResourcesRequest);
    }

}
