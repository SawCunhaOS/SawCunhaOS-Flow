
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

package br.com.sawcunhaos.organization.boot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"br.com.sawcunhaos"})
@SpringBootApplication(exclude = {
        DataRedisAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
@RequiredArgsConstructor
@Slf4j
public class ScosOrganizationApplication {

    private final BuildProperties buildProperties;   // ← injetado automaticamente

    static void main(String[] args) {
        SpringApplication.run(ScosOrganizationApplication.class, args);
    }

    @PostConstruct
    void init() {
        log.info("[SCOS] ==========================================");
        log.info("[SCOS] Aplicação : {}", buildProperties.getName());
        log.info("[SCOS] Versão    : {}", buildProperties.getVersion());
        log.info("[SCOS] Build     : {}", buildProperties.getTime());
        log.info("[SCOS] ==========================================");
    }
}
