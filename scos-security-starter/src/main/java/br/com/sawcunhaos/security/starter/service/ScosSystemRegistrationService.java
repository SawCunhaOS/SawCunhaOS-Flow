
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

package br.com.sawcunhaos.security.starter.service;

import br.com.sawcunhaos.organization.grpc.proto.RegistryResourcesRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemRequest;
import br.com.sawcunhaos.organization.grpc.proto.RegistrySystemResponse;
import br.com.sawcunhaos.organization.grpc.proto.Resource;
import br.com.sawcunhaos.security.starter.configuration.properties.ScosRegistryProperties;
import br.com.sawcunhaos.security.starter.model.ScosSystemContext;
import br.com.sawcunhaos.security.starter.model.ScosSystemContextHolder;
import br.com.sawcunhaos.security.starter.service.grpc.ScosRegistryService;
import br.com.sawcunhaos.security.starter.specification.ScosPermission;
import br.com.sawcunhaos.security.starter.specification.ScosSystemRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.boot.info.BuildProperties;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class ScosSystemRegistrationService implements ScosSystemRegistration {

    private static final int MAX_RETRIES  = 5;
    private static final int DELAY_MS     = 3000;

    private final ScosRegistryService scosRegistryService;
    private final ScosRegistryProperties properties;
    private final BuildProperties buildProperties;

    @Override
    public void register(int attempt) {
        try {
            RegistrySystemRequest request = RegistrySystemRequest.newBuilder()
                    .setName(properties.getSystemName())
                    .setCode(properties.getSystemCode())
                    .setDescription(properties.getSystemDescription())
                    .setVersion(buildProperties.getVersion())
                    .build();

            RegistrySystemResponse response = scosRegistryService.registrySystem(request);

            ScosSystemContextHolder.set(new ScosSystemContext(
                    response.getSystemId(),
                    response.getSecretKey(),
                    properties.getSystemCode()
            ));

            if (response.getUpdate()) {
                log.info("[SCOS] System registered. systemId={}", response.getSystemId());

                RegistryResourcesRequest registryResourcesRequest = RegistryResourcesRequest.newBuilder()
                        .setSystemId(response.getSystemId())
                        .addAllResources(createResources())
                        .build();

                scosRegistryService.registryResources(registryResourcesRequest);
                log.info("[SCOS] Resources registered.");
            } else {
                log.info("[SCOS] System already registered. systemId={}", response.getSystemId());
            }
        } catch (Exception ex) {
            if (attempt >= MAX_RETRIES) {
                // sem registro o sistema não consegue validar permissões
                throw new IllegalStateException(
                        "[SCOS] Falha ao registrar sistema após " + MAX_RETRIES
                                + " tentativas. Startup abortado.", ex
                );
            }

            log.warn("[SCOS] Tentativa {}/{} falhou. Retrying em {}ms...",
                    attempt, MAX_RETRIES, DELAY_MS);

            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            register(attempt + 1);
        }
    }

    private List<Resource> createResources() {
        Reflections reflections = new Reflections("br.com.sawcunhaos");

        Set<Class<? extends ScosPermission>> classes =
                reflections.getSubTypesOf(ScosPermission.class);

        return classes.stream()
                .filter(Class::isEnum)
                .flatMap(c -> Stream.of(((Class<? extends Enum<?>>) c).getEnumConstants()))
                .map(p -> (ScosPermission) p)
                .map(p -> Resource.newBuilder()
                        .setCode(p.getPermission())
                        .setDescriptionPt(p.getDescriptionPtBr())
                        .setDescriptionEn(p.getDescriptionEng())
                        .setActive(p.getActive())
                        .build())
                .collect(Collectors.toList());
    }


}
